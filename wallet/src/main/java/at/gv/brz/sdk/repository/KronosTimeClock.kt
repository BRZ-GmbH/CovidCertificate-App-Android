package at.gv.brz.sdk.repository

import com.lyft.kronos.KronosClock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class KronosTimeClock(val clock: KronosClock) : Clock {
    override fun now(): Instant {
        val kronosMilliseconds = clock.getCurrentNtpTimeMs()
        if (kronosMilliseconds != null) {
            return Instant.fromEpochMilliseconds(kronosMilliseconds)
        }
        return Clock.System.now()
    }

}