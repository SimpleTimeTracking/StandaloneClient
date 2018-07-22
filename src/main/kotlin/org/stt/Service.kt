package org.stt

interface Service {
    @Throws(Exception::class)
    fun start()

    fun stop()
}
