package pt.ipc_app.ui.screens.exercises

import pt.ipc_app.R
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import pt.ipc_app.domain.exercise.ExerciseTotalInfo
import pt.ipc_app.service.models.exercises.SensorSession
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.utils.executeRequest
import kotlinx.coroutines.flow.asStateFlow
import pt.ipc_app.domain.exercise.ExerciseInfo
import pt.ipc_app.ui.screens.exercises.info.ExerciseActivity
import pt.ipc_app.ui.screens.exercises.list.ExercisesListActivity
import pt.ipc_app.service.ExercisesService
import pt.ipc_app.service.models.sse.PhysiotherapistFeedBack
import pt.ipc_app.service.models.sse.SseEvent
import pt.ipc_app.service.sse.EventBus
import pt.ipc_app.service.sse.SseEventListener
import pt.ipc_app.preferences.SessionManagerSharedPrefs
import pt.ipc_app.ui.components.ProgressState
import pt.ipc_app.ui.screens.AppViewModel
import java.util.*

/**
 * View model for the [ExerciseActivity] and [ExercisesListActivity].
 *
 * @param sessionManager the manager used to handle the user session
 */
class ExercisesViewModel(
    private val exercisesService: ExercisesService,
    private val sessionManager: SessionManagerSharedPrefs
) : AppViewModel(), SseEventListener {
    val modalityInfo = MutableStateFlow<ExerciseInfo?>(null)
    fun loadModalities(id: UUID) {
        modalityInfo.value = null
        launchAndExecuteRequest(
            request = { exercisesService.getExerciseInfo(id, sessionManager.userLoggedIn.accessToken) },
            onSuccess = { modalityInfo.value = it }
        )
    }

    val sensorResult = MutableStateFlow<SensorSession?>(null)
    val sensorProfile = MutableStateFlow<SensorProfile?>(null)
    val executionMode = MutableStateFlow<String?>(null)
    val executionLoadValue = MutableStateFlow<Float?>(null)
    val executionLoadUnit = MutableStateFlow<String?>(null)
    val executionWithLoad = MutableStateFlow<Boolean?>(null)
    val executionMessage = MutableStateFlow<Int?>(null)
    private var executionJob: Job? = null
    val feedbackRating = MutableStateFlow<Int?>(null)
    val feedbackSaving = MutableStateFlow(false)
    private var viewedExerciseId: Int? = null

    fun loadExecution(patientId: String, exercise: ExerciseTotalInfo) {
        executionJob?.cancel()
        viewedExerciseId = exercise.exercise.id
        feedbackRating.value = null
        sensorResult.value = null
        sensorProfile.value = null
        executionMode.value = null
        executionWithLoad.value = null
        executionLoadValue.value = null
        executionLoadUnit.value = null
        _exerciseVideoFeedBack.value = null
        executionMessage.value = R.string.execution_loading
        val selectedSet = nrSetToSee.value
        executionJob = viewModelScope.launch {
            try {
                val token = sessionManager.userLoggedIn.accessToken
                val profile = exercisesService.getSensorProfile(patientId, exercise.planId, exercise.dailyListId, exercise.exercise.id, token)
                if (profile is APIResult.Success) sensorProfile.value = profile.data
                val progress = executeRequest { exercisesService.getSensorProgress(patientId, exercise.planId, exercise.dailyListId, exercise.exercise.id, token) }
                if (selectedSet !in progress.completedSets) {
                    executionMessage.value = R.string.execution_not_complete
                    return@launch
                }
                val feedback = executeRequest { exercisesService.getFeedbackOfPhysiotherapist(patientId, exercise.planId, exercise.dailyListId, exercise.exercise.id, selectedSet, token) }
                _exerciseVideoFeedBack.value = feedback.physiotherapistFeedBack
                feedbackRating.value = feedback.physiotherapistFeedbackScore?.toIntOrNull()?.takeIf { it in 1..5 }
                executionMode.value = feedback.executionMode ?: "camera"
                executionWithLoad.value = feedback.withLoad
                executionLoadValue.value = feedback.loadValue
                executionLoadUnit.value = feedback.loadUnit
                if (feedback.executionMode == "sensor") {
                    sensorResult.value = executeRequest { exercisesService.getSensorSession(patientId, exercise.planId, exercise.dailyListId, exercise.exercise.id, selectedSet, token) }
                }
                executionMessage.value = null
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                executionMessage.value = R.string.execution_load_failed
            }
        }
    }

    fun resetSensorProfile(patientId: String, exercise: ExerciseTotalInfo) {
        launchAndExecuteRequest(request = {
            exercisesService.resetSensorProfile(patientId, exercise.planId, exercise.dailyListId, exercise.exercise.id, sessionManager.userLoggedIn.accessToken)
        }, onSuccess = { sensorProfile.value = it; executionMessage.value = R.string.execution_defaults_restored })
    }

    fun saveSensorProfile(patientId: String, exercise: ExerciseTotalInfo, profile: SensorProfile) {
        launchAndExecuteRequest(request = {
            exercisesService.saveSensorProfile(patientId, exercise.planId, exercise.dailyListId, exercise.exercise.id,
                profile, sessionManager.userLoggedIn.accessToken)
        }, onSuccess = { sensorProfile.value = it; executionMessage.value = R.string.execution_profile_saved })
    }

    private val _state = MutableStateFlow(ProgressState.IDLE)
    val state
        get() = _state.asStateFlow()

    private val _exercises = MutableStateFlow(listOf<ExerciseInfo>())
    val exercises
        get() = _exercises.asStateFlow()

    private var _nrSetToSee = MutableStateFlow(1)
    val nrSetToSee
        get() = _nrSetToSee.asStateFlow()

    private var _exerciseVideoFeedBack = MutableStateFlow<String?>(null)
    val exerciseVideoFeedBack
        get() = _exerciseVideoFeedBack.asStateFlow()

    /**
     * Attempts to get a preview url of an exercise.
     */
    fun getExercisePreviewUrl(
        exerciseInfoId: UUID
    ): String =
        exercisesService.getExercisePreviewUrl(exerciseInfoId)

    /**
     * Attempts to get a patient video url of an exercise.
     */
    fun getExerciseVideoOfPatientUrl(
        patientId: String,
        planId: Int,
        dailyListId: Int,
        exerciseId: Int
    ): String =
        exercisesService.getExerciseVideoOfPatientUrl(
            patientId = patientId,
            planId = planId,
            dailyListId = dailyListId,
            exerciseId = exerciseId,
            set = nrSetToSee.value
        )

    fun selectSetToSee(set: Int) { _nrSetToSee.value = set }

    fun getFeedbackOfPhysiotherapist(
        planId: Int,
        dailyListId: Int,
        exerciseId: Int
    ) {
        launchAndExecuteRequest(
            request = {
                exercisesService.getFeedbackOfPhysiotherapist(
                    patientId = sessionManager.userUUID.toString(),
                    planId = planId,
                    dailyListId = dailyListId,
                    exerciseId = exerciseId,
                    set = nrSetToSee.value,
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = {
                _exerciseVideoFeedBack.value = it.physiotherapistFeedBack
                feedbackRating.value = it.physiotherapistFeedbackScore?.toIntOrNull()?.takeIf { it in 1..5 }
            }
        )
    }

    fun sendFeedbackToExerciseDone(
        patientId: String, planId: Int, dailyListId: Int, exerciseId: Int,
        feedback: String, rating: Int?
    ) {
        if (feedbackSaving.value) return
        val selectedSet = nrSetToSee.value
        feedbackSaving.value = true
        viewModelScope.launch {
            try {
                executeRequest {
                    exercisesService.sendFeedbackToExerciseDone(patientId, planId, dailyListId,
                        exerciseId, selectedSet, feedback, sessionManager.userLoggedIn.accessToken, rating)
                }
                if (viewedExerciseId == exerciseId && nrSetToSee.value == selectedSet) {
                    _exerciseVideoFeedBack.value = feedback
                    feedbackRating.value = rating
                    executionMessage.value = R.string.execution_feedback_saved
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                executionMessage.value = R.string.execution_feedback_failed
            } finally {
                feedbackSaving.value = false
            }
        }
    }

    val selectedJoint = MutableStateFlow<String?>(null)
    private var catalogueRequest = 0

    fun selectJoint(joint: String?) { selectedJoint.value = joint; _exercises.value = emptyList(); getExercises() }

    fun getExercises(
        skip: Int = 0
    ) {
        val requestId = ++catalogueRequest
        val joint = selectedJoint.value
        launchAndExecuteRequest(
            request = {
                exercisesService.getExercises(
                    skip = skip,
                    joint = joint,
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = {
                if (requestId == catalogueRequest) _exercises.value = it.exercises
            }
        )
    }

    init {
        EventBus.registerListener(this)
    }

    override fun onCleared() {
        EventBus.unregisterListener(this)
        super.onCleared()
    }

    override fun onEventReceived(eventData: SseEvent) {
        if (eventData is PhysiotherapistFeedBack && eventData.exerciseId == viewedExerciseId &&
            eventData.set == nrSetToSee.value) {
            _exerciseVideoFeedBack.value = eventData.feedBack
            feedbackRating.value = eventData.feedbackScore?.toIntOrNull()?.takeIf { it in 1..5 }
        }
    }
}
