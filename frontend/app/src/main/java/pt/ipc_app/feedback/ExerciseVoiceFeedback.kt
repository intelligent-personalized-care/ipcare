package pt.ipc_app.feedback

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class ExerciseVoiceFeedback(context: Context) : DefaultLifecycleObserver {
    private val preferences = context.applicationContext.getSharedPreferences("exercise_voice", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val gate = VoiceFeedbackGate()
    private var engine: TextToSpeech? = null
    private var active = false
    private var ready = false
    private var closed = false
    private val _enabled = MutableStateFlow(preferences.getBoolean("enabled", true))
    val enabled = _enabled.asStateFlow()
    private val _unavailable = MutableStateFlow(false)
    val unavailable = _unavailable.asStateFlow()

    private val speechLocale = context.resources.configuration.locales[0].let {
        if (it.language == "pt") Locale("pt", "PT") else Locale.ENGLISH
    }

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            handler.post {
                if (!closed) {
                    val tts = engine
                    ready = status == TextToSpeech.SUCCESS && tts != null &&
                        tts.setLanguage(speechLocale) >= TextToSpeech.LANG_AVAILABLE
                    if (ready) {
                        // Prefer an installed local voice; availability is determined by the phone's engine.
                        tts?.voices?.firstOrNull {
                            it.locale.language == speechLocale.language && !it.isNetworkConnectionRequired
                        }?.let { tts?.voice = it }
                        tts?.setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    }
                    _unavailable.value = !ready
                }
            }
        }
    }

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        preferences.edit().putBoolean("enabled", value).apply()
        if (!value) engine?.stop()
        gate.reset()
    }

    fun speak(message: String, important: Boolean = false) {
        // Initialisation is asynchronous: discard old instructions instead of replaying them later.
        if (!active || !ready || closed || !_enabled.value || message.isBlank()) return
        val tts = engine ?: return
        if (!important && tts.isSpeaking) return
        if (!gate.accept(message, SystemClock.elapsedRealtime(), important)) return
        if (tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "exercise-feedback") == TextToSpeech.ERROR) {
            _unavailable.value = true
        }
    }

    override fun onResume(owner: LifecycleOwner) { active = true }
    override fun onPause(owner: LifecycleOwner) {
        active = false
        engine?.stop()
        gate.reset()
    }
    override fun onDestroy(owner: LifecycleOwner) { close() }

    fun close() {
        closed = true
        active = false
        handler.removeCallbacksAndMessages(null)
        engine?.stop()
        engine?.shutdown()
        engine = null
    }
}
