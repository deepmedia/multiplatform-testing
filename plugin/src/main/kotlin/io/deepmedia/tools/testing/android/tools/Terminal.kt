package io.deepmedia.tools.testing.android.tools

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random

internal class Terminal(private val logsDir: File) {

    constructor(project: Project) : this(File(project.buildDir, "logs"))

    private fun log(
        name: String = Random.nextLong(0, Long.MAX_VALUE).toString()
    ): File = File(logsDir, "$name.log")

    private fun File.prepare(content: String? = null) = apply {
        ensureParentDirsCreated()
        delete()
        if (content != null) writeText(content)
    }

    private fun Process.join(seconds: Long?) = when {
        seconds != null -> waitFor(seconds, TimeUnit.SECONDS)
        else -> waitFor().let { true }
    }

    // Filter out garbage from sdkmanager
    private fun String.isPrintable(): Boolean {
        return !startsWith("Warning: Mapping new ns ")
    }

    fun run(
        command: String,
        vararg args: String,
        silent: Boolean = false,
        answer: String? = null,
        timeout: Long? = null,
        catch: Boolean = false
    ): String {
        // Acquire log files. These are mandatory - if output is big and stream is not consumed,
        // the process hangs. Redirecting to fs ensures that this does not happen without extra threads.
        val inputFile = log()
        val outputFile = log()
        val errorFile = log()

        // Execute the command.
        println("[*] $command ${args.joinToString(" ") { "'$it'" }}")
        val process = ProcessBuilder(command, *args)
            .redirectInput(inputFile.prepare(answer ?: ""))
            .redirectOutput(outputFile.prepare())
            .redirectError(errorFile.prepare())
            .start()

        // Wait for process to finish.
        var failure: String? = null
        if (!process.join(timeout)) {
            failure = "Command $command timed out after $timeout seconds."
            process.destroy()
        } else if (process.exitValue() != 0) {
            failure = "Command $command exited with ${process.exitValue()}."
        }

        // Print to stdout. Ignore the 'silent' flag for error stream
        val output = outputFile.readText()
        if (output.isNotEmpty() && (!silent || failure != null)) {
            output.lines().filter { it.isPrintable() }.forEach { println("    $it") }
        }
        val error = errorFile.readText()
        if (error.isNotEmpty()) {
            error.lines().filter { it.isPrintable() }.forEach { println("[!] $it") }
        }

        // Delete temporary files and return.
        inputFile.delete()
        outputFile.delete()
        errorFile.delete()
        if (failure != null && !catch) error(failure)
        return output
    }

    fun spawn(
        command: String,
        vararg args: String,
        outputFile: File? = null,
        errorFile: File? = outputFile
    ): Process {
        println("[*] $command ${args.joinToString(" ") { "'$it'" }} &")
        return ProcessBuilder(command, *args)
            .redirectOutput((outputFile ?: log()).prepare())
            .redirectError((errorFile ?: log()).prepare())
            .start()
    }
}