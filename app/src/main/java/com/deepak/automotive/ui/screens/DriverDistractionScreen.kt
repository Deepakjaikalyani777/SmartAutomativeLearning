package com.deepak.automotive.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.core.ux.CarUxRestrictionsSource
import com.deepak.automotive.core.ux.UxRestrictionsState
import com.deepak.automotive.core.vehicle.DataSourceType
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DriverDistractionScreen(vm: VehicleViewModel = viewModel()) {
    val vehicle by vm.state.collectAsStateWithLifecycle()
    var demoSpeed by remember { mutableFloatStateOf(0f) }
    val speed = maxOf(demoSpeed, vehicle.speedKmh)

    // On a real car, restrictions come from CarService; otherwise we simulate its decision.
    val context = LocalContext.current
    val realSource = remember {
        if (vm.isAutomotive && vehicle.source == DataSourceType.CAR_API) CarUxRestrictionsSource(context.applicationContext) else null
    }
    DisposableEffect(realSource) {
        realSource?.start()
        onDispose { realSource?.stop() }
    }
    val fallback = remember { MutableStateFlow(UxRestrictionsState.BASELINE) }
    val real by (realSource?.state ?: fallback).collectAsState()
    val ux = if (realSource != null) real else UxRestrictionsState.forSpeed(speed)
    val restricted = ux.requiresDistractionOptimization

    Panel("Driving state") {
        Text("Speed %.0f km/h → %s".format(speed, if (speed > 0.5f) "MOVING" else "PARKED"),
            style = MaterialTheme.typography.titleLarge, color = if (restricted) CarColors.Red else CarColors.Green)
        Slider(demoSpeed, { demoSpeed = it }, valueRange = 0f..120f)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UxRestrictionsState.Restriction.entries.forEach { r ->
                val on = r in ux.activeRestrictions
                val scale by animateFloatAsState(if (on) 1f else 0.9f, spring(Spring.DampingRatioHighBouncy), label = "chip")
                Text(r.meaning, color = if (on) Color.Black else CarColors.TextSecondary, fontSize = 13.sp,
                    modifier = Modifier.scale(scale).clip(RoundedCornerShape(10.dp))
                        .background(if (on) CarColors.Red else CarColors.SurfaceHigh).padding(8.dp))
            }
        }
    }

    Panel("🎬 Video (NO_VIDEO)") { VideoBox(blocked = UxRestrictionsState.Restriction.NO_VIDEO in ux.activeRestrictions) }

    Panel("⌨ Keyboard (NO_KEYBOARD)") {
        var text by remember { mutableStateOf("") }
        val blocked = UxRestrictionsState.Restriction.NO_KEYBOARD in ux.activeRestrictions
        OutlinedTextField(text, { text = it }, enabled = !blocked, modifier = Modifier.fillMaxWidth(),
            label = { Text(if (blocked) "Keyboard locked while driving - use voice 🎤" else "Search destination") })
    }

    Panel("📃 List (LIMIT_CONTENT, max ${if (restricted) ux.maxCumulativeItems else "∞"} items)") {
        val all = (1..30).map { "Contact $it" }
        val visible = if (restricted) all.take(minOf(ux.maxCumulativeItems, 6)) else all.take(12)
        Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            visible.forEach { Text("• $it", color = CarColors.TextPrimary) }
            Text(if (restricted) "…list capped while driving (AOSP default 21, templates: 6)" else "…and ${all.size - visible.size} more",
                color = CarColors.TextSecondary)
        }
    }

    Panel("✉ Message text (LIMIT_STRING_LENGTH = ${if (restricted) ux.maxStringLength else "∞"})") {
        val msg = "Hi! The meeting moved to 4 PM at the Koramangala office. Please bring the VHAL test report, the CAN " +
            "trace logs from yesterday's drive and the signed OTA package so we can review everything together before release."
        val shown = if (restricted) msg.take(minOf(ux.maxStringLength, 60)) + "…" else msg
        Text(shown, color = CarColors.TextPrimary, modifier = Modifier.animateContentSize())
    }
}

@Composable
private fun VideoBox(blocked: Boolean) {
    val t = rememberInfiniteTransition(label = "video")
    val x by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "x")
    Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.horizontalGradient(listOf(CarColors.Violet, CarColors.Pink, CarColors.Blue),
                startX = -size.width + x * size.width * 2, endX = x * size.width * 2))
            drawCircle(Color.White.copy(alpha = 0.7f), 30f, Offset(size.width * x, size.height / 2))
        }
        AnimatedVisibility(blocked, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔒", fontSize = 48.sp)
                    Text("Video is not available while driving", color = CarColors.TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Text(if (blocked) "Paused automatically. Passenger screens may still play video." else "Parked: video allowed.",
        color = CarColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
}
