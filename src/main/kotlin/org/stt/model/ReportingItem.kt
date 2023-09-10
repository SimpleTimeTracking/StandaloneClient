package org.stt.model


import java.time.Duration

data class ReportingItem(val duration: Duration, val comment: String, val isBreak: Boolean) {
    override fun toString() = "$duration $comment ${if (isBreak) "(break)" else ""}"
}
