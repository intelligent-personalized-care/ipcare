package pt.ipc_app.ui.screens.exercises.sensor

import pt.ipc_app.R

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import pt.ipc_app.feedback.ExerciseVoiceFeedback
import pt.ipc_app.feedback.VoiceCues
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.ble.BleViewModel
import pt.ipc_app.ui.setAppContentPatient
import pt.ipc_app.utils.viewModelInit

class SensorActivity : ComponentActivity() {

    private val viewModel by viewModels<BleViewModel> {
        viewModelInit {
            BleViewModel(applicationContext)
        }
    }

    companion object {
        const val EXTRA_EXERCISE = "sensor_exercise"
        const val EXTRA_ROLL_RAISE_DEG = "sensor_roll_raise_deg"
        const val EXTRA_ROLL_LOWER_DEG = "sensor_roll_lower_deg"

        /** Chart placeholders only; execution requires the selected backend profile. */
        fun navigate(
            context: Context,
            exercise: Exercise,
            rollRaiseTargetDeg: Float = 60f,
            rollLowerTargetDeg: Float = 25f
        ) {
            val intent = Intent(context, SensorActivity::class.java).apply {
                putExtra(EXTRA_EXERCISE, exercise)
                putExtra(EXTRA_ROLL_RAISE_DEG, rollRaiseTargetDeg)
                putExtra(EXTRA_ROLL_LOWER_DEG, rollLowerTargetDeg)
            }
            context.startActivity(intent)
        }
    }

    @Suppress("deprecation")
    private val exercise: Exercise by lazy {
        val exe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(EXTRA_EXERCISE, Exercise::class.java)
        else
            intent.getParcelableExtra(EXTRA_EXERCISE)
        checkNotNull(exe)
    }

    private val rollRaiseTargetDeg: Float
        get() = intent.getFloatExtra(EXTRA_ROLL_RAISE_DEG, 60f)

    private val rollLowerTargetDeg: Float
        get() = intent.getFloatExtra(EXTRA_ROLL_LOWER_DEG, 25f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val voice = ExerciseVoiceFeedback(this)
        lifecycle.addObserver(voice)
        viewModel.configureExercise(exercise)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var previous = viewModel.uiState.value
                viewModel.uiState.collect { state ->
                    val before = previous
                    previous = state
                    when {
                        before.connected && !state.connected -> voice.speak(getString(R.string.feedback_sensor_connection_lost_check_the_connection_before_continuing), true)
                        state.lastError != null && state.lastError != before.lastError -> voice.speak(getString(R.string.feedback_unable_to_complete_the_operation_check_the_message_on_screen), true)
                        state.exerciseComplete && !before.exerciseComplete -> voice.speak(getString(R.string.feedback_exercise_complete_all_sets_have_been_saved), true)
                        state.sessionReadyToSave && !before.sessionReadyToSave -> voice.speak(getString(R.string.feedback_set_complete_save_the_result_to_continue), true)
                        state.sensorCalibrating && !before.sensorCalibrating -> voice.speak(getString(R.string.feedback_calibration_keep_the_sensors_still_in_the_neutral_position), true)
                        before.sensorCalibrating && !state.sensorCalibrating && state.sensorCalibrated -> voice.speak(getString(R.string.feedback_calibration_complete_you_can_start_the_set), true)
                        state.sessionRunning && !before.sessionRunning -> voice.speak(getString(R.string.voice_start_set, state.currentSet), true)
                        state.sessionRunning && (state.sensorReps ?: 0) > (before.sensorReps ?: 0) -> voice.speak(getString(R.string.voice_repetition, state.sensorReps ?: 0), true)
                        state.sessionRunning -> VoiceCues.sensor(state.sensorStatus)?.let {
                            voice.speak(getString(it), state.sensorStatus.startsWith("LIMIT"))
                        }
                        state.servicesDiscovered && !before.servicesDiscovered -> voice.speak(getString(R.string.feedback_sensors_connected_calibrate_before_starting))
                    }
                }
            }
        }

        setAppContentPatient(viewModel) {
            SensorScreen(
                voice = voice,
                viewModel = viewModel,
                exerciseTitle = exercise.exeTitle,
                exerciseDescription = exercise.exeDescription,
                targetReps = exercise.exeReps.takeIf { it > 0 } ?: 10,
                targetSets = exercise.exeSets.takeIf { it > 0 } ?: 1,
                rollRaiseTargetDeg = rollRaiseTargetDeg,
                rollLowerTargetDeg = rollLowerTargetDeg,
                onExit = { finish() }
            )
        }
    }
}
