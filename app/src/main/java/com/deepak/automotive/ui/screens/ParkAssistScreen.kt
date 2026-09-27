package com.deepak.automotive.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.core.vehicle.Gear
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors
import kotlinx.coroutines.delay

@Composable
fun ParkAssistScreen(vm: VehicleViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val cm = s.rearObstacleCm
    val zoneColor by animateColorAsState(
        when { cm < 40 -> CarColors.Red; cm < 100 -> CarColors.Amber; else -> CarColors.Green }, label = "zone",
    )
    var sound by remember { mutableStateOf(false) }

    // Beep interval shrinks as the obstacle gets closer; continuous tone under 30 cm.
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    LaunchedEffect(cm, sound, s.gear) {
        while (sound && s.gear == Gear.REVERSE && cm < 150) {
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, if (cm < 30) 400 else 80)
            delay(if (cm < 30) 400L else (cm * 5L).coerceAtLeast(120L))
        }
    }

    val pulse by rememberInfiniteTransition(label = "ultra")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "p")

    Panel("Top view (ultrasonic sensors: distance = speed of sound × echo time / 2)") {
        Canvas(Modifier.fillMaxWidth().height(320.dp)) {
            val carW = 140f
            val carH = 260f
            val carX = size.width / 2 - carW / 2
            val carY = 20f
            drawRoundRect(CarColors.Cyan, Offset(carX, carY), Size(carW, carH), CornerRadius(40f))
            drawRoundRect(CarColors.Background, Offset(carX + 20f, carY + 60f), Size(carW - 40f, 70f), CornerRadius(12f))
            val rearY = carY + carH
            // Obstacle (wall) position scales with distance
            val wallY = rearY + (cm / 300f) * (size.height - rearY - 10f) + 6f
            drawRect(CarColors.TextSecondary, Offset(size.width * 0.15f, wallY), Size(size.width * 0.7f, 10f))
            // Expanding sensor waves from the rear bumper, clipped at the obstacle
            for (i in 0 until 3) {
                val p = (pulse + i / 3f) % 1f
                val r = p * (wallY - rearY)
                drawArc(zoneColor.copy(alpha = 1f - p), 20f, 140f, false,
                    Offset(size.width / 2 - r, rearY - r), Size(r * 2, r * 2), style = Stroke(6f))
            }
            listOf(0.2f, 0.4f, 0.6f, 0.8f).forEach { f -> drawCircle(CarColors.Amber, 6f, Offset(carX + carW * f, rearY)) }
        }
        Text("Rear obstacle: $cm cm", style = MaterialTheme.typography.titleLarge, color = zoneColor, fontWeight = FontWeight.Bold)
        Slider(cm.toFloat(), { vm.car.setRearObstacle(it.toInt()) }, valueRange = 0f..300f)
        CarButton(if (s.gear == Gear.REVERSE) "In REVERSE: camera + sensors active" else "Shift to R", selected = s.gear == Gear.REVERSE,
            accent = CarColors.Red) { vm.car.shift(Gear.REVERSE) }
        androidx.compose.foundation.layout.Row {
            Switch(sound, { sound = it })
            Text("  Beep sound", color = CarColors.TextPrimary)
        }
    }
    Panel("Why EVS exists") {
        Text("On AAOS the Extended View System is a native service started early by init. It reads GEAR_SELECTION " +
            "from VHAL and shows the rear camera through the EVS HAL, bypassing SurfaceFlinger apps and the Java framework, " +
            "so it can appear within the ~2 s required by FMVSS 111 even during boot.", color = CarColors.TextPrimary)
    }
}
