package pt.ipc.domain.exceptions

abstract class Conflict(msg: String) : Exception(msg)

object PatientAlreadyHavePhysiotherapist : Conflict("You already have a physiotherapist")
object PatientAlreadyHavePlanInThisPeriod : Conflict("Patient Already have Plan in this Period")
object ExerciseAlreadyUploaded : Conflict("You already uploaded a video with this exercise")
