package io.deepmedia.tools.testing.android.tools

data class ConnectedDevice(
    val id: String,
    val status: String,
    val extras: Map<String, String>,
    val info: ConnectedDeviceInfo? // null if status != "device"
) {
    companion object {
        internal fun parse(line: String): ConnectedDevice {
            val parts = line.split(' ').mapNotNull {
                it.trim().takeIf { it.isNotEmpty() }
            }
            return ConnectedDevice(
                id = parts[0],
                status = parts[1],
                extras = parts.subList(2, parts.size).associate {
                    val kv = it.split(':')
                    kv[0] to kv[1]
                },
                info = null
            )
        }
    }
}