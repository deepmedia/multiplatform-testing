package io.deepmedia.tools.testing.sample

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidNativeConcreteTests : CommonAbstractTests() {

    override val two: Int
        get() = 2

    @Test
    fun concreteTestInConcreteClass() {
        println("Executing concreteTestInConcreteClass...")
        assertEquals(4, two * 2)
    }
}