package io.deepmedia.tools.testing.android.tools

import io.deepmedia.tools.testing.MultiplatformTestingPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register


internal fun Project.registerAndroidToolsTasks(options: AndroidToolsOptions) {
    tasks.register(
        AndroidToolsRefreshTask.taskName(),
        AndroidToolsRefreshTask::class
    ) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description = "Refresh Android tools dependencies (emulator, cmdline-tools, platform-tools)."
        sdkHome.set(options.sdkHome)
    }
    tasks.register(
        AndroidToolsKillEmulatorsTask.taskName(),
        AndroidToolsKillEmulatorsTask::class
    ) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description = "Kill Android emulators after a multiplatform testing run."
        sdkHome.set(options.sdkHome)
    }
}