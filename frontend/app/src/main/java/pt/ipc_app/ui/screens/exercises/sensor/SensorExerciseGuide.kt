package pt.ipc_app.ui.screens.exercises.sensor

import pt.ipc_app.R

import java.util.Locale

enum class SensorJoint(val proximal: Int, val distal: Int) {
    WRIST(R.string.feedback_forearm, R.string.feedback_back_of_the_hand),
    ELBOW(R.string.feedback_upper_arm_above_the_elbow, R.string.feedback_forearm_below_the_elbow),
    KNEE(R.string.feedback_thigh_above_the_knee, R.string.feedback_lower_leg_below_the_knee),
    UNKNOWN(R.string.feedback_segment_closest_to_the_torso, R.string.feedback_segment_beyond_the_joint)
}

fun sensorJoint(title: String): SensorJoint {
    val name = title.lowercase(Locale.ROOT)
    return when {
        "wrist" in name || "pulso" in name -> SensorJoint.WRIST
        "elbow" in name || "cotovelo" in name -> SensorJoint.ELBOW
        "knee" in name || "joelho" in name || "squat" in name || "agachamento" in name -> SensorJoint.KNEE
        else -> SensorJoint.UNKNOWN
    }
}

fun sensorStartPosition(title: String): Int? {
    val name = title.lowercase(Locale.ROOT)
    return when (sensorJoint(title)) {
        SensorJoint.WRIST -> R.string.feedback_support_your_forearm_palm_down_with_the_hand_free_beyond_the_edge
        SensorJoint.ELBOW -> if ("extension" in name || "extensão" in name)
            R.string.feedback_support_your_upper_arm_beside_your_body_and_start_with_your_elbow
        else R.string.feedback_keep_your_upper_arm_beside_your_body_and_your_forearm_in_the_comf
        SensorJoint.KNEE -> when {
            "extension" in name || "extensão" in name -> R.string.feedback_sit_on_a_stable_chair_with_your_thigh_supported_and_knee_comforta
            "heel" in name || "calcanhar" in name -> R.string.feedback_lie_on_your_back_with_your_leg_comfortably_extended_calibrate_bef
            else -> R.string.feedback_stand_with_stable_support_and_your_leg_comfortably_straight_calib
        }
        SensorJoint.UNKNOWN -> null
    }
}

/** Progress is acknowledged by the firmware, never inferred from a phone timer. */
fun sensorHoldProgress(status: String): Float? = when {
    status == "HOLD" -> 0f
    status.startsWith("HOLDING ") && status.endsWith("%") ->
        status.removePrefix("HOLDING ").removeSuffix("%").toIntOrNull()
            ?.takeIf { it in 0..100 }?.div(100f)
    else -> null
}

fun sensorMovementInstruction(status: String): Int = when {
    status == "HOLD" || sensorHoldProgress(status) != null -> R.string.feedback_hold_your_position
    status == "HOLD LOST" -> R.string.feedback_position_lost_return_to_the_start_and_repeat
    status == "RETURN TO START" || status == "RETURNING" -> R.string.feedback_hold_complete_return_to_the_starting_position
    status == "RAISING" -> R.string.feedback_move_slowly_towards_the_target_angle
    status == "SET COMPLETE" -> R.string.feedback_set_complete
    status.startsWith("LIMIT") -> R.string.feedback_limit_exceeded_return_to_the_starting_position
    status == "MOVE TO START" -> R.string.feedback_return_to_the_starting_position
    status.startsWith("REP") -> R.string.feedback_repetition_counted_prepare_for_the_next_movement
    else -> R.string.feedback_begin_in_the_starting_position_and_move_slowly
}
