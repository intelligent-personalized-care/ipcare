package pt.ipc_app.ble

import pt.ipc_app.R

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class BleManager(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val onEvent: (BleEvent) -> Unit
) {
    sealed class BleEvent {
        data class Error(val message: String) : BleEvent()
        data class ScanStarted(val filters: String) : BleEvent()
        object ScanStopped : BleEvent()
        data class DeviceFound(val device: BluetoothDevice) : BleEvent()
        object Connecting : BleEvent()
        object Connected : BleEvent()
        object Disconnected : BleEvent()
        object ServicesDiscovered : BleEvent()
        data class ValueReceived(val value: String) : BleEvent()
    }

    private val bluetoothManager: BluetoothManager? =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private var scanTimeout: Job? = null

    private var gatt: BluetoothGatt? = null
    private var targetCharacteristic: BluetoothGattCharacteristic? = null
    private var negotiatedMtu = 23
    private var transportReady = false
    private var connectionEpoch = 0
    private val outgoing = java.util.ArrayDeque<ByteArray>()
    private var writeInFlight = false
    private var writeTimeout: Job? = null

    fun isBleSupported(): Boolean =
        appContext.packageManager.hasSystemFeature("android.hardware.bluetooth_le")

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan() {
        val btAdapter = adapter
        if (btAdapter == null) {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_bluetooth_unavailable)))
            return
        }
        if (!btAdapter.isEnabled) {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_bluetooth_is_disabled)))
            return
        }

        stopScan()
        scanner = btAdapter.bluetoothLeScanner
        val leScanner = scanner
        if (leScanner == null) {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_unable_to_scan_for_sensors)))
            return
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanFailed(errorCode: Int) {
                onEvent(BleEvent.Error(appContext.getString(R.string.ble_scan_failed, errorCode)))
            }

            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val fromAd = result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }
                val name = device.name?.takeIf { it.isNotBlank() } ?: fromAd
                val target = BleUuids.TARGET_DEVICE_NAME
                if (name == target || name == "ESP32-C3_BLE" || result.scanRecord?.serviceUuids?.any { it.uuid == BleUuids.SERVICE_UUID } == true) {
                    onEvent(BleEvent.DeviceFound(device))
                    stopScan()
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }
        }
        scanCallback = callback
        onEvent(BleEvent.ScanStarted("open scan, match name=${BleUuids.TARGET_DEVICE_NAME}"))
        leScanner.startScan(null, settings, callback)
        scanTimeout = scope.launch {
            delay(15000)
            if (scanCallback === callback) {
                stopScan()
                onEvent(BleEvent.Error(appContext.getString(R.string.message_sensor_not_found_check_that_it_is_powered_on_and_advertising)))
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeout?.cancel()
        scanTimeout = null
        val cb = scanCallback
        val leScanner = scanner
        if (cb != null && leScanner != null) {
            try {
                leScanner.stopScan(cb)
            } catch (_: Exception) {
            }
        }
        scanCallback = null
        scanner = null
        onEvent(BleEvent.ScanStopped)
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        disconnect()
        onEvent(BleEvent.Connecting)

        val epoch = connectionEpoch
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (epoch != connectionEpoch) { gatt.close(); return }
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onEvent(BleEvent.Error(appContext.getString(R.string.ble_connection_error, status)))
                    try {
                        gatt.close()
                    } catch (_: Exception) {
                    }
                    onEvent(BleEvent.Disconnected)
                    return
                }
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        onEvent(BleEvent.Connected)
                        if (!gatt.requestMtu(247)) {
                            onEvent(BleEvent.Error(appContext.getString(R.string.message_unable_to_prepare_the_sensor_connection_reconnect_and_try_ag)))
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        scope.launch { if (epoch == connectionEpoch) { disconnect(); onEvent(BleEvent.Disconnected) } }
                        try {
                            gatt.close()
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                if (epoch != connectionEpoch) return
                if (status != BluetoothGatt.GATT_SUCCESS || mtu < 183) {
                    onEvent(BleEvent.Error(appContext.getString(R.string.message_the_connection_does_not_support_the_required_message_size_up)))
                    return
                }
                negotiatedMtu = mtu
                if (!gatt.discoverServices()) onEvent(BleEvent.Error(appContext.getString(R.string.message_unable_to_discover_sensor_services)))
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                if (epoch != connectionEpoch || descriptor.uuid != BleUuids.CCCD_UUID) return
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    transportReady = true
                    onEvent(BleEvent.ServicesDiscovered)
                } else {
                    onEvent(BleEvent.Error(appContext.getString(R.string.ble_subscription_error, status)))
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (epoch != connectionEpoch) return
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onEvent(BleEvent.Error(appContext.getString(R.string.ble_discovery_error, status)))
                    return
                }
                val service = gatt.getService(BleUuids.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleUuids.CHARACTERISTIC_UUID)
                if (service == null || characteristic == null) {
                    onEvent(BleEvent.Error(appContext.getString(R.string.message_the_expected_sensor_service_is_unavailable)))
                    return
                }
                targetCharacteristic = characteristic

                // Enable notifications if supported
                tryEnableNotifications(gatt, characteristic)
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                scope.launch {
                    if (epoch != connectionEpoch || gatt !== this@BleManager.gatt || !writeInFlight) return@launch
                    writeTimeout?.cancel()
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        failWrite(appContext.getString(R.string.ble_write_error, status))
                    } else {
                        outgoing.pollFirst()
                        writeInFlight = false
                        sendNextWrite()
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onEvent(BleEvent.Error(appContext.getString(R.string.ble_read_error, status)))
                    return
                }
                if (epoch == connectionEpoch && characteristic.uuid == BleUuids.CHARACTERISTIC_UUID) {
                    val value = characteristic.value?.toString(Charset.forName("UTF-8")).orEmpty()
                    onEvent(BleEvent.ValueReceived(value))
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (epoch == connectionEpoch && characteristic.uuid == BleUuids.CHARACTERISTIC_UUID) {
                    val value = characteristic.value?.toString(Charset.forName("UTF-8")).orEmpty()
                    onEvent(BleEvent.ValueReceived(value))
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (epoch == connectionEpoch && characteristic.uuid == BleUuids.CHARACTERISTIC_UUID) {
                    val str = value.toString(Charsets.UTF_8)
                    onEvent(BleEvent.ValueReceived(str))
                }
            }
        }

        val newGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(appContext, false, callback)
        }
        if (newGatt == null) {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_unable_to_connect_to_the_sensor)))
            onEvent(BleEvent.Disconnected)
            return
        }
        gatt = newGatt
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        connectionEpoch++
        transportReady = false
        negotiatedMtu = 23
        writeTimeout?.cancel()
        writeInFlight = false
        outgoing.clear()
        stopScan()
        try {
            gatt?.disconnect()
        } catch (_: Exception) {
        }
        try {
            gatt?.close()
        } catch (_: Exception) {
        }
        gatt = null
        targetCharacteristic = null
    }

    @SuppressLint("MissingPermission")
    fun read() {
        val g = gatt ?: run {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_connect_the_sensor_to_continue)))
            return
        }
        val ch = targetCharacteristic ?: run {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_the_sensor_communication_channel_is_unavailable)))
            return
        }
        val ok = g.readCharacteristic(ch)
        if (!ok) onEvent(BleEvent.Error(appContext.getString(R.string.message_unable_to_read_sensor_data)))
    }

    fun writeText(text: String) {
        scope.launch {
            if (!transportReady || gatt == null || targetCharacteristic == null) {
                onEvent(BleEvent.Error(appContext.getString(R.string.message_the_wearable_connection_is_not_ready_yet))); return@launch
            }
            val payload = text.toByteArray(Charsets.UTF_8)
            if (payload.isEmpty() || payload.size > minOf(negotiatedMtu - 3, SensorExerciseCommand.MAX_BYTES)) {
                onEvent(BleEvent.Error(appContext.getString(R.string.message_the_command_exceeds_the_allowed_size_update_the_firmware_and))); return@launch
            }
            if (outgoing.size >= 8) { onEvent(BleEvent.Error(appContext.getString(R.string.message_wait_for_confirmation_of_the_previous_command))); return@launch }
            outgoing.addLast(payload)
            sendNextWrite()
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNextWrite() {
        if (writeInFlight || outgoing.isEmpty()) return
        val g = gatt ?: return
        val ch = targetCharacteristic ?: return
        val payload = outgoing.peekFirst() ?: return
        writeInFlight = true
        val ok = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeCharacteristic(ch, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                ch.value = payload
                @Suppress("DEPRECATION")
                g.writeCharacteristic(ch)
            }
        } catch (_: SecurityException) { false }
        if (!ok) { failWrite(appContext.getString(R.string.message_unable_to_send_the_command_to_the_wearable_reconnect)); return }
        writeTimeout = scope.launch {
            delay(5000)
            if (g === gatt && writeInFlight) failWrite(appContext.getString(R.string.message_the_wearable_did_not_acknowledge_the_message_reconnect))
        }
    }

    private fun failWrite(message: String) {
        disconnect()
        onEvent(BleEvent.Disconnected)
        onEvent(BleEvent.Error(message))
    }

    @SuppressLint("MissingPermission")
    private fun tryEnableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val canNotify =
            characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0
        if (!canNotify) {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_the_sensor_does_not_support_notifications)))
            return
        }

        val ok = gatt.setCharacteristicNotification(characteristic, true)
        if (!ok) {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_unable_to_enable_sensor_notifications)))
            return
        }

        val cccd = characteristic.getDescriptor(BleUuids.CCCD_UUID) ?: run {
            onEvent(BleEvent.Error(appContext.getString(R.string.message_the_sensor_notification_configuration_is_missing)))
            return
        }
        val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

        val okWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(cccd, value) == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            run {
                cccd.value = value
                gatt.writeDescriptor(cccd)
            }
        }
        if (okWrite == false) onEvent(BleEvent.Error(appContext.getString(R.string.message_unable_to_subscribe_to_sensor_notifications)))
    }
}
