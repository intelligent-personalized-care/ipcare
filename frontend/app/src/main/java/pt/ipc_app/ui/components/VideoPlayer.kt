package pt.ipc_app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import pt.ipc_app.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.ui.StyledPlayerView

@Composable
fun VideoPlayer(
    url: String,
    playing: Boolean = true,
    accessToken: String? = null,
    muted: Boolean = false
) {
    val context = LocalContext.current

    val exoPlayer = remember(context, accessToken) {
        val dataSource = DefaultHttpDataSource.Factory().setDefaultRequestProperties(
            accessToken?.let { mapOf("Authorization" to "Bearer $it") } ?: emptyMap()
        )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .build().apply { volume = if (muted) 0f else 1f }
    }

    var fullscreen by remember(url) { mutableStateOf(false) }

    LaunchedEffect(exoPlayer, url) {
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        exoPlayer.prepare()
    }

    LaunchedEffect(exoPlayer, muted) { exoPlayer.volume = if (muted) 0f else 1f }

    LaunchedEffect(exoPlayer, playing) {
        exoPlayer.playWhenReady = playing
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        if (!fullscreen) {
            VideoSurface(exoPlayer, false, { fullscreen = true }, Modifier.matchParentSize())
        }
    }
    if (fullscreen) {
        Dialog(
            onDismissRequest = { fullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val view = LocalView.current
            DisposableEffect(view) {
                val window = (view.parent as? DialogWindowProvider)?.window
                val controller = window?.let { WindowCompat.getInsetsController(it, view) }
                controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller?.hide(WindowInsetsCompat.Type.systemBars())
                onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
            }
            VideoSurface(exoPlayer, true, { fullscreen = false }, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun VideoSurface(player: ExoPlayer, fullscreen: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val playerView = remember(context, player) { StyledPlayerView(context).apply { this.player = player } }
    DisposableEffect(playerView) {
        onDispose { playerView.player = null }
    }
    Box(modifier.background(Color.Black)) {
        AndroidView(factory = { playerView }, modifier = Modifier.matchParentSize())
        IconButton(onClick = onToggle, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(
                imageVector = if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = stringResource(if (fullscreen) R.string.video_exit_fullscreen else R.string.video_fullscreen),
                tint = Color.White
            )
        }
    }
}
