package io.deepmedia.tools.testing.sample

import kotlin.native.internal.test.mainNoExit
import kotlin.test.Test

class AndroidNativeArm64Tests {
    val ops = AndroidNativeArm64Ops()

    @Test
    fun testUsePlatformApi() {
        println("Executing testUsePlatformApi...")
        ops.usePlatformApi()
    }
}