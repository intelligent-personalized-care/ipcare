package pt.ipc.domain.exercises

import pt.ipc.domain.exceptions.BadRequest

class InvalidFeedbackScore : BadRequest("Feedback score must be a whole number from 1 to 5")

fun validateFeedbackScore(score: String?) {
    if (score != null && score !in setOf("1", "2", "3", "4", "5")) throw InvalidFeedbackScore()
}
