package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.mlkit.vision.VideoUploadPolicy

class VideoUploadPolicyTests {
    @Test fun multipartBudgetLeavesSpaceBelowHostingLimit() {
        assertFalse(VideoUploadPolicy.needsCompression(28_000_000))
        assertTrue(VideoUploadPolicy.needsCompression(28_000_001))
        assertTrue(VideoUploadPolicy.MAX_BYTES < 32_000_000)
    }

    @Test fun longerRecordingsReceiveLowerBitratesWithoutClippingDuration() {
        val short = VideoUploadPolicy.bitrate(60_000)
        val long = VideoUploadPolicy.bitrate(300_000)
        assertTrue(long < short)
        assertTrue(long.toLong() * 300 / 8 < VideoUploadPolicy.MAX_BYTES)
        assertEquals(1_200_000, short)
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingDurationCannotProduceAnUploadBudget() { VideoUploadPolicy.bitrate(0) }

    @Test fun locallyDeferredSetAllowsNextSetWithoutServerAcknowledgement() {
        assertEquals(2, VideoUploadPolicy.nextSet(3, emptyList(), listOf(1)))
        assertEquals(3, VideoUploadPolicy.nextSet(3, listOf(1), listOf(2)))
    }

    @Test fun holesAndOutOfOrderAcknowledgementsDoNotSkipUnrecordedSets() {
        assertEquals(2, VideoUploadPolicy.nextSet(4, listOf(1, 4), listOf(3)))
        assertEquals(1, VideoUploadPolicy.nextSet(4, listOf(2), listOf(4)))
    }

    @Test fun allRecordedSetsFinishEvenWhenUploadsRemainPending() {
        assertNull(VideoUploadPolicy.nextSet(3, listOf(1), listOf(2, 3)))
        assertNull(VideoUploadPolicy.nextSet(3, listOf(1, 2, 3), emptyList()))
    }
}
