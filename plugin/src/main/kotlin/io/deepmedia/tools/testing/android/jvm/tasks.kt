package io.deepmedia.tools.testing.android.jvm

import com.android.build.gradle.BaseExtension
import io.deepmedia.tools.testing.MultiplatformTestingPlugin
import io.deepmedia.tools.testing.android.tools.AndroidToolsOptions
import io.deepmedia.tools.testing.android.tools.AndroidToolsRefreshTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

// private typealias OldAppExtension = com.android.build.gradle.AppExtension
// private typealias OldLibExtension = com.android.build.gradle.LibraryExtension
// private typealias OldDfExtension = com.android.build.gradle.internal.dsl.DynamicFeatureExtension

private val ANDROID_PLUGINS = listOf(
    "com.android.application",
    "com.android.library",
    "com.android.dynamic-feature"
)

internal fun Project.registerAndroidJvmTasks(
    kotlin: KotlinMultiplatformExtension,
    options: AndroidJvmOptions,
    toolsOptions: AndroidToolsOptions
) {
    val prepareTools = tasks.named(AndroidToolsRefreshTask.taskName())

    ANDROID_PLUGINS.forEach {
        plugins.withId(it) {
            val extension = extensions.getByName("android") as BaseExtension
            kotlin.targets
                .withType(KotlinAndroidTarget::class)
                .all { registerTasks(extension, options, toolsOptions, prepareTools) }
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun KotlinAndroidTarget.registerTasks(
    android: BaseExtension,
    options: AndroidJvmOptions,
    toolsOptions: AndroidToolsOptions,
    prepareToolsTask: TaskProvider<Task>
) {
    val prepareDevice = project.tasks.register(
        AndroidJvmPrepareDeviceTask.taskName(this),
        AndroidJvmPrepareDeviceTask::class
    ) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description = "Prepare multiplatform $targetName test device."
        sdkHome.set(toolsOptions.sdkHome)
        dependsOn(prepareToolsTask)
        apiLevel.set(options.apiLevel)
        tag.set(options.tag)
    }

    val run = project.tasks.register(
        AndroidJvmRunTestsTask.taskName(this),
        // AndroidJvmRunTestsTask::class
    ) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description = "Run multiplatform $targetName tests."
        val variant = options.defaultVariant.orNull?.replaceFirstChar { it.uppercase() } ?: "Debug"
        dependsOn(prepareDevice)
        dependsOn("connected${variant}AndroidTest")
        if (options.includeUnitTests.get()) {
            dependsOn("test${variant}UnitTest")
        }
    }
}