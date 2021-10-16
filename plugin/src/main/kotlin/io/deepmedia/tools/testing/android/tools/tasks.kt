package io.deepmedia.tools.testing.android.tools

import io.deepmedia.tools.testing.MultiplatformTestingPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*


internal fun Project.registerAndroidToolsTasks(options: AndroidToolsOptions) {
    tasks.register(
        AndroidToolsRefreshTask.taskName(),
        AndroidToolsRefreshTask::class
    ) {
        group = MultiplatformTestingPlugin.TASK_GROUP
        description =
            "Refresh Android tools dependencies (emulator, cmdline-tools, platform-tools)."
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

    registerAndroidToolsRepo()
}

// https://github.com/ReactiveCircus/android-emulator-runner/blob/main/src/sdk-installer.ts
// https://github.com/cirruslabs/docker-images-android/blob/master/sdk/tools/Dockerfile
// https://stackoverflow.com/a/34327202/4288782
private fun Project.registerAndroidToolsRepo() {
    repositories {
        val repo = ivy("https://dl.google.com/") {
            patternLayout {
                artifact("/[organisation]/repository/[module]-[revision].[ext]")
                // android/repository/commandlinetools-mac-7583922_latest.zip
            }
            metadataSources { artifact() }
        }
        exclusiveContent {
            forRepositories(repo)
            filter { includeGroup("android") }
        }
    }
    val androidCommandLineTools by configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
        isVisible = false
    }
    dependencies {
        val name = androidCommandLineTools.name
        val os = System.getProperty("os.name").toLowerCase()
        val isMac = listOf("darwin", "mac os", "macos", "osx").any { it in os }
        val isLinux = listOf("linux", "nix").any { it in os }
        when {
            isMac -> add(name, "android:commandlinetools-mac:7583922_latest@zip")
            isLinux -> add(name, "android:commandlinetools-linux:7583922_latest@zip")
        }
    }
}