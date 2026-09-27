package com.deepak.automotive.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.core.vehicle.HvacZone
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.CodeBlock
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HvacScreen(vm: VehicleViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    Panel("Dual-zone climate") {
        FlowRow(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            ZoneControl("Driver", HvacZone.DRIVER, s.driverTempC) { vm.car.setHvacTemperature(HvacZone.DRIVER, it) }
            Fan(s.fanSpeed, s.acOn)
            ZoneControl("Passenger", HvacZone.PASSENGER, s.passengerTempC) { vm.car.setHvacTemperature(HvacZone.PASSENGER, it) }
        }
        Text("Fan speed", color = CarColors.TextSecondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..6).forEach { f -> CarButton(if (f == 0) "OFF" else "$f", selected = s.fanSpeed == f, accent = CarColors.Blue) { vm.car.setFanSpeed(f) } }
            CarButton("A/C ${if (s.acOn) "ON" else "OFF"}", selected = s.acOn, accent = CarColors.Cyan) { vm.car.setAcOn(!s.acOn) }
        }
        AnimatedVisibility(s.lastError != null) {
            Text("⛔ ${s.lastError}", color = CarColors.Red)
        }
        Text("Outside ${s.outsideTempC.toInt()} °C. On a real AAOS build a 3rd-party app gets SecurityException here, " +
            "because CONTROL_CAR_CLIMATE is signature|privileged.", color = CarColors.TextSecondary)
    }
    Panel("How the HVAC app does it") {
        CodeBlock(
            """
            // Same property ID, different AREA per seat
            val cfg = carPropertyManager.getCarPropertyConfig(HVAC_TEMPERATURE_SET)
            val driverArea = cfg.areaIds.first { it and VehicleAreaSeat.SEAT_ROW_1_LEFT != 0 }
            carPropertyManager.setProperty(Float::class.java, HVAC_TEMPERATURE_SET, driverArea, 21.5f)
            // VHAL -> CAN frame to HVAC ECU -> blend door actuator moves
            """
        )
    }
}

@Composable
private fun ZoneControl(label: String, zone: HvacZone, temp: Float, onSet: (Float) -> Unit) {
    // Colour moves from icy blue (16 °C) to hot red (30 °C).
    val fraction = ((temp - 16f) / 14f).coerceIn(0f, 1f)
    val color by animateColorAsState(lerp(CarColors.Blue, CarColors.Red, fraction), label = "temp")
    val shown by animateFloatAsState(temp, label = "tempNum")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), CarColors.Surface)))
            .padding(20.dp)) {
        Text(label, color = CarColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
        Text("area 0x%X (%s)".format(zone.areaId, zone.label), color = CarColors.TextSecondary, fontSize = 12.sp)
        Text("%.1f°".format(shown), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = color)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("−", accent = CarColors.Blue) { onSet(temp - 0.5f) }
            CarButton("+", accent = CarColors.Red) { onSet(temp + 0.5f) }
        }
    }
}

/** Fan blades rotate proportionally to fan speed; air particles flow out when on. */
@Composable
private fun Fan(speed: Int, acOn: Boolean) {
    var angle by remember { mutableFloatStateOf(0f) }
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(speed) {
        var last = 0L
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1000f
                    angle = (angle + dt * speed * 180f) % 360f
                    time += dt
                }
                last = now
            }
        }
    }
    val tint = if (acOn) CarColors.Cyan else CarColors.Orange
    Canvas(Modifier.size(200.dp).padding(8.dp)) {
        val c = center
        val r = size.minDimension / 2
        // Airflow particles
        if (speed > 0) {
            for (i in 0 until 18) {
                val p = ((time * speed * 0.35f + i / 18f) % 1f)
                val a = i * 20.0
                val dist = r * (0.35f + 0.65f * p)
                val x = c.x + dist * kotlin.math.cos(Math.toRadians(a)).toFloat()
                val y = c.y + dist * sin(Math.toRadians(a)).toFloat()
                drawCircle(tint.copy(alpha = 1f - p), radius = 4f + 3f * (1 - p), center = Offset(x, y))
            }
        }
        rotate(angle, c) {
            for (b in 0 until 5) {
                rotate(b * 72f, c) {
                    drawOval(tint, topLeft = Offset(c.x - r * 0.12f, c.y - r * 0.62f),
                        size = androidx.compose.ui.geometry.Size(r * 0.24f, r * 0.55f))
                }
            }
        }
        drawCircle(CarColors.SurfaceHigh, r * 0.14f, c)
        drawCircle(Color.White.copy(alpha = 0.6f), r * 0.06f, c)
    }
}
