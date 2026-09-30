package pt.ipc.http.controllers.physiotherapists

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
import pt.ipc.domain.patient.PatientOfPhysiotherapist
import pt.ipc.domain.exceptions.ForbiddenRequest
import pt.ipc.domain.physiotherapist.PhysiotherapistDetails
import pt.ipc.domain.physiotherapist.PhysiotherapistProfile
import pt.ipc.domain.plan.PlanInput
import pt.ipc.domain.plan.PlanOutput
import pt.ipc.http.controllers.physiotherapists.models.ExercisesOfPatients
import pt.ipc.http.controllers.physiotherapists.models.FeedbackInput
import pt.ipc.http.controllers.physiotherapists.models.ListOfPatients
import pt.ipc.http.controllers.physiotherapists.models.ListOfPlans
import pt.ipc.http.controllers.physiotherapists.models.PlanID
import pt.ipc.http.controllers.physiotherapists.models.PlanToPatient
import pt.ipc.http.controllers.physiotherapists.models.RequestsOfPhysiotherapist
import pt.ipc.http.models.Decision
import pt.ipc.http.models.emitter.PhysiotherapistFeedBack
import pt.ipc.http.models.emitter.PlanAssociation
import pt.ipc.http.models.emitter.RequestAcceptance
import pt.ipc.http.pipeline.authentication.Authentication
import pt.ipc.http.pipeline.exceptionHandler.Problem.Companion.PROBLEM_MEDIA_TYPE
import pt.ipc.http.utils.SseEmitterRepository
import pt.ipc.http.utils.Uris
import pt.ipc.services.dtos.CredentialsOutput
import pt.ipc.services.dtos.RegisterInput
import pt.ipc.services.physiotherapistService.PhysiotherapistService
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(produces = ["application/json", "image/png", PROBLEM_MEDIA_TYPE])
class PhysiotherapistsController(private val physiotherapistService: PhysiotherapistService, private val sseEmitterRepository: SseEmitterRepository) {

    @PostMapping(value = [Uris.PHYSIOTHERAPISTS])
    fun registerPhysiotherapist(@RequestBody registerInput: RegisterInput): ResponseEntity<CredentialsOutput> {
        val registerOutput = physiotherapistService.registerPhysiotherapist(registerInput = registerInput)

        return ResponseEntity.status(HttpStatus.CREATED).body(registerOutput)
    }

