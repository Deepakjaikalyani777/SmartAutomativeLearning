package com.deepak.automotive.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors

@Composable
fun EvChargingScreen(vm: VehicleViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val level by animateFloatAsState(s.evBatteryPercent / 100f, label = "soc")
    val glow by rememberInfiniteTransition(label = "charge")
        .animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "g")

    Panel("High-voltage battery") {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val w = size.width - 30f
            drawRoundRect(CarColors.TextSecondary, Offset.Zero, Size(w, size.height), CornerRadius(24f), style = Stroke(6f))
            drawRoundRect(CarColors.TextSecondary, Offset(w, size.height * 0.3f), Size(24f, size.height * 0.4f), CornerRadius(6f))
            val fillColor = lerp(CarColors.Red, CarColors.Green, level)
            drawRoundRect(fillColor.copy(alpha = if (s.isCharging) glow else 1f), Offset(10f, 10f),
                Size((w - 20f) * level, size.height - 20f), CornerRadius(16f))
            if (s.isCharging) {
                val cx = w / 2
                val bolt = Path().apply {
                    moveTo(cx + 10f, 20f); lineTo(cx - 30f, size.height / 2 + 10f); lineTo(cx, size.height / 2 + 10f)
                    lineTo(cx - 10f, size.height - 20f); lineTo(cx + 30f, size.height / 2 - 10f); lineTo(cx, size.height / 2 - 10f); close()
                }
                drawPath(bolt, androidx.compose.ui.graphics.Color.White.copy(alpha = glow))
            }
        }
        Text("SoC %.1f %%   ·   Range ≈ %d km   ·   %s".format(s.evBatteryPercent, s.evRangeKm,
            if (s.isCharging) (if (s.evBatteryPercent < 80) "Fast charging (CC)" else "Tapering (CV)") else "Not charging"),
            style = MaterialTheme.typography.titleMedium, color = CarColors.TextPrimary, fontWeight = FontWeight.Bold)
        CarButton(if (s.isCharging) "Unplug" else "Plug in DC charger", selected = s.isCharging, accent = CarColors.Green) {
            vm.car.setCharging(!s.isCharging)
        }
        s.lastError?.let { Text("⛔ $it", color = CarColors.Red) }
    }

    Panel("DC fast-charge curve (power vs state of charge)") {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val curve = Path()
            fun power(soc: Float) = if (soc < 0.8f) 1f - soc * 0.15f else (1f - soc) * 4.4f
            for (i in 0..100) {
                val soc = i / 100f
                val x = soc * size.width
                val y = size.height - power(soc) * size.height * 0.9f
                if (i == 0) curve.moveTo(x, y) else curve.lineTo(x, y)
            }
            drawPath(curve, CarColors.Cyan, style = Stroke(5f))
            val x = level * size.width
            drawCircle(CarColors.Amber, 12f, Offset(x, size.height - power(level) * size.height * 0.9f))
            drawLine(CarColors.TextSecondary, Offset(size.width * 0.8f, 0f), Offset(size.width * 0.8f, size.height), 2f)
        }
        Text("Past 80 % the charger lowers current to protect the cells (constant-voltage phase). " +
            "That is why road-trip planners (Google Maps EV routing) plan 10 → 80 % stops.", color = CarColors.TextSecondary)
    }

    Panel("Car App Library: charging category") {
        Text("This APK also contains ChargingStationCarAppService (category CHARGING). Run it on the Android Auto " +
            "Desktop Head Unit or an AAOS emulator to see the ListTemplate → PaneTemplate flow and the Navigate hand-off.",
            color = CarColors.TextPrimary)
    }
}
