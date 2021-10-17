package io.deepmedia.tools.testing.android.tools

import org.gradle.api.Project
import java.io.File

internal class Emulator(project: Project, sdkHome: String) {

    private val terminal = Terminal(project)

    private val emulator = run {
        val file = File(File(sdkHome, "emulator"), "emulator")
        require(file.exists()) {
            "Could not find 'emulator' in ${file.absolutePath}. " +
                    "Please make sure that Android SDK Emulator is installed."
        }
        file.absolutePath
    }

    fun start(avd: Avd, port: Int, outputFile: File?): Process {
        val accelerated = runCatching {
            terminal.run(emulator, "-accel-check", timeout = 10)
        }.isSuccess
        // emulator -avd <avd> -no-window -gpu swiftshader_indirect -no-snapshot -noaudio -no-boot-anim
        return terminal.spawn(emulator,
            "-avd", avd.name,
            "-port", port.toString(),
            "-no-window",
            "-gpu", "swiftshader_indirect",
            "-no-snapshot",
            "-noaudio",
            "-no-boot-anim",
            "-accel", if (accelerated) "auto" else "off",
            outputFile = outputFile
        )
    }

    // Kill: either kill process or adb -s <EMU> emu kill
}