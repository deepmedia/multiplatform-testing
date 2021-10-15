plugins {
    kotlin("android")
    id("com.android.library")
}

val archs = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

android {
    compileSdk = 31
    ndkVersion = "21.3.6528147"
    defaultConfig {
        targetSdk = 31
        ndk { abiFilters.addAll(archs) }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
}

val deploy by tasks.registering(Copy::class) {
    dependsOn("externalNativeBuildDebug")
    from("build/intermediates/cmake/debug/obj")
    into("build/outputs")
}

val output by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(output.name, file("build/outputs")) {
        builtBy(deploy)
    }
}