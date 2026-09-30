package pt.ipc.domain.exceptions

abstract class NotFound(msg: String) : Exception(msg)

object UserNotExists : NotFound("This User does Not Exists")
object PhysiotherapistNotFound : NotFound("Physiotherapist not found")
object PlanNotFound : NotFound("Plan not found")
object RequestNotExists : NotFound("This Request does not exists")
object PatientDontHavePlan : NotFound("This Patient does not have a plan assigned")
object PatientNotPostedVideo : NotFound("Patient has not posted the video yet")
object ExerciseNotExists : NotFound("This exercise does not exists")
object FileDoesNotExists : NotFound("This file does not exists")
object PatientDontHaveThisExercise : NotFound("You don't have this exercise")
