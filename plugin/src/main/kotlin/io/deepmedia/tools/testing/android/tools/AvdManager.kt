package io.deepmedia.tools.testing.android.tools

import org.gradle.api.Project
import java.io.File

internal class AvdManager(project: Project, sdkHome: String) {

    private val terminal = Terminal(project)

    private val avd = run {
        val sep = File.separator
        val file = File(sdkHome, "cmdline-tools${sep}latest${sep}bin${sep}avdmanager")
        require(file.exists()) {
            "Could not find 'avdmanager' in ${file.absolutePath}. Please make sure that the " +
                    "Android Command Line Tools (cmdline-tools;latest) are installed."
        }
        file.absolutePath
    }

    fun list(): List<Avd> {
        val content = terminal.run(avd, "list", "avd", silent = true, timeout = 5)
        val lines = content.lines().drop(1) // remove header
        // TODO consider split("\n---------\n")
        val devices = lines.fold(mutableListOf("")) { devices, line ->
            if (line.trim() == "---------") {
                devices.add("") // new device
                devices
            } else {
                val last = devices.last()
                devices[devices.lastIndex] = if (last.isEmpty()) line else "$last\n$line"
                devices
            }
        }.filter { it.isNotEmpty() }
        return devices.map { Avd.parse(it) }
    }

    // https://developer.android.com/studio/command-line/avdmanager#commands_and_command_options
    // example image: "system-images;android-25;google_apis;x86"
    fun create(image: SdkPackage.SystemImage) = create(
        name = "multiplatform-testing-${image.id.replace(';', '-').replace('_', '-')}",
        image = image.id,
        // Our names are unique, so if we're duplicating some avd something is wrong
        force = false
    )

    private fun create(name: String, image: String, force: Boolean): Avd {
        println("Creating AVD from image: $image...")
        terminal.run(avd,
            "create", "avd",
            "--name", name,
            "--package", image,
            *(if (force) arrayOf("--force") else emptyArray()),
            silent = true,
            // This is to answer the question: Do you wish to create a custom hardware profile?
            answer = "no",
            timeout = 20,
        )
        println("AVD $name created.")
        return list().first { it.name == name }
    }

    fun delete(device: Avd) {
        println("Deleting AVD ${device.name}...")
        terminal.run(avd,
            "delete", "avd",
            "--name", device.name,
            silent = false,
            timeout = 5,
        )
        println("AVD ${device.name} deleted.")
    }

}