package org.stt.event


@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class Event


class NotifyUser(val message: String)
class ShuttingDown
class TimePassedEvent

