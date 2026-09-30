package pt.ipc.http.controllers.patients

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import pt.ipc.domain.User
import pt.ipc.domain.patient.PatientOutput
import pt.ipc.domain.exceptions.ForbiddenRequest
import pt.ipc.domain.exercises.Exercise
import pt.ipc.domain.physiotherapist.RatingInput
import pt.ipc.http.controllers.patients.models.AllPhysiotherapistsAvailableOutput
import pt.ipc.http.controllers.patients.models.ConnectionRequest
import pt.ipc.http.controllers.patients.models.ListOfExercisesOfPatient
import pt.ipc.http.controllers.patients.models.RegisterPatientInput
import pt.ipc.http.controllers.patients.models.RequestIdOutput
import pt.ipc.http.models.emitter.PostedVideo
import pt.ipc.http.models.emitter.RatePhysiotherapist
import pt.ipc.http.pipeline.authentication.Authentication
import pt.ipc.http.pipeline.exceptionHandler.Problem.Companion.PROBLEM_MEDIA_TYPE
import pt.ipc.http.utils.SseEmitterRepository
import pt.ipc.http.utils.Uris
import pt.ipc.services.patientService.PatientsService
import pt.ipc.services.dtos.CredentialsOutput
import pt.ipc.services.dtos.PhysiotherapistOutput
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(produces = ["application/json", PROBLEM_MEDIA_TYPE])
class PatientsController(private val patientsService: PatientsService, private val sseEmitterRepository: SseEmitterRepository) {

    @PostMapping(value = [Uris.PATIENT_REGISTER])
    fun registerPatient(@RequestBody registerPatientInput: RegisterPatientInput): ResponseEntity<CredentialsOutput> {
        val credentialsOutput: CredentialsOutput = patientsService.registerPatient(registerPatientInput)

        return ResponseEntity.status(HttpStatus.CREATED).body(credentialsOutput)
    }

    @Authentication
    @PostMapping(value = [Uris.PATIENT_PHOTO])
    fun addProfilePicture(
        @PathVariable patientID: UUID,
        @RequestParam photo: MultipartFile,
        user: User
    ): ResponseEntity<Unit> {
        if (user.id != patientID) throw ForbiddenRequest

        patientsService.addProfilePicture(patientID = patientID, profilePicture = photo.bytes)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Authentication
    @GetMapping(value = [Uris.PATIENT_PROFILE])
    fun patientProfile(@PathVariable patientID: UUID, user: User): ResponseEntity<PatientOutput> {
        if (user.id != patientID) throw ForbiddenRequest

        val patient = patientsService.getPatientProfile(patientID = patientID)

        return ResponseEntity.ok(patient)
    }

    @Authentication
    @GetMapping(value = [Uris.PHYSIOTHERAPISTS])
    fun searchPhysiotherapistsAvailable(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false, defaultValue = DEFAULT_SKIP) skip: Int,
        @RequestParam(required = false, defaultValue = DEFAULT_LIMIT) limit: Int,
        user: User
    ): ResponseEntity<AllPhysiotherapistsAvailableOutput> {
        val physiotherapists = patientsService.searchPhysiotherapistsAvailable(
            patientID = user.id,
            name = name,
            skip = skip,
            limit = limit
        )

        return ResponseEntity.status(HttpStatus.OK).body(AllPhysiotherapistsAvailableOutput(physiotherapists))
    }

    @Authentication
    @PostMapping(value = [Uris.PHYSIOTHERAPIST_BY_ID])
    fun makeRequestForPhysiotherapist(@PathVariable("physiotherapistID") physiotherapistID: UUID, @RequestBody connRequest: ConnectionRequest, user: User): ResponseEntity<RequestIdOutput> {
        if (user.id != connRequest.patientID) throw ForbiddenRequest

        val requestPhysiotherapist = patientsService.requestPhysiotherapist(physiotherapistID = physiotherapistID, patientID = connRequest.patientID, requestText = connRequest.text)

        sseEmitterRepository.send(userID = physiotherapistID, obj = requestPhysiotherapist)

        return ResponseEntity.status(HttpStatus.CREATED).body(RequestIdOutput(requestID = requestPhysiotherapist.requestID))
    }

    @Authentication
    @GetMapping(value = [Uris.PATIENT_PHYSIOTHERAPIST])
    fun getPhysiotherapistOfPatient(@PathVariable patientID: UUID, user: User): ResponseEntity<PhysiotherapistOutput> {
        if (user.id != patientID) throw ForbiddenRequest

        val physiotherapistOutput = patientsService.getPhysiotherapistOfPatient(patientID = patientID)

        return ResponseEntity.status(HttpStatus.OK).body(physiotherapistOutput)
    }

    @Authentication
    @DeleteMapping(value = [Uris.PATIENT_PHYSIOTHERAPIST])
    fun endPhysiotherapistConnection(@PathVariable patientID: UUID, user: User): ResponseEntity<Unit> {
        if (user.id != patientID) throw ForbiddenRequest
        patientsService.deleteConnection(patientID = patientID)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Authentication
    @GetMapping(value = [Uris.EXERCISES_OF_PATIENT])
    fun getExercisesOfPatient(
        @PathVariable patientID: UUID,
        @RequestParam(required = false)
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        date: LocalDate?,
        @RequestParam(required = false, defaultValue = DEFAULT_SKIP) skip: Int,
        @RequestParam(required = false, defaultValue = DEFAULT_LIMIT) limit: Int,
        user: User
    ): ResponseEntity<ListOfExercisesOfPatient> {
        if (user.id != patientID) throw ForbiddenRequest
        val exercises: List<Exercise> = patientsService.getExercisesOfPatient(patientID = patientID, date = date, skip = skip, limit = limit)
        return ResponseEntity.ok(ListOfExercisesOfPatient(exercises = exercises))
    }

    @Authentication
    @PostMapping(value = [Uris.PHYSIOTHERAPIST_RATE])
    fun ratePhysiotherapist(@PathVariable("physiotherapistID") physiotherapistID: UUID, @RequestBody ratingInput: RatingInput, user: User): ResponseEntity<Unit> {
        if (ratingInput.user != user.id) throw ForbiddenRequest

        patientsService.ratePhysiotherapist(physiotherapistID = physiotherapistID, patientID = user.id, rating = ratingInput.rating)

        sseEmitterRepository.send(userID = physiotherapistID, obj = RatePhysiotherapist(rate = ratingInput.rating))

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Authentication
    @PostMapping(value = [Uris.VIDEO_OF_EXERCISE])
    fun postVideoOfExercise(
        @RequestParam video: MultipartFile,
        @RequestParam set: Int,
        @RequestParam(required = false) feedBack: String?,
        @RequestParam(required = false) executionMode: String?,
        @RequestParam(required = false) withLoad: Boolean?,
        @RequestParam(required = false) loadValue: Float?,
        @RequestParam(required = false) loadUnit: String?,
        @RequestParam(required = false) executionScore: String?,
        @PathVariable patientID: UUID,
        @PathVariable planID: Int,
        @PathVariable dailyListID: Int,
        @PathVariable exerciseID: Int,
        user: User
    ): ResponseEntity<Unit> {
        if (user.id != patientID) throw ForbiddenRequest

        val (physiotherapistID, postedVideo) = patientsService.uploadVideoOfPatient(
            video = video.bytes,
            patientID = patientID,
            planID = planID,
            dailyListID = dailyListID,
            exerciseID = exerciseID,
            set = set,
            feedback = feedBack,
            executionMode = executionMode,
            withLoad = withLoad,
            loadValue = loadValue,
            loadUnit = loadUnit,
            executionScore = executionScore
        )

        postedVideo?.let {
            sseEmitterRepository.send(userID = physiotherapistID, obj = PostedVideo(patientID = it.patientID, name = it.name, exerciseID = it.exerciseID))
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    companion object {
        const val DEFAULT_SKIP = "0"
        const val DEFAULT_LIMIT = "10"
    }
}
