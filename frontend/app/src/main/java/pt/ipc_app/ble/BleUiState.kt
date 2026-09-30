package pt.ipc_app.ble

import android.bluetooth.BluetoothDevice
import pt.ipc_app.service.models.exercises.SensorProfile

data class BleUiState(
    val profile: SensorProfile? = null,
    val profileLoading: Boolean = false,
    val sessionStarting: Boolean = false,
    val sensorCalibrating: Boolean = false,
    val sensorCalibrated: Boolean = false,
    val sessionRunning: Boolean = false,
    val sessionReadyToSave: Boolean = false,
    val sessionSaving: Boolean = false,
    val withLoad: Boolean? = null,
    val loadText: String = "",
    val loadSelectionLocked: Boolean = false,
    val exerciseComplete: Boolean = false,
    val currentSet: Int = 1,
    val sessionMessage: String = "",
    val bleSupported: Boolean = true,
    val bluetoothEnabled: Boolean = true,
    val permissionsGranted: Boolean = false,

    val scanning: Boolean = false,
    val device: BluetoothDevice? = null,

    val connecting: Boolean = false,
    val connected: Boolean = false,
    val servicesDiscovered: Boolean = false,

    val receivedValue: String = "",
    val lastError: String? = null,

    /** Last parsed exercise/sensor sample (from BLE notify). */
    val sensorAngle: Float? = null,
    val sensorPitch: Float? = null,
    val sensorRoll: Float? = null,
    val sensorReps: Int? = null,
    val sensorVelocity: Float? = null,
    val sensorStatus: String = "",
    /** Roll samples for live chart (most recent at end). */
    val rollHistory: List<Float> = emptyList()
)
