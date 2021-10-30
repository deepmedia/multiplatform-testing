package io.deepmedia.tools.testing.android.tools

sealed class SdkPackage(
    val id: String,
    val version: String,
    val description: String,
    val location: String?
) {
    protected val attrs by lazy { id.split(';') }

    override fun toString(): String {
        return "${this::class.simpleName ?: "SdkPackage"}(id=$id,version=$version,path=$location)"
    }

    constructor(parts: List<String>) : this(
        id = parts[0],
        version = parts[1],
        description = parts[2],
        location = parts.getOrNull(3) // null if not installed package
    )

    class SystemImage(parts: List<String>) : SdkPackage(parts) {
        val api: Int by lazy {
            // android-Sv2 appeared recently - let's treat as API 1 for now,
            // not sure what this means nor where it'll go.
            attrs[1].removePrefix("android-").toIntOrNull() ?: 1
        }
        val tag: String by lazy { attrs[2] }
        val abi: String by lazy { attrs[3] }
    }

    class Platform(parts: List<String>) : SdkPackage(parts) {
        val api: Int by lazy { attrs[1].removePrefix("android-").toInt() }
    }

    class Emulator(parts: List<String>) : SdkPackage(parts)

    class CmdLineTools(parts: List<String>) : SdkPackage(parts) {
        val latest: Boolean by lazy { attrs[1] == "latest" }
    }

    class PlatformTools(parts: List<String>) : SdkPackage(parts)

    class Unspecified(parts: List<String>) : SdkPackage(parts)

    companion object {
        /**
         * sdkmanager --list avd returns (among other things) a list of:
         * build-tools;28.0.3        | 28.0.3           | Android SDK Build-Tools 28.0.3      | build-tools/28.0.3
         */
        internal fun parse(line: String): SdkPackage {
            val list = line.split('|').map { it.trim() }
            return when {
                list[0].startsWith("system-images;") -> SystemImage(list)
                list[0].startsWith("platforms;") -> Platform(list)
                list[0].startsWith("cmdline-tools;") -> CmdLineTools(list) // sdkmanager, avdmanager
                list[0] == "platform-tools" -> PlatformTools(list) // adb
                list[0] == "emulator" -> Emulator(list)
                else -> Unspecified(list)
            }
        }
    }
}