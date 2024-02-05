package org.stt.time


import java.time.Duration
import javax.inject.Singleton

@Singleton
class DurationRounder {

    private var intervalMillis: Long = 1
    private var tieBreakMillis: Long = 1

    fun setInterval(interval: Duration) {
        intervalMillis = interval.toMillis()
        tieBreakMillis = intervalMillis / 2
    }

    fun roundDuration(duration: Duration): Duration {
        val millisToRound = duration.toMillis()
        val segments = millisToRound / intervalMillis
        var result = segments * intervalMillis
        val delta = millisToRound - result
        if (delta == tieBreakMillis && segments % 2 == 1L || delta > tieBreakMillis) {
            result += intervalMillis
        }
        return Duration.ofMillis(result)
    }

}
