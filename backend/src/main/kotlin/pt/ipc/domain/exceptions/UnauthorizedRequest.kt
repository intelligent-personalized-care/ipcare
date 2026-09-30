package pt.ipc.domain.exceptions

abstract class UnauthorizedRequest(msg: String) : Exception(msg)

object Unauthenticated : UnauthorizedRequest("Unauthenticated")
object NotPhysiotherapistOfPatient : UnauthorizedRequest("You are not the Physiotherapist of this Patient")
object HasNotUploadedVideo : UnauthorizedRequest("The patient has not uploaded the video")
object NotPlanOfPhysiotherapist : UnauthorizedRequest("This plan does not belong to you")
object LoginFailed : UnauthorizedRequest("The credentials do not match")
