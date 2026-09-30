package pt.ipc.services.dtos

import java.time.LocalDate
import java.util.*

data class DocAuthenticity(val physiotherapistID: UUID, val dateSubmit: LocalDate)
