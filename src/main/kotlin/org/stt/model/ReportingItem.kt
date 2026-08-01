package org.stt.model


import java.time.Duration

data class ReportingItem(val duration: Duration, val roundedDuration: Duration, val comment: String, val isBreak: Boolean, val backingItems: List<TimeTrackingItem> = emptyList()) {
    override fun toString() = "$duration $comment ${if (isBreak) "(break)" else ""}"
}
