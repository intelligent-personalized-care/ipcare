package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.mlkit.posedetector.CameraStartCountdown

class CameraStartCountdownTests {
    @Test fun waitsThreeFullSecondsAfterBecomingReady() {
        val countdown = CameraStartCountdown()
        assertNull(countdown.update(true, 0))
        countdown.arm()
        assertNull(countdown.update(false, 500))
        assertEquals(3, countdown.update(true, 1000))
        assertEquals(3, countdown.update(true, 1999))
        assertEquals(2, countdown.update(true, 2000))
        assertEquals(1, countdown.update(true, 3999))
        assertEquals(0, countdown.update(true, 4000))
    }
    @Test fun lostTrackingDoesNotRestartOrPauseAnExistingCountdown() {
        val countdown = CameraStartCountdown(); countdown.arm()
        countdown.update(true, 0)
        assertEquals(1, countdown.update(false, 2900))
        assertEquals(0, countdown.update(false, 3000))
        assertEquals(0, countdown.update(true, 5999))
        assertEquals(0, countdown.update(true, 6000))
    }
    @Test fun cancellationAndLifecycleResetPreventDelayedStart() {
        val countdown = CameraStartCountdown(); countdown.arm()
        countdown.update(true, 0); countdown.cancel()
        assertNull(countdown.update(true, 10000))
        countdown.arm()
        assertEquals(3, countdown.update(true, 11000))
    }
}
