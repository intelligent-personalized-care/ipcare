package pt.ipc.http.controllers.exercises

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import pt.ipc.domain.User
import pt.ipc.domain.exercises.SensorProfile
import pt.ipc.domain.exercises.SensorSessionInput
import pt.ipc.http.pipeline.authentication.Authentication
import pt.ipc.services.exercisesService.SensorSessionsService
import java.util.UUID

@RestController
class SensorSessionsController(private val service: SensorSessionsService) {
    @Authentication
    @GetMapping(value = ["$PATH/progress", "$EXERCISE_PATH/progress"])
    fun progress(
        @PathVariable patientID: UUID,
        @PathVariable planID: Int,
        @PathVariable dailyListID: Int,
        @PathVariable exerciseID: Int,
        user: User
    ): Map<String, List<Int>> =
        service.progress(user.id, patientID, planID, dailyListID, exerciseID)

    @Authentication
    @GetMapping(value = ["$PATH/profile", "$EXERCISE_PATH/profile"])
    fun profile(
        @PathVariable patientID: UUID,
        @PathVariable planID: Int,
        @PathVariable dailyListID: Int,
        @PathVariable exerciseID: Int,
        user: User
    ): SensorProfile =
        service.getProfile(user.id, patientID, planID, dailyListID, exerciseID)

    @Authentication
    @PostMapping("$PATH/profile")
    fun profile(
        @PathVariable patientID: UUID,
        @PathVariable planID: Int,
        @PathVariable dailyListID: Int,
        @PathVariable exerciseID: Int,
        @RequestBody profile: SensorProfile,
        user: User
    ): SensorProfile =
        service.saveProfile(user.id, patientID, planID, dailyListID, exerciseID, profile)

    @Authentication
    @PostMapping("$PATH/profile/defaults")
    fun resetProfile(
        @PathVariable patientID: UUID, @PathVariable planID: Int,
        @PathVariable dailyListID: Int, @PathVariable exerciseID: Int, user: User
    ): SensorProfile = service.resetProfile(user.id, patientID, planID, dailyListID, exerciseID)

    @Authentication
    @PostMapping(PATH)
    fun save(
        @PathVariable patientID: UUID,
        @PathVariable planID: Int,
        @PathVariable dailyListID: Int,
        @PathVariable exerciseID: Int,
        @RequestBody input: SensorSessionInput,
        user: User
    ): SensorSessionInput =
        service.save(user.id, patientID, planID, dailyListID, exerciseID, input)

    @Authentication
    @GetMapping(PATH)
    fun get(
        @PathVariable patientID: UUID,
        @PathVariable planID: Int,
        @PathVariable dailyListID: Int,
        @PathVariable exerciseID: Int,
        @RequestParam set: Int,
        user: User
    ): SensorSessionInput =
        service.get(user.id, patientID, planID, dailyListID, exerciseID, set)

    companion object {
        const val EXERCISE_PATH = "/users/patients/{patientID}/plans/{planID}/daily_lists/{dailyListID}/exercises/{exerciseID}"
        const val PATH = "$EXERCISE_PATH/sensor"
    }
}
