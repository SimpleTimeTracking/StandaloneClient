package org.stt

import java.util.logging.Logger

class StopWatch(private val name: String) {
    private val start: Long

    init {
        start = System.currentTimeMillis()
    }

    fun stop() = LOG.finest { String.format("%s : %d", name, System.currentTimeMillis() - start) }

    companion object {
        private val LOG = Logger.getLogger(StopWatch::class.java.name)
    }
}
