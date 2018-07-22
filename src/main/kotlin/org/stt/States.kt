package org.stt

object States {

    fun requireThat(condition: Boolean, message: String) {
        if (!condition) {
            throw IllegalStateException(message)
        }
    }
}
