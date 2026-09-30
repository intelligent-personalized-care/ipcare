package pt.ipc_app.mlkit.vision

/** Leaves room for multipart headers below App Engine Standard's 32 MB request limit. */
object VideoUploadPolicy {
    const val MAX_BYTES = 28_000_000L
    private const val TARGET_BYTES = 22_000_000L
    fun needsCompression(bytes: Long) = bytes > MAX_BYTES
    fun bitrate(durationMs: Long): Int {
        require(durationMs > 0)
        return (TARGET_BYTES * 8_000 / durationMs).coerceIn(100_000L, 1_200_000L).toInt()
    }
    fun nextSet(total: Int, remote: Collection<Int>, deferred: Collection<Int>): Int? =
        (1..total).firstOrNull { it !in remote && it !in deferred }
}
