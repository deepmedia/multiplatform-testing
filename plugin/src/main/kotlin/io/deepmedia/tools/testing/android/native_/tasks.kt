package io.deepmedia.tools.testing.android.native_

import io.deepmedia.tools.testing.MultiplatformTestingPlugin
import io.deepmedia.tools.testing.android.tools.AndroidToolsOptions
import io.deepmedia.tools.testing.android.tools.AndroidToolsRefreshTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.Family

internal fun Project.registerAndroidNativeTasks(
    kotlin: KotlinMultiplatformExtension,
    options: AndroidNativeOptions,
    toolsOptions: AndroidToolsOptions
) {
    val androidNativeTargets = kotlin.targets
        .withType(KotlinNativeTarget::class)
        .matching { it.konanTarget.family == Family.ANDROID }

    val prepareTools = tasks.named(AndroidToolsRefreshTask.taskName())

    val runAll = tasks.register(AndroidNativeRunTestsTask.allTaskName()) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description = "Run all androidNative* multiplatform tests."
    }

    androidNativeTargets.all {
        registerAndroidNativeTasks(options, toolsOptions, prepareTools, runAll)
    }

    afterEvaluate {
        androidNativeTargets.forEach {
            // Always execute 32bit preparation after 64bit: 64bit device likely supports 32bit archs.
            // Always execute arm preparation after x86/64: modern x emulators support arm execution.
            // Doing this ensures that we download less images and spin up less emulators, at least
            // when running all tests together.
            val dependencies = when (Architecture.fromTarget(it.konanTarget)) {
                Architecture.Arm32 -> listOf(Architecture.X64, Architecture.X86, Architecture.Arm64)
                Architecture.Arm64 -> listOf(Architecture.X64)
                Architecture.X86 -> listOf(Architecture.X64)
                Architecture.X64 -> emptyList()
            }.flatMap { arch ->
                androidNativeTargets.filter { Architecture.fromTarget(it.konanTarget) == arch }
            }.mapNotNull {
                val runTask = AndroidNativeRunTestsTask.taskName(it)
                runCatching { tasks.named(runTask) }.getOrNull()
            }
            val deviceTask = AndroidNativePrepareDeviceTask.taskName(it)
            tasks.named(deviceTask).configure {
                mustRunAfter(*dependencies.toTypedArray())
            }
        }
    }
}

private fun KotlinNativeTarget.registerAndroidNativeTasks(
    options: AndroidNativeOptions,
    toolsOptions: AndroidToolsOptions,
    prepareToolsTask: TaskProvider<*>,
    runAllTask: TaskProvider<*>
) {
    val targetArch = Architecture.fromTarget(konanTarget)
    val targetName = name

    val prepareRunner = project.tasks.register(
        AndroidNativePrepareRunnerTask.taskName(this),
        AndroidNativePrepareRunnerTask::class
    ) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description = "Prepare multiplatform $targetName test runner."
        architecture.set(targetArch)
        this.targetName.set(targetName)
    }

    val prepareDevice = project.tasks.register(
        AndroidNativePrepareDeviceTask.taskName(this),
        AndroidNativePrepareDeviceTask::class
    ) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description = "Prepare multiplatform $targetName test device."
        architecture.set(targetArch)
        sdkHome.set(toolsOptions.sdkHome)
        dependsOn(prepareToolsTask)
    }

    // binaries.sharedLib(listOf(DEBUG)) { baseName = "tmpShared" }
    val run = project.tasks.register(
        AndroidNativeRunTestsTask.taskName(this),
        AndroidNativeRunTestsTask::class
    ) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description = "Run multiplatform $targetName tests."
        architecture.set(targetArch)
        sdkHome.set(toolsOptions.sdkHome)

        // val binary = binaries.getSharedLib(DEBUG)
        val binary = binaries.getTest(DEBUG)
        dependsOn(binary.linkTaskName)
        executable.fileValue(binary.outputFile)

        dependsOn(prepareRunner)
        runner.set(prepareRunner.flatMap { it.runner })

        dependsOn(prepareDevice)
        device.set(prepareDevice.flatMap { it.device })

        /* if (targetArch == Architecture.X64) {
            onlyIf { options.x64Enabled.get() }
        } */
    }

    runAllTask.configure {
        dependsOn(run)
    }
}