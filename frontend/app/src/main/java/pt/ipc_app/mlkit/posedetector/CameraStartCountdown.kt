package pt.ipc_app.mlkit.posedetector

/** Once started, detection jitter cannot restart the three-second countdown. */
class CameraStartCountdown {
    private var armed = false
    private var deadline: Long? = null
    fun arm() { armed = true; deadline = null }
    fun cancel() { armed = false; deadline = null }
    fun update(ready: Boolean, now: Long): Int? {
        if (!armed) return null
        if (!ready && deadline == null) return null
        val end = deadline ?: (now + 3000).also { deadline = it }
        return ((end - now).coerceAtLeast(0) + 999).div(1000).toInt()
    }
}
