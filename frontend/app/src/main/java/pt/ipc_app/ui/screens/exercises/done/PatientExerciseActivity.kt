package pt.ipc_app.ui.screens.exercises.done

import androidx.compose.ui.res.stringResource
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.domain.exercise.ExerciseTotalInfo
import pt.ipc_app.domain.user.isPatient
import pt.ipc_app.ui.screens.exercises.ExercisesViewModel
import pt.ipc_app.ui.setAppContentPatient
import pt.ipc_app.ui.setAppContentPhysiotherapist
import pt.ipc_app.utils.viewModelInit
import java.util.*

class PatientExerciseActivity: ComponentActivity() {

    private val repo by lazy {
        (application as DependenciesContainer).sessionManager
    }

    private val viewModel by viewModels<ExercisesViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            ExercisesViewModel(app.services.exercisesService, app.sessionManager)
        }
    }

    companion object {
        const val EXERCISE_TOTAL_INFO = "EXERCISE_TOTAL_INFO"
        const val PATIENT_ID = "PATIENT_ID_OF_EXERCISE"
        fun navigate(context: Context, exercise: ExerciseTotalInfo, patientId: UUID) {
            with(context) {
                val intent = Intent(this, PatientExerciseActivity::class.java)
                intent.putExtra(EXERCISE_TOTAL_INFO, exercise)
                intent.putExtra(PATIENT_ID, patientId.toString())
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadExecution(patientId, exercise)

        val content: @Composable () -> Unit = {
            val set = viewModel.nrSetToSee.collectAsState().value

            PatientExerciseScreen(
                exercise = exercise,
                isPatient = repo.userUUID.toString() == patientId,
                patientExerciseUrl = viewModel.getExerciseVideoOfPatientUrl(
                    patientId = patientId,
                    planId = exercise.planId,
                    dailyListId = exercise.dailyListId,
                    exerciseId = exercise.exercise.id
                ),
                setSelected = set,
                accessToken = repo.userLoggedIn.accessToken,
                sensorResult = viewModel.sensorResult.collectAsState().value,
                sensorProfile = viewModel.sensorProfile.collectAsState().value,
                executionMode = viewModel.executionMode.collectAsState().value,
                withLoad = viewModel.executionWithLoad.collectAsState().value,
                loadValue = viewModel.executionLoadValue.collectAsState().value,
                loadUnit = viewModel.executionLoadUnit.collectAsState().value,
                executionMessage = viewModel.executionMessage.collectAsState().value?.let { stringResource(it) }.orEmpty(),
                onResetProfile = { viewModel.resetSensorProfile(patientId, exercise) },
                onSaveProfile = { viewModel.saveSensorProfile(patientId, exercise, it) },
                onSetSelected = {
                    viewModel.selectSetToSee(it)
                    viewModel.loadExecution(patientId, exercise)
                },
                feedbackReceived = viewModel.exerciseVideoFeedBack.collectAsState().value,
                ratingReceived = viewModel.feedbackRating.collectAsState().value,
                feedbackSaving = viewModel.feedbackSaving.collectAsState().value,
                onFeedbackSent = { text, rating ->
                    viewModel.sendFeedbackToExerciseDone(
                        patientId = patientId,
                        planId = exercise.planId,
                        dailyListId = exercise.dailyListId,
                        exerciseId = exercise.exercise.id,
                        feedback = text,
                        rating = rating
                    )
                }
            )
        }

        if (repo.userLoggedIn.role.isPatient()) {
            setAppContentPatient(
                viewModel = viewModel,
                content = content
            )
        }
        else
            setAppContentPhysiotherapist(
                viewModel = viewModel,
                content = content
            )

    }

    @Suppress("deprecation")
    private val exercise: ExerciseTotalInfo by lazy {
        val exe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(EXERCISE_TOTAL_INFO, ExerciseTotalInfo::class.java)
        else
            intent.getParcelableExtra(EXERCISE_TOTAL_INFO)
        checkNotNull(exe)
    }

    @Suppress("deprecation")
    private val patientId: String by lazy {
        val cId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getStringExtra(PATIENT_ID)
        else
            intent.getStringExtra(PATIENT_ID)
        checkNotNull(cId)
    }

}