    @Authentication
    @PostMapping(value = [Uris.PHYSIOTHERAPIST_CREDENTIAL])
    fun postCredentialOfPhysiotherapist(
        @PathVariable("physiotherapistID") physiotherapistID: UUID,
        @RequestBody credential: MultipartFile,
        user: User
    ): ResponseEntity<Unit> {
        if (physiotherapistID != user.id) throw ForbiddenRequest

        physiotherapistService.insertCredential(physiotherapistID = physiotherapistID, credential = credential.bytes)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Authentication
    @GetMapping(value = [Uris.PHYSIOTHERAPIST_PROFILE])
    fun physiotherapistProfile(@PathVariable("physiotherapistID") physiotherapistID: UUID, user: User): ResponseEntity<PhysiotherapistProfile> {
        if (physiotherapistID != user.id) throw ForbiddenRequest

        val profile = physiotherapistService.getPhysiotherapistProfile(physiotherapistID = physiotherapistID)

        return ResponseEntity.ok(profile)
    }

    @Authentication
    @GetMapping(value = [Uris.PATIENTS_OF_PHYSIOTHERAPIST])
    fun getPatientsOfPhysiotherapist(@PathVariable("physiotherapistID") physiotherapistID: UUID, user: User): ResponseEntity<ListOfPatients> {
        if (user.id != physiotherapistID) throw ForbiddenRequest

        val patients = physiotherapistService.getPatientsOfPhysiotherapist(physiotherapistID = user.id)

        return ResponseEntity.ok(ListOfPatients(patients = patients))
    }

    @Authentication
    @GetMapping(value = [Uris.PATIENT_OF_PHYSIOTHERAPIST])
    fun patientOfPhysiotherapist(
        @PathVariable patientID: UUID,
        @PathVariable("physiotherapistID") physiotherapistID: UUID,
        user: User
    ): ResponseEntity<PatientOfPhysiotherapist> {
        if (user.id != physiotherapistID) throw ForbiddenRequest

        val patient = physiotherapistService.getPatientOfPhysiotherapist(physiotherapistID = physiotherapistID, patientID = patientID)

        return ResponseEntity.ok(patient)
    }

    @Authentication
    @GetMapping(value = [Uris.PHYSIOTHERAPIST_BY_ID])
    fun getPhysiotherapist(@PathVariable("physiotherapistID") physiotherapistID: UUID): ResponseEntity<PhysiotherapistDetails> {
        val physiotherapistDetails = physiotherapistService.getPhysiotherapist(physiotherapistID = physiotherapistID)

        return ResponseEntity.status(HttpStatus.OK).body(physiotherapistDetails)
    }

    @Authentication
    @PostMapping(value = [Uris.PHYSIOTHERAPIST_DECIDE_REQUEST])
    fun decideRequest(
        @PathVariable("physiotherapistID") physiotherapistID: UUID,
        @PathVariable requestID: UUID,
        @RequestBody decision: Decision,
        user: User
    ): ResponseEntity<ListOfPatients> {
        if (user.id != physiotherapistID) throw ForbiddenRequest

        val triple = physiotherapistService.decideRequest(
            requestID = requestID,
            physiotherapistID = physiotherapistID,
            decision = decision.accept
        )

        triple?.let { (patients, patientID, physiotherapistOutput) ->
            sseEmitterRepository.send(userID = patientID, obj = RequestAcceptance(physiotherapist = physiotherapistOutput))
            return ResponseEntity.ok(ListOfPatients(patients = patients))
        }

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Authentication
    @PostMapping(value = [Uris.PHYSIOTHERAPIST_PHOTO])
    fun addProfilePicture(@PathVariable("physiotherapistID") physiotherapistID: UUID, @RequestParam photo: MultipartFile, user: User): ResponseEntity<Unit> {
        if (user.id != physiotherapistID) throw ForbiddenRequest

        physiotherapistService.updateProfilePicture(physiotherapistID = physiotherapistID, photo = photo.bytes)

        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @Authentication
    @GetMapping(value = [Uris.PHYSIOTHERAPIST_REQUESTS])
    fun getPhysiotherapistRequests(@PathVariable("physiotherapistID") physiotherapistID: UUID, user: User): ResponseEntity<RequestsOfPhysiotherapist> {
        if (physiotherapistID != user.id) throw ForbiddenRequest

        val requests = physiotherapistService.physiotherapistRequests(physiotherapistID = physiotherapistID)

        return ResponseEntity.ok(RequestsOfPhysiotherapist(requests))
    }

    @Authentication
    @PostMapping(value = [Uris.PLANS_OF_PHYSIOTHERAPIST])
    fun createPlanOfPhysiotherapist(@PathVariable("physiotherapistID") physiotherapistID: UUID, @RequestBody planInput: PlanInput, user: User): ResponseEntity<PlanID> {
        if (user.id != physiotherapistID) throw ForbiddenRequest

        val planID = physiotherapistService.createPlan(physiotherapistID = physiotherapistID, planInput = planInput)

        return ResponseEntity.status(HttpStatus.CREATED).body(PlanID(planID))
    }

    @Authentication
    @GetMapping(value = [Uris.PLANS_OF_PHYSIOTHERAPIST])
    fun getPlansOfPhysiotherapist(@PathVariable("physiotherapistID") physiotherapistID: UUID, user: User): ResponseEntity<ListOfPlans> {
        if (physiotherapistID != user.id) throw ForbiddenRequest

        val plans = physiotherapistService.getPlans(physiotherapistID = physiotherapistID)

        return ResponseEntity.ok(ListOfPlans(plans = plans))
    }

    @Authentication
    @PostMapping(value = [Uris.PHYSIOTHERAPIST_PATIENT_PLANS])
    fun associatePlanForPatient(
        @PathVariable patientID: UUID,
        @PathVariable("physiotherapistID") physiotherapistID: UUID,
        @RequestBody planInfo: PlanToPatient,
        user: User
    ): ResponseEntity<Unit> {
        if (user.id != physiotherapistID) throw ForbiddenRequest

        val (title, startDate) = physiotherapistService.associatePlanToPatient(
            physiotherapistID = physiotherapistID,
            patientID = patientID,
            startDate = planInfo.startDate,
            planID = planInfo.planID
        )

        sseEmitterRepository.send(userID = patientID, obj = PlanAssociation(title = title, startDate = startDate))

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Authentication
    @GetMapping(value = [Uris.PHYSIOTHERAPIST_PLAN_BY_ID])
    fun getPlanOfPhysiotherapistByID(@PathVariable("physiotherapistID") physiotherapistID: UUID, @PathVariable planID: Int, user: User): ResponseEntity<PlanOutput> {
        if (user.id != physiotherapistID) throw ForbiddenRequest
        val planOutput: PlanOutput = physiotherapistService.getPlan(physiotherapistID = physiotherapistID, planID = planID)
        return ResponseEntity.ok(planOutput)
    }

    @Authentication
    @PostMapping(value = [Uris.EXERCISE_FEEDBACK])
    fun postFeedback(
        @RequestBody feedbackInput: FeedbackInput,
        @PathVariable patientID: UUID,
        @PathVariable planID: Int,
        @PathVariable dailyListID: Int,
        @PathVariable exerciseID: Int,
        user: User
    ): ResponseEntity<Unit> {
        physiotherapistService.giveFeedbackOfExercise(
            physiotherapistID = user.id,
            planID = planID,
            dailyListID = dailyListID,
            dailyExerciseID = exerciseID,
            set = feedbackInput.set,
            feedback = feedbackInput.feedback,
            feedbackScore = feedbackInput.feedbackScore,
            patientID = patientID
        )

        sseEmitterRepository.send(userID = patientID, obj = PhysiotherapistFeedBack(feedBack = feedbackInput.feedback, feedbackScore = feedbackInput.feedbackScore, exerciseId = exerciseID, set = feedbackInput.set))

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Authentication
    @DeleteMapping(value = [Uris.PATIENT_OF_PHYSIOTHERAPIST])
    fun endPatientConnection(@PathVariable("physiotherapistID") physiotherapistID: UUID, @PathVariable patientID: UUID, user: User): ResponseEntity<Unit> {
        if (user.id != physiotherapistID) throw ForbiddenRequest
        physiotherapistService.deleteConnection(physiotherapistID = physiotherapistID, patientID = patientID)

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @Authentication
    @GetMapping(value = [Uris.PHYSIOTHERAPIST_PATIENT_EXERCISES])
    fun exercisesOfPatients(
        @PathVariable("physiotherapistID") physiotherapistID: UUID,
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @RequestParam
        date: LocalDate,
        user: User
    ): ResponseEntity<ExercisesOfPatients> {
        if (user.id != physiotherapistID) throw ForbiddenRequest
        val exercises = physiotherapistService.exercisesOfPatients(physiotherapistID = physiotherapistID, date = date)

        return ResponseEntity.ok(ExercisesOfPatients(patientsExercises = exercises))
    }
}
