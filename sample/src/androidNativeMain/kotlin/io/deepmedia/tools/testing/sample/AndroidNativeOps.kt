package io.deepmedia.tools.testing.sample

import platform.posix.printf

class AndroidNativeOps {
    fun multiplyCorrect(a: Int, b: Int) = a * b
    fun multiplyFailing(a: Int, b: Int) = a * b + 1
}


@CName("extraFunction")
fun extraFunction() {
    // error("FOO EXTRA FUNCTION")
    println("FOO EXTRA")
    printf("PRINTING USING printf")
}