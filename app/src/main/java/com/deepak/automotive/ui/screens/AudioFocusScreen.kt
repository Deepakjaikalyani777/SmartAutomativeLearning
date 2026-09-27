package com.deepak.automotive.ui.screens

import android.media.AudioManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepak.automotive.core.audio.AudioFocusRequester
import com.deepak.automotive.core.audio.CarAudioUsage
import com.deepak.automotive.core.audio.FocusMatrix
import com.deepak.automotive.core.audio.Interaction
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioFocusScreen() {
    val context = LocalContext.current
    val requester = remember { AudioFocusRequester(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { requester.abandon() } }

    // Simulated mixer: what is holding focus and what just interrupted it.
    var holder by remember { mutableStateOf(CarAudioUsage.MEDIA) }
    var interrupter by remember { mutableStateOf<CarAudioUsage?>(null) }
    var log by remember { mutableStateOf("Music is playing. Tap an event.") }
    val interaction = interrupter?.let { FocusMatrix.interaction(holder, it) }

    LaunchedEffect(interrupter) {
        if (interrupter != null && interaction != Interaction.REJECT) {
            delay(if (interrupter == CarAudioUsage.CALL) 6000 else 3000) // prompt / call finishes
            requester.abandon()
            log = "${interrupter!!.label} finished → focus returns to ${holder.label}"
            interrupter = null
        }
    }

    val holderVolume by animateFloatAsState(
        when (interaction) {
            null, Interaction.REJECT -> 1f
            Interaction.CONCURRENT_DUCK -> 0.3f
            Interaction.EXCLUSIVE -> 0f
        }, tween(600), label = "holder",
    )
    val interrupterVolume by animateFloatAsState(
        if (interrupter != null && interaction != Interaction.REJECT) 1f else 0f, tween(600), label = "int",
    )

    Panel("Car mixer") {
        Row(Modifier.height(220.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Bottom) {
            VolumeBar(holder.label, holderVolume, CarColors.Pink)
            VolumeBar(interrupter?.label ?: "—", interrupterVolume, CarColors.Orange)
        }
        Text(log, color = CarColors.TextPrimary, fontWeight = FontWeight.SemiBold)
    }

    Panel("Trigger an audio event") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CarAudioUsage.entries.filter { it != CarAudioUsage.MEDIA }.forEach { usage ->
                CarButton(usage.label, accent = CarColors.Orange, selected = interrupter == usage) {
                    val result = FocusMatrix.interaction(holder, usage)
                    // Also issue the REAL request so you can watch `adb shell dumpsys audio`.
                    val granted = requester.request(usage) {} == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                    log = when (result) {
                        Interaction.CONCURRENT_DUCK -> "${usage.label}: CONCURRENT → ${holder.label} ducks (granted=$granted)"
                        Interaction.EXCLUSIVE -> "${usage.label}: EXCLUSIVE → ${holder.label} pauses (granted=$granted)"
                        Interaction.REJECT -> "${usage.label}: REJECTED while ${holder.label} is active"
                    }
                    interrupter = usage
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("Holder: Music", selected = holder == CarAudioUsage.MEDIA) { holder = CarAudioUsage.MEDIA; interrupter = null }
            CarButton("Holder: Phone call", selected = holder == CarAudioUsage.CALL) { holder = CarAudioUsage.CALL; interrupter = null }
        }
        Text("Interview gold: on a call, try 'Music' style requests (Voice assistant) → REJECT. Nav prompt → ducks the call.",
            color = CarColors.TextSecondary)
    }
}

@Composable
private fun VolumeBar(label: String, level: Float, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(56.dp).height(170.dp).clip(RoundedCornerShape(12.dp)).background(CarColors.SurfaceHigh),
            contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.width(56.dp).fillMaxHeight(level.coerceIn(0.001f, 1f)).background(color))
        }
        Text(label, color = CarColors.TextPrimary)
    }
}
