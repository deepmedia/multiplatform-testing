package io.deepmedia.tools.testing.sample

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidJvmDeviceTests {

    @Test
    fun testSumIntsOnDevice() {
        println("Testing int sum on device...")
        val res = AndroidJvmOps.sumIntegers(4, 10)
        assertEquals(14, res)
    }

    @Test
    fun testSumLongsOnDevice() {
        println("Testing long sum on device...")
        val res = AndroidJvmOps.sumLongs(4000L, 1000L)
        assertEquals(5000L, res)
    }

    @get:Rule
    val rule = activityScenarioRule<AndroidJvmActivity>(Intent(
        InstrumentationRegistry.getInstrumentation().context,
        AndroidJvmActivity::class.java
    ).apply {
        putExtra("foo", "bar")
    })

    @Test
    fun testActivityFoo() {
        rule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.scenario.onActivity {
            assertEquals("bar", it.getIntentFoo())
        }
    }
}