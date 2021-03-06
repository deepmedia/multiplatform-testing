# Multiplatform Testing Plugin

A Gradle plugin for easy testing of Kotlin Multiplatform projects in your CI pipeline.

- Support for testing `android()` targets on the emulator or a connected device.
  The plugin takes care of downloading SDK components, identifying the correct platform
  and system image, spinning up a well configured emulator and run the tests.

- Support for testing `androidNative*()` binaries on the emulator or a connected device.
  Just like JVM-based android targets, the plugin takes care of the emulator setup
  and ensures that the device will be able to run the target architecture.

|Component|Status|
|--------|-------|
|Android JVM tests (Linux, amd64)|[![Build Status](https://api.cirrus-ci.com/github/deepmedia/multiplatform-testing.svg?task=AndroidJvm%20Tests%20%28linux%29&script=test)](https://cirrus-ci.com/github/deepmedia/multiplatform-testing)|
|Android JVM tests (macOS, x64)|[![Build Status](https://github.com/deepmedia/multiplatform-testing/actions/workflows/test_androidjvm.yml/badge.svg)](https://github.com/deepmedia/multiplatform-testing/actions)|
|Android Native tests (Linux, amd64)|[![Build Status](https://api.cirrus-ci.com/github/deepmedia/multiplatform-testing.svg?task=AndroidNative%20Tests%20%28linux%29&script=test)](https://cirrus-ci.com/github/deepmedia/multiplatform-testing)|
|Android Native tests (macOS, x64)|[![Build Status](https://github.com/deepmedia/multiplatform-testing/actions/workflows/test_androidnative.yml/badge.svg)](https://github.com/deepmedia/multiplatform-testing/actions)|

To run the plugin, Gradle 6.8+ is required.

### Install

The plugin is available on Maven Central. This means you have to add it to the project classpath:

```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("io.deepmedia.tools.testing:plugin:0.4.0")
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

# Android (JVM) targets

### Requirements

A few things are required to test Android targets:

1. The Android SDK Command Line tools should be installed or, if they are not, you should provide
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

2. [Hardware acceleration](https://developer.android.com/studio/run/emulator-acceleration). While
   Android documentation states that it is "recommended", hosts without acceleration are typically
   unable to run the emulator at all. 
   
Note that if you connect a real device, the plugin will detect it and it will not try to launch an emulator.
In this case, host hardware acceleration is not needed of course.

### Tasks

> Use ./gradlew tasks --group='Multiplatform Testing' to list all testing tasks.

The plugin provides two relevant tasks:

- `run<TargetName>Tests` tasks: runs tests for the specified target, typically this will be
  `runAndroidTests` unless you used a custom name for the android target.
- `killAndroidEmulators` task: kills all currently running emulators. Can be used to cleanup.

This means that the typical command will be:

```
./gradlew app:runAndroidTests app:killAndroidEmulators
```

### Configuration

```kotlin
multiplatformTesting {
    android {
        // Enforce testing on a specific API level. If not set, we'll choose the API level in
        // a way that minimizes the number of emulators and the download of new system images.
        // Defaults to the MPT_ANDROID_API environment variable.
        apiLevel.set(21)

        // Enforce testing on a specific image tag. If not set, we'll choose the image tag in
        // a way that minimizes the number of emulators and the download of new system images.
        // Defaults to the MPT_ANDROID_TAG environment variable.
        tag.set("google_apis")

        // Choose the default variant that will be tested when running 'runAndroidTests'.
        // Defaults to the MPT_ANDROID_VARIANT environment variable, falls back to "debug".
        defaultVariant.set("debug")

        // By default, run* tasks execute both instrumented tests and unit tests.
        // Set this flag to false to avoid running unit tests.
        includeUnitTests.set(false)
    }
}
```

# Android Native targets

### Requirements

All the requirements for running [Android JVM targets](#android-jvm-targets) apply. 

In addition, it is highly recommended to use a X64 host machine, especially to be able to test all four architectures 
(x86, x86_64, armeabi-v7a, arm64-v8a). The plugin *might* work on ARM hosts but this has not been tested,
and anyway ARM emulators would not be able to run non-ARM binaries, while x86-based hosts can run ARM code
through binary translation.

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
