package io.deepmedia.tools.testing.android.native_

import io.deepmedia.tools.testing.android.tools.Adb
import io.deepmedia.tools.testing.android.tools.ConnectedDevice
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import javax.inject.Inject

open class AndroidNativeRunTestsTask @Inject constructor(objects: ObjectFactory) : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NONE) // is this ok?
    @get:InputFile
    val executable: RegularFileProperty = objects.fileProperty()

    @get:PathSensitive(PathSensitivity.NONE) // is this ok?
    @get:InputFile
    val runner: RegularFileProperty = objects.fileProperty()

    @get:Input
    val architecture: Property<Architecture> = objects.property()

    @get:Input
    val device: Property<ConnectedDevice> = objects.property()

    @Option(
        option = "entry_point",
        description = "Entry point function name. Can be useful for debugging."
    )
    @get:Input
    val entryPoint: Property<String> = objects.property<String>()
        .convention("Konan_main")

    @Option(
        option = "core_dumps",
        description = "Whether to generate core dumps in case of errors."
    )
    @get:Input
    val coreDumps: Property<Boolean> = objects.property<Boolean>()
        .convention(false)

    @Option(
        option = "no_runner",
        description = "Whether to avoid using the dlopen runner."
    )
    @get:Input
    val noRunner: Property<Boolean> = objects.property<Boolean>()
        .convention(false)

    @Option(
        option = "dl_path",
        description = "Value for LD_LIBRARY_PATH, with runtime replacement: " +
                "{64} becomes 64 on 64-bit machines, {32} becomes 32 on 32-bit machines. " +
                "The string {bits} behaves like {32}{64}."
    )
    @get:Input
    val dlPath: Property<String> = objects.property<String>()
        .convention("/vendor/lib{64}:/system/lib{64}")

    @get:Input
    val sdkHome: Property<String> = objects.property()

    private val adb by lazy { Adb(project, sdkHome.get()) }

    // Useful info:
    // https://stackoverflow.com/q/35231168/4288782
    // https://stackoverflow.com/questions/23424602/android-permission-denied-for-data-local-tmp

    @TaskAction
    fun execute() {
        val executable = executable.get()
        val runner = runner.get()
        val device = device.get()
        val arch = architecture.get()
        val coreDumps = coreDumps.get()
        val noRunner = noRunner.get()

        // Push executable files.
        adb.push(executable.asFile.absolutePath, "/data/local/tmp", device)
        if (!noRunner) {
            adb.push(runner.asFile.absolutePath, "/data/local/tmp", device)
            adb.run("chmod 777 /data/local/tmp/${runner.asFile.name}", device, 5) // using + in chmod does not work on all devices
        }

        // Dynamic loading: the wrapper will use dlopen at runtime to open the actual executable.
        // We need to pass LD_LIBRARY_PATH for loading it and its dependencies. Note that this path
        // will be ignored when NDK translation is enabled (running arm on x86), which makes this option
        // pretty useless.
        // There exists a specific flag called NDK_TRANSLATION_GUEST_LD_LIBRARY_PATH, but it
        // is undocumented and subject to change: https://issuetracker.google.com/issues/198022909#comment12
        // It is also error prone, because one shouldn't add system/lib64 to translated arm binaries,
        // but rather use system/lib64/arm64.
        val dlPath = dlPath.get()
            .replace("{bits}", "{64}{32}")
            .replace("{64}", if (arch.bits == 64) "64" else "")
            .replace("{32}", if (arch.bits == 32) "32" else "")
        var command = "LD_LIBRARY_PATH=/data/local/tmp:$dlPath"

        // Android native logs are redirected to logcat with Konan_main tag, so we need to
        // use adb logcat to pull them and report them back. Ideally we should spawn a process
        // to listen to them in the background, but for now we just dump (-d) them at the end
        // in a finally block because we only have one thread.
        // https://github.com/JetBrains/kotlin/blob/31d7d341d4efb8d8ebcfa24136f39d0bc4d5ab35/kotlin-native/runtime/src/main/cpp/Porting.cpp#L64-L67
        // https://developer.android.com/studio/command-line/logcat
        runCatching {
            // clear previous, catch because it hangs on some devices
            adb.run("logcat -c", device, 5)
        }

        // Prepare for core dumps if needed. If enabled, we need proper ulimit and we need to
        // execute from writable directory (tmp) otherwise it won't work.
        command = when (coreDumps) {
            true -> "ulimit -c unlimited && cd /data/local/tmp && $command ./"
            false -> "$command /data/local/tmp/"
        }
        command = when (noRunner) {
            true -> "$command${executable.asFile.name}"
            false -> "$command${runner.asFile.name}"
        }

        // Prepare arguments.
        val args = if (noRunner) emptyList() else listOf(
            // Passing just the executable name should be enough because we added it to LD path,
            // However this is not always enough, see comments in LD_LIBRARY_PATH definition above.
            "/data/local/tmp/${executable.asFile.name}",
            // Function name
            entryPoint.get()
        )

        // Execute (timeout = 60 minutes)
        try {
            adb.run("$command ${args.joinToString(" ")}", device, 60L * 60)
        } finally {
            // On some devices, adb command stays on even with the -d flag, so we need a timeout
            runCatching {
                adb.run("logcat -v tag *:F Konan_main:V tombstoned:E -d", device, 5)
            }
        }
    }

    companion object {

        @OptIn(ExperimentalStdlibApi::class)
        fun taskName(target: KotlinNativeTarget) =
            "run${target.name.replaceFirstChar { it.uppercase() }}Tests"

        fun allTaskName() = "runAllAndroidNativeTests"
    }
}