package io.deepmedia.tools.testing.android.tools

import org.gradle.api.Project
import java.io.File

internal class SdkManager(project: Project, sdkHome: String) {

    companion object {
        fun cmdlineToolsPath(home: String, version: String) =
            File(File(home, "cmdline-tools"), version)
        private fun path(home: String, version: String) =
            File(File(cmdlineToolsPath(home, version), "bin"), "sdkmanager")
    }

    private val terminal = Terminal(project)

    private val sdk = run {
        val versions = listOf("latest", *(10 downTo 1).map { "$it.0" }.toTypedArray())
        val file = versions
            .asSequence()
            .map { path(sdkHome, it) }
            .filter { it.exists() }
            .firstOrNull()
        requireNotNull(file) {
            "Could not find 'sdkmanager' in ${File(sdkHome, "cmdline-tools").absolutePath}. " +
                    "Please make sure that the Android Command Line Tools are installed."
        }
        file.absolutePath
    }

    init {
        terminal.run(sdk, "--version", timeout = 10)
        // terminal.run(sdk, "--list", timeout = 10)
    }

    private fun List<String>.dropUntil(predicate: (String) -> Boolean): List<String> {
        val index = indexOfFirst(predicate)
        return subList(index + 1, size)
    }

    private fun List<String>.dropBeyond(predicate: (String) -> Boolean): List<String> {
        val index = indexOfFirst(predicate)
        return subList(0, index)
    }

    private fun String.isDividerLine() = contains("-------") && contains('|')

    inline fun <reified T: SdkPackage> listInstalled(): List<T> {
        val content = terminal.run(sdk, "--list_installed", silent = true, timeout = 10)
        return content.lines()
            .dropUntil { it.isDividerLine() }
            .dropBeyond { it.isBlank() }
            .map { SdkPackage.parse(it) }
            .filterIsInstance<T>()
    }

    inline fun <reified T: SdkPackage> list(): List<T> {
        val content = terminal.run(sdk, "--list", silent = true, timeout = 10)
        return content.lines()
            .dropUntil { it.startsWith("Available Packages:") }
            .dropUntil { it.isDividerLine() }
            .dropBeyond { it.isBlank() }
            .map { SdkPackage.parse(it) }
            .filterIsInstance<T>()
    }

    fun acceptLicenses() {
        terminal.run(sdk, "--licenses", silent = true, answer = "yes", timeout = 30)
    }

    fun installPlatform(api: Int): SdkPackage.Platform {
        return install("platforms;android-$api").first() as SdkPackage.Platform
    }

    fun install(vararg packages: SdkPackage) =
        install(*packages.map(SdkPackage::id).toTypedArray())

    fun install(vararg ids: String): List<SdkPackage> {
        if (ids.isEmpty()) return emptyList()
        println("Installing package(s): ${ids.toList()}")
        terminal.run(sdk,
            // "--install",
            *ids,
            answer = "yes", // Can ask for licenses
            silent = true, // Pollutes logs a lot, but it'd give download updates. Thinking about it.
            timeout = 60 * 30L, // 30 min
        )
        val installed = listInstalled<SdkPackage>()
        return ids.map { id -> installed.first { it.id == id } }
    }

    fun uninstall(vararg packages: SdkPackage) {
        if (packages.isEmpty()) return
        println("Uninstalling package(s): ${packages.map { it.id }}")
        terminal.run(sdk,
            "--uninstall", *packages.map { "\"${it.id}\"" }.toTypedArray(),
            silent = true,
            timeout = 30
        )
    }
}