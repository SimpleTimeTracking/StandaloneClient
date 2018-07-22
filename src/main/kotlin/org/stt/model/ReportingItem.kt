package org.stt.model


import java.time.Duration

data class ReportingItem(val duration: Duration, val comment: String) {
    override fun toString() = "$duration $comment"
}
