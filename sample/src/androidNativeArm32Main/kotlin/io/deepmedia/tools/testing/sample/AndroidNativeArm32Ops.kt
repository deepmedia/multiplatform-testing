package io.deepmedia.tools.testing.sample

import kotlin.native.asCPointer
import kotlin.native.concurrent.AtomicInt
import kotlin.native.concurrent.AtomicLong

class AndroidNativeArm32Ops {
    fun useAtomics() {
        val int = AtomicInt(3)
        val res = int.addAndGet(4)
        require(res == 7)

        val lon = AtomicLong(1300000000340343423L)
        require(lon.value == 1300000000340343423L)

        require(!lon.compareAndSet(500000000000L,500000000000L))
        require(lon.compareAndSet(1300000000340343423L,500000000000L))
        require(lon.value == 500000000000L)
        
        val lon2 = AtomicLong(1300000000340343423L)
        val res2 = lon2.addAndGet(-300000000340343423L)
        require(res2 == 1000000000000000000L)
    }
}