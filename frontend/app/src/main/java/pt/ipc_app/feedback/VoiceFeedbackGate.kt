package pt.ipc_app.feedback

import pt.ipc_app.R

class VoiceFeedbackGate {
    private var lastAt: Long? = null
    private val recent = mutableMapOf<String, Long>()

    fun accept(message: String, now: Long, important: Boolean = false): Boolean {
        if (message.isBlank()) return false
        recent.entries.removeAll { now - it.value >= 6000 }
        if (recent.containsKey(message)) return false
        if (!important && lastAt?.let { now - it < 2000 } == true) return false
        recent[message] = now
        lastAt = now
        return true
    }

    fun reset() { recent.clear(); lastAt = null }
}

object VoiceCues {
    fun sensor(status: String): Int? = when {
        status.startsWith("LIMIT") -> R.string.feedback_movement_outside_the_configured_limits_return_to_the_starting_pos
        status == "HOLD" || status.startsWith("HOLDING") -> R.string.feedback_hold_your_position
        status == "RETURN TO START" || status == "RETURNING" -> R.string.feedback_return_to_the_starting_position
        status == "MOVE TO START" -> R.string.feedback_adopt_the_starting_position
        status == "HOLD LOST" -> R.string.feedback_hold_your_position_for_the_specified_duration
        status == "INCOMPLETE MOVEMENT" -> R.string.feedback_incomplete_movement_begin_again_from_the_starting_position
        else -> null
    }

    fun camera(message: String): Int? = when (message.trim()) {
        "Please stand up straight" -> R.string.feedback_keep_your_torso_upright
        "Please hold your hands behind your head" -> R.string.feedback_place_your_hands_behind_your_head
        "Please spread your feet shoulder-width apart" -> R.string.feedback_place_your_feet_shoulder_width_apart
        "Please keep in a push up position" -> R.string.feedback_get_into_the_push_up_position
        "Please hold your hands straight out in front of your body" -> R.string.feedback_keep_your_hands_in_front_of_your_body
        "Please put your elbows slightly above shoulder height" -> R.string.feedback_place_your_elbows_slightly_above_your_shoulders
        "Please hold your hands above your shoulders" -> R.string.feedback_keep_your_hands_above_your_shoulders
        "Body not detected" -> R.string.feedback_body_not_detected_adjust_your_position_relative_to_the_camera
        "Ensure arms are straight" -> R.string.feedback_keep_your_arms_straight
        "Raise arms above shoulders" -> R.string.feedback_raise_your_arms_above_your_shoulders
        "Stand straight" -> R.string.feedback_keep_your_torso_upright
        "Gesture ready, control each rep" -> R.string.feedback_position_ready_control_each_repetition
        "Exercise not yet supported" -> R.string.feedback_camera_detection_is_not_yet_available_for_this_exercise
        else -> null
    }
}
