package pt.ipc_app.ble

import java.util.UUID

object BleUuids {
    val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("abcd1234-5678-1234-5678-abcdef123456")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val TARGET_DEVICE_NAME: String = "ESP32-C3_DUAL_IMU"
}
