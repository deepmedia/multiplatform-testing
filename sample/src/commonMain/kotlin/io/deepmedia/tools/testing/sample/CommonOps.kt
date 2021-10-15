package io.deepmedia.tools.testing.sample

import kotlin.native.CName

class CommonOps {
    fun sumCorrect(a: Int, b: Int) = a + b
    fun sumFailing(a: Int, b: Int) = a + b + 1
}