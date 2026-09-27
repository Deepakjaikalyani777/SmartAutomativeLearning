package com.deepak.automotive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors

private data class Layer(val name: String, val detail: String, val example: String, val color: Color)

private val layers = listOf(
    Layer("Apps", "System apps (Launcher, HVAC, Media Center, Settings) and 3rd-party apps (media, templates).",
        "Spotify, Google Maps, the OEM's climate app", CarColors.Cyan),
    Layer("Car API (android.car)", "Java/Kotlin managers: CarPropertyManager, CarUxRestrictionsManager, CarAudioManager, CarPowerManager, CarOccupantZoneManager...",
        "carPropertyManager.getFloatProperty(PERF_VEHICLE_SPEED, 0)", CarColors.Blue),
    Layer("CarService", "A privileged system service (com.android.car) that owns vehicle state, enforces car permissions and fans events out to apps.",
        "Checks CAR_SPEED permission, then subscribes to VHAL on your behalf", CarColors.Violet),
    Layer("Vehicle HAL (VHAL)", "AIDL HAL (android.hardware.automotive.vehicle.IVehicle) written by the OEM/Tier-1. Maps properties to vehicle signals.",
        "Harman/Bosch VHAL converts HVAC_TEMPERATURE_SET into a CAN signal", CarColors.Pink),
    Layer("Vehicle MCU / Gateway", "A small always-on microcontroller (often AUTOSAR) that talks to the vehicle buses and filters what the SoC may send.",
        "Secure gateway blocks the infotainment domain from powertrain CAN", CarColors.Amber),
    Layer("CAN / LIN / Ethernet → ECUs", "Dozens of ECUs: engine, ABS, body control, HVAC, ADAS, battery management.",
        "HVAC ECU receives 'set driver temp 21.5 °C' and drives the blend door", CarColors.Green),
)

@Composable
fun ArchitectureScreen() {
    var selected by remember { mutableIntStateOf(2) }
    val t = rememberInfiniteTransition(label = "packet")
    val n = layers.size
    // 0..n-1 = request travelling down, n..2n-1 = response travelling up
    val phase by t.animateFloat(0f, (2 * n).toFloat(), infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "p")
    val goingDown = phase < n
    val active = if (goingDown) phase.toInt() else (2 * n - 1 - phase.toInt())

    Panel(if (goingDown) "⬇ Request: app sets driver temperature" else "⬆ Event: ECU confirms new value") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            layers.forEachIndexed { i, layer ->
                val highlight by animateColorAsState(
                    if (i == active) layer.color.copy(alpha = 0.35f) else CarColors.SurfaceHigh, label = "layer",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(highlight)
                        .border(2.dp, if (i == selected) layer.color else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable { selected = i }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (i == active) (if (goingDown) "▼ " else "▲ ") else "   ", color = layer.color,
                        style = MaterialTheme.typography.titleMedium)
                    Text(layer.name, style = MaterialTheme.typography.titleMedium, color = CarColors.TextPrimary)
                }
            }
        }
    }
    Panel("Tap a layer to learn more") {
        AnimatedContent(selected, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "detail") { i ->
            val layer = layers[i]
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(layer.name, style = MaterialTheme.typography.titleLarge, color = layer.color)
                Text(layer.detail, style = MaterialTheme.typography.bodyLarge, color = CarColors.TextPrimary)
                Text("Example: ${layer.example}", style = MaterialTheme.typography.bodyMedium, color = CarColors.Green)
            }
        }
    }
    Panel("AAOS vs Android Auto (asked in almost every interview)") {
        Text(
            "Android Auto = your PHONE runs the app and projects the UI to the car screen (like casting).\n" +
                "Android Automotive OS = the CAR runs Android itself, no phone needed, and can read vehicle data (VHAL).\n" +
                "Same Car App Library & media APIs work on both.",
            style = MaterialTheme.typography.bodyLarge, color = CarColors.TextPrimary,
        )
    }
}
