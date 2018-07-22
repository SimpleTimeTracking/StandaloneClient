package org.stt

import org.mockito.Mockito

object Matchers {
    fun <T> argThat(matcher: (T) -> Boolean): T {
        Mockito.argThat(matcher)
        return unitialized()
    }

    inline fun <reified T> any(): T {
        Mockito.any<T>()
        return unitialized()
    }

    fun <T> unitialized() = null as T
}