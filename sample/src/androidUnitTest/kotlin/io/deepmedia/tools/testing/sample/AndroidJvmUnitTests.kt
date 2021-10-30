package io.deepmedia.tools.testing.sample

import androidx.test.runner.AndroidJUnitRunner
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidJvmUnitTests {

    @Test
    fun testSumIntsOnHostMachine() {
        println("Testing int sum on host machine...")
        val res = AndroidJvmOps.sumIntegers(4, 10)
        assertEquals(14, res)
    }

    @Test
    fun testSumLongsOnHostMachine() {
        println("Testing long sum on host machine...")
        val res = AndroidJvmOps.sumLongs(4000L, 1000L)
        assertEquals(5000L, res)
    }
}