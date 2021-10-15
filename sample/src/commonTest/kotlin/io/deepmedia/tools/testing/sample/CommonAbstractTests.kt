package io.deepmedia.tools.testing.sample

import kotlin.test.Test
import kotlin.test.assertEquals

abstract class CommonAbstractTests {

    @Test
    fun concreteTestInAbstractClass() {
        println("Executing concreteTestInAbstractClass...")
        assertEquals(2, two)
    }

    abstract val two: Int
}