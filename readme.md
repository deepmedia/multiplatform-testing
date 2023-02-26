![Project logo](assets/logo.svg)

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

Please check out [the documentation](https://opensource.deepmedia.io/testing).
