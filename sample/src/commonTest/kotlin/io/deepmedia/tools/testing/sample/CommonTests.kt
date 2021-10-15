package io.deepmedia.tools.testing.sample

import kotlin.test.Test
import kotlin.test.assertEquals

class CommonTests {

    private val ops = CommonOps()

    @Test
    fun testSumCorrect() {
        println("Executing testSumCorrect...")
        val sum = ops.sumCorrect(5, 5)
        assertEquals(10, sum)
    }

    @Test
    fun testSumFailing() {
        println("Executing testSumFailing...")
        val sum = ops.sumFailing(5, 5)
        // assertEquals(10, sum)
    }

    @Test
    fun testSumCorrect2() {
        println("Executing testSumCorrect2...")
        val sum = ops.sumCorrect(2, 2)
        assertEquals(4, sum)
    }
}