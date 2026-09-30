package pt.ipc_app.ui.screens.exercises.selection

import androidx.compose.runtime.collectAsState
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.mlkit.vision.CameraXLivePreviewActivity
import pt.ipc_app.ui.screens.exercises.ExercisesViewModel
import pt.ipc_app.ui.screens.exercises.sensor.SensorActivity
import pt.ipc_app.ui.setAppContentPatient
import pt.ipc_app.utils.viewModelInit

class ChooseCameraOrSensorActivity : ComponentActivity() {

    private val viewModel by viewModels<ExercisesViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            ExercisesViewModel(app.services.exercisesService, app.sessionManager)
        }
    }

    companion object {
        const val EXERCISE = "EXERCISE_TO_DO"
        fun navigate(context: Context, exercise: Exercise) {
            with(context) {
                val intent = Intent(this, ChooseCameraOrSensorActivity::class.java)
                intent.putExtra(EXERCISE, exercise)
                startActivity(intent)
            }
        }
    }

    @Suppress("deprecation")
    private val exercise: Exercise by lazy {
        val exe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(EXERCISE, Exercise::class.java)
        else
            intent.getParcelableExtra(EXERCISE)
        checkNotNull(exe)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.loadModalities(exercise.exeID)
        setAppContentPatient(viewModel) {
            val info = viewModel.modalityInfo.collectAsState().value
            ChooseCameraOrSensorScreen(
                cameraEnabled = info?.supportsCamera == true,
                sensorsEnabled = info?.supportsSensors == true,
                loading = info == null,
                onCameraSelected = {
                    CameraXLivePreviewActivity.navigate(this, exercise)
                },
                onSensorSelected = {
                    SensorActivity.navigate(this, exercise)
                }
            )
        }
    }
}
