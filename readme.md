# Multiplatform Testing Plugin

A Gradle plugin for easy testing of Kotlin Multiplatform projects.

- Support for testing `androidNative*` binaries on the emulator.
  The plugin takes care of downloading SDK components, identifying the correct platform
  and system image, spinning up a well configured emulator and run the tests.

- Planned support for `android()` targets to do the same.

|Component|Status|
|--------|-------|
|Android Native tests (Linux, amd64)|[![Build Status](https://api.cirrus-ci.com/github/deepmedia/multiplatform-testing.svg?task=Build%20%26%20Test%20%28linux%29&script=test)](https://cirrus-ci.com/github/deepmedia/multiplatform-testing)|
|Android Native tests (macOS, x64)|[![Build Status](https://github.com/deepmedia/multiplatform-testing/actions/workflows/test.yml/badge.svg)](https://github.com/deepmedia/multiplatform-testing/actions)|

To run the plugin, Gradle 6.8+ is required.

### Install

The plugin is available on Maven Central. This means you have to add it to the project classpath:

```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("io.deepmedia.tools.testing:plugin:0.2.0")
    }
}
```

and then apply it to the desired project:

```kotlin
plugins {
    id("kotlin-multiplatform")
    id("io.deepmedia.tools.multiplatform-testing")
}
```

We also publish development snapshots with version `latest-SNAPSHOT` in sonatype:

```kotlin
buildscript {
    repositories {
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    dependencies {
        classpath("io.deepmedia.tools.testing:plugin:latest-SNAPSHOT")
    }
}
```

# Android Native targets

### Requirements

A few things are required to test Android Native targets:

1. A X64 host machine. The plugin *might* work properly on ARM, but this has not been tested yet.
   If it works (emulator runs), it is unlikely that X86-based binaries can be executed successfully.

2. The Android SDK Command Line tools should be installed or, if they are not, you should provide
   a valid `sdkHome` directory in which they will be downloaded (macOS / Linux only).

   ```kotlin
   multiplatformTesting {
       androidTools {
           // path to installed SDK, or path where SDK will be installed
           // defaults to $ANDROID_HOME environment variable.
           sdkHome.set("path/to/sdk")
       }
   }
   ```

3. [Hardware acceleration](https://developer.android.com/studio/run/emulator-acceleration). While
   Android documentation states that it is "recommended", hosts without acceleration are typically
   unable to run the emulator at all.

Conditions 1. and 2. should be met by most continuous integration runners. Hardware acceleration is available
in GitHub Actions (`macos` runners) and Cirrus CI (linux containers with `kvm: true`).

### How it works

Running Android Native tests is a multi-step process and between these steps, the plugin applies
workarounds to known issues to make testing as smooth and fast as possible.

1. A K/N test executable is built. These executables are built by the Kotlin compiler with the -tr
   option and automatically run all test suites and print logs to Android logcat.
2. K/N test executables [are currently broken](https://youtrack.jetbrains.com/issue/KT-49144).
   The plugin workarounds this issue by treating them as shared libraries and loading them at
   runtime using `dlopen()` and passing appropriate arguments to trick the Kotlin runtime launcher.
3. The plugin looks for a connected device (real device or emulator) that is able to run the architecture.
4. If not found, the plugin looks for existing AVDs and starts the first one that would work.
5. If not found, the plugin downloads the needed packages from `sdkmanager`, creates an AVD and starts it.
6. The runner executable is executed using `adb shell`. This makes a huge difference with respect
   to e.g. JNI-based tests, as we don't have to wait for the emulator to be completely booted and care
   about all details (unlock, avoid welcome screens, animations...) that make testing hard in a JVM process.
7. Interesting logcat logs are printed to the host stdout.

### Tasks

> Use ./gradlew tasks --group='Multiplatform Testing' to list all testing tasks.

The plugin provides three types of tasks:

- `runAllAndroidNativeTests` task: runs tests for all androidNative* targets.
- `run<TargetName>Tests` tasks: runs tests for the specified target, e.g. `runAndroidNativeX86Tests`
- `killAndroidEmulators` task: kills all currently running emulators. Can be used to cleanup.

Using the `runAllAndroidNativeTests` tasks is recommended, because it ensures the most efficient
emulator installation. This is because emulator images can run multiple architectures
(for example: 64bit images might run 32bit binaries, and X86/64 images might run ARM binaries through binary
translation). The plugin is aware of this and, if `runAllAndroidNativeTests` is used, is able to pick up the best
emulator for the job, saving time and resources.

This means that the typical command will be:

```
./gradlew app:runAllAndroidNativeTests app:killAndroidEmulators
```
