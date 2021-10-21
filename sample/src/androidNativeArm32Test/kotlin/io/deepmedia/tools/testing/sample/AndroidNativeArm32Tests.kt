package io.deepmedia.tools.testing.sample

import kotlin.native.internal.test.mainNoExit
import kotlin.test.Test

class AndroidNativeArm32Tests {
    val ops = AndroidNativeArm32Ops()

    @Test
    fun testAtomics() {
        println("Executing testAtomics...")
        ops.useAtomics()
    }
}