package io.deepmedia.tools.testing.sample

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidNativeTests {

    private val ops = AndroidNativeOps()

    @Test
    fun testProductCorrect() {
        println("Executing testProductCorrect...")
        val product = ops.multiplyCorrect(5, 5)
        assertEquals(25, product)
    }

    @Test
    fun testProductFailing() {
        println("Executing testProductFailing...")
        val product = ops.multiplyFailing(5, 5)
        // assertEquals(25, product)
    }
}