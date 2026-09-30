package pt.ipc.domain.exceptions

abstract class Forbidden(msg: String) : Exception(msg)

object ForbiddenRequest : Forbidden("You cannot access this resource")
object PhysiotherapistNotVerified : Forbidden("You have to wait for your document to be verified")
