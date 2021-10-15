package io.deepmedia.tools.testing.android.tools

data class ConnectedDeviceInfo(
    private val props: Map<String, String>
): Map<String, String> by props {

    // All abis that can run on this device. Typically 64bit images can run 32bit
    // code, and latest x86/x64 images can also run arm code through NDK translation.
    val abiList: List<String> = this["ro.product.cpu.abilist"]!!.split(',')

    val abi: String = this["ro.product.cpu.abi"]!!

    val api: Int = this["ro.build.version.sdk"]!!.toInt()

    val avdName: String? = this["ro.kernel.qemu.avd_name"]
        ?: this["ro.boot.qemu.avd_name"]

    override fun toString(): String {
        return "ConnectedDeviceInfo(abiList=$abiList,abi=$abi,api=$api,avdName=$avdName)"
    }

    companion object {
        private fun String.removeBrackets() = removeSurrounding("[", "]")

        internal fun parse(content: String): ConnectedDeviceInfo {
            return ConnectedDeviceInfo(
                props = content.lines().associate {
                    if (it.contains(':') && it.startsWith('[') && it.endsWith(']')) {
                        val key = it.split(':')[0]
                        val value = it.removePrefix("$key:")
                        key.trim().removeBrackets() to value.trim().removeBrackets()
                    } else {
                        // Malformed / multiline property, like:
                        // [persist.sys.boot.reason.history]: [reboot,factory_reset,1633976748
                        // reboot,1633969519]
                        // Ignoring these for now. TODO consider splitting by ]\n[ and then by ]: [
                        "" to ""
                    }
                }
            )
        }
    }
}