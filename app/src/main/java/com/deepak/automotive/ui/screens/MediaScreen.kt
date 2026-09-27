package com.deepak.automotive.ui.screens

import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deepak.automotive.media.MediaClient
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.CodeBlock
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors
import kotlinx.coroutines.delay

@Composable
fun MediaScreen() {
    val context = LocalContext.current
    val client = remember { MediaClient(context.applicationContext) }
    DisposableEffect(client) {
        client.connect()
        onDispose { client.disconnect() }
    }
    val tree by client.tree.collectAsStateWithLifecycle()
    val meta by client.metadata.collectAsStateWithLifecycle()
    val playback by client.playback.collectAsStateWithLifecycle()
    val playing = playback?.state == PlaybackStateCompat.STATE_PLAYING

    Panel("Now playing (rendered from MediaSession metadata)") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Equalizer(playing)
            AnimatedContent(meta?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID),
                transitionSpec = { slideInHorizontally { it / 2 } + fadeIn() togetherWith fadeOut() }, label = "track") { _ ->
                Column {
                    Text(meta?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "Nothing playing",
                        style = MaterialTheme.typography.titleLarge, color = CarColors.TextPrimary)
                    Text(meta?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "Pick a track below",
                        color = CarColors.TextSecondary)
                }
            }
        }
        Progress(playback, meta?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0L)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("⏮") { client.previous() }
            CarButton(if (playing) "⏸ Pause" else "▶ Play", selected = playing, accent = CarColors.Pink) { client.togglePlayPause() }
            CarButton("⏭") { client.next() }
        }
    }

    Panel("Browse tree (from onGetRoot / onLoadChildren)") {
        tree[client.rootId].orEmpty().forEach { category ->
            Text("📁 ${category.title}", style = MaterialTheme.typography.titleMedium, color = CarColors.Pink)
            tree[category.id].orEmpty().forEach { item ->
                val isCurrent = item.id == meta?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                Text("   ${if (isCurrent) "🔊" else "🎵"} ${item.title} — ${item.subtitle}",
                    color = if (isCurrent) CarColors.Cyan else CarColors.TextPrimary,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { client.play(item.id) }.padding(10.dp))
            }
        }
    }

    Panel("Who draws what?") {
        CodeBlock(
            """
            Car Media Center (OEM UI)
               │ MediaBrowser.connect()      ─▶ CarMediaBrowserService.onGetRoot()
               │ subscribe("root")           ─▶ onLoadChildren("root")  → categories
               │ subscribe("drive")          ─▶ onLoadChildren("drive") → playable tracks
               │ controller.playFromMediaId  ─▶ MediaSession.Callback.onPlayFromMediaId()
               ▼
            Steering wheel ⏭  ─▶ KeyEvent ─▶ active MediaSession.onSkipToNext()
            """
        )
    }
}

/** Five bars bouncing with different periods = a cheap, convincing equalizer. */
@Composable
private fun Equalizer(playing: Boolean) {
    val t = rememberInfiniteTransition(label = "eq")
    val periods = listOf(420, 300, 520, 360, 460)
    Row(Modifier.height(64.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        periods.forEachIndexed { i, p ->
            val h by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(p), RepeatMode.Reverse), label = "bar$i")
            Box(Modifier.width(10.dp).height(64.dp * (if (playing) h else 0.15f))
                .clip(RoundedCornerShape(4.dp)).background(CarColors.Pink))
        }
    }
}

/** Position is extrapolated from (position, updateTime, speed) - no polling of the service. */
@Composable
private fun Progress(state: PlaybackStateCompat?, duration: Long) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state) {
        while (state?.state == PlaybackStateCompat.STATE_PLAYING) { now = SystemClock.elapsedRealtime(); delay(250) }
    }
    val pos = when {
        state == null -> 0L
        state.state == PlaybackStateCompat.STATE_PLAYING ->
            state.position + ((now - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
        else -> state.position
    }
    val fraction = if (duration > 0) (pos.toFloat() / duration).coerceIn(0f, 1f) else 0f
    LinearProgressIndicator(progress = { fraction }, color = CarColors.Pink,
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
    Text("%d:%02d / %d:%02d".format(pos / 60000, pos / 1000 % 60, duration / 60000, duration / 1000 % 60),
        color = CarColors.TextSecondary, modifier = Modifier.size(width = 200.dp, height = 24.dp))
}
