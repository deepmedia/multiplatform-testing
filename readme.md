# Multiplatform Testing Plugin

A Gradle plugin for easy testing of Kotlin Multiplatform projects.

- Support for testing `androidNative*` binaries on the emulator.
  The plugin takes care of downloading SDK components, identifying the correct platform
  and system image, spinning up a well configured emulator and run the tests.

- Planned support for `android()` targets to do the same.

### Install

The plugin is available on Maven Central. This means you have to add it to the project classpath:

```kotlin
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("io.deepmedia.tools.testing:plugin:0.1.1")
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

# Android Native targets

### Requirements

A few things are required to test Android Native targets:

1. The Android SDK should be installed, including the Command Line tools. The plugin will look for
   the SDK in `$ANDROID_HOME`, but you can configure the directory:

   ```kotlin
   multiplatformTesting {
       androidTools {
           sdkHome.set("path/to/sdk")
       }
   }
   ```

2. A X64 host machine. The plugin *might* work properly on ARM, but this has not been tested
   and the documentation around which emulators can run on which machine is currently very poor.

3. [Hardware acceleration](https://developer.android.com/studio/run/emulator-acceleration). While
   Android documentation states that it is "recommended", hosts without acceleration are typically
   unable to run the emulator at all.

Conditions 1. and 2. should be met by most continuous integration runners. Hardware acceleration
is currently less common (e.g. `macos-latest` machines in GitHub actions).

### How it works

Running Android Native tests is a multi-step process and between these steps, the plugin applies
workarounds to known issues to make testing as smooth and fast as possible.

1. A K/N test executable is built. These executables are built by the Kotlin compiler with the -tr
   option and automatically run all test suites and print logs to Android logcat.
2. K/N test executables [are currently broken](https://youtrack.jetbrains.com/issue/KT-49144).
   The plugin workarounds this issue by treating them as shared libraries and loading them at
   runtime using `dlopen()`, which works because these are PIE binaries.
3. The plugin looks for a connected device (real device or emulator) that is able to run the architecture.
4. If not found, the plugin looks for existing AVDs and starts the first one that would work.
5. If not found, the plugin downloads the needed packages from `sdkmanager`, creates an AVD and starts it.
6. The runner executable is executed using `adb shell`. This makes a huge difference with respect
   to e.g. JNI-based tests, as we don't have to wait for the emulator to be completely booted and care
   about all details (unlock, avoid welcome screens, animations...) that make testing hard in a JVM process.
7. Interesting logcat logs are printed to the host stdout.

### Tasks

> Hint: use ./gradlew tasks --group='Multiplatform Testing' to list all testing tasks.

The plugin provides three types of tasks:

- `runAllAndroidNativeTests` task: runs tests for all androidNative* targets.
- `run<TargetName>Tests` tasks: runs tests for the specified target, e.g. `runAndroidNativeX86Tests`
- `killAndroidEmulators` task: kills all currently running emulators. Can be used to cleanup.

Using the `runAllAndroidNativeTests` tasks is recommended, because it ensures proper ordering and thus
the most efficient emulator installation. Many emulator images can run multiple architectures. For example:

- 64bit images can typically run 32bit binaries
- latest X86/64 images can run ARM binaries through binary translation

The plugin is aware of this and, if `runAllAndroidNativeTests` is used, is able to pick up the best
emulator for the job, saving time and resources.

This means that the typical command will be:

```
./gradlew app:runAllAndroidNativeTests app:killAndroidEmulators
```

or just `./gradlew app:runAllAndroidNativeTests` if you plan to run tests again.

### x86_64 binaries

Testing `x86_64` is disabled by default. The reason is simple - these binaries seem to be broken
in K/N and hit a segmentation fault. This means that the `run<x64Target>Tests` task will always
be skipped, despite being defined. Should you want to actually run them, explicitly enable them:

```kotlin
multiplatformTesting {
    androidNative {
        enableX64()
    }
}
```
