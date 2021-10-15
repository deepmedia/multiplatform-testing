package io.deepmedia.tools.testing.android.native_

import org.jetbrains.kotlin.konan.target.KonanTarget

enum class Architecture(val abi: String, val bits: Int) {
    X86("x86", 32),
    X64("x86_64", 64),
    Arm32("armeabi-v7a", 32),
    Arm64("arm64-v8a", 64);

    override fun toString() = abi

    companion object {
        fun fromTarget(target: KonanTarget) = when (target) {
            KonanTarget.ANDROID_ARM32 -> Arm32
            KonanTarget.ANDROID_ARM64 -> Arm64
            KonanTarget.ANDROID_X64 -> X64
            KonanTarget.ANDROID_X86 -> X86
            else -> error("Unexpected target: $this")
        }
    }
}