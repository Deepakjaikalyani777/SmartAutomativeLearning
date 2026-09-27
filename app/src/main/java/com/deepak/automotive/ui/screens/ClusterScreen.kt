package com.deepak.automotive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.core.vehicle.Gear
import com.deepak.automotive.core.vehicle.Ignition
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.Gauge
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClusterScreen(vm: VehicleViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var throttle by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) { onDispose { vm.car.setThrottle(0f) } }

    val blink = rememberInfiniteTransition(label = "blink")
        .animateFloat(0.15f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "b")

    Panel("Instrument cluster") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()) {
            Text("◀", fontSize = 40.sp, color = CarColors.Green,
                modifier = Modifier.alpha(if (s.leftTurnSignal) blink.value else 0.1f))
            // Overspeed tell-tale
            Text("⚠ OVERSPEED", color = CarColors.Red, fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(if (s.speedKmh > 120f) blink.value else 0f))
            Text("P(!)", color = CarColors.Red, fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(if (s.parkingBrakeOn) 1f else 0.1f))
            Text("▶", fontSize = 40.sp, color = CarColors.Green,
                modifier = Modifier.alpha(if (s.rightTurnSignal) blink.value else 0.1f))
        }
        FlowRow(horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
            Gauge(s.speedKmh, 220f, "Speed", "km/h", Modifier.widthIn(max = 300.dp).size(260.dp), redZoneFrom = 180f, majorTicks = 11)
            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                // AnimatedContent: new gear slides in, old slides out.
                AnimatedContent(
                    s.gear,
                    transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                    label = "gear",
                ) { g ->
                    Text(g.label, fontSize = 96.sp, fontWeight = FontWeight.Black,
                        color = if (g == Gear.REVERSE) CarColors.Red else CarColors.Cyan)
                }
            }
            Gauge(s.rpm / 1000f, 8f, "RPM", "x1000", Modifier.widthIn(max = 300.dp).size(260.dp), redZoneFrom = 6.5f, majorTicks = 8)
        }
        val fuel by animateFloatAsState(s.fuelPercent / 100f, label = "fuel")
        Text("Fuel ${s.fuelPercent.toInt()} %   ·   Outside ${s.outsideTempC.toInt()} °C   ·   Ignition ${s.ignition}",
            color = CarColors.TextSecondary)
        LinearProgressIndicator(progress = { fuel }, color = if (fuel < 0.15f) CarColors.Red else CarColors.Green,
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)))
        AnimatedVisibility(s.lastError != null) {
            Text("⛔ ${s.lastError}", color = CarColors.Red, style = MaterialTheme.typography.bodyLarge)
        }
    }

    Panel("Drive the car (simulator)") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Ignition.entries.filter { it != Ignition.START }.forEach { ign ->
                CarButton(ign.name, selected = s.ignition == ign, accent = CarColors.Amber) { vm.car.setIgnition(ign) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Gear.entries.forEach { g -> CarButton(g.label, selected = s.gear == g) { vm.car.shift(g) } }
        }
        Text("Accelerator ${throttle.toInt()} %", color = CarColors.TextPrimary)
        Slider(value = throttle, onValueChange = { throttle = it; vm.car.setThrottle(it) }, valueRange = 0f..100f)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarColors.Red.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            vm.car.setBrake(true)
                            tryAwaitRelease()
                            vm.car.setBrake(false)
                        })
                    }
                    .padding(horizontal = 32.dp, vertical = 20.dp),
            ) { Text("HOLD TO BRAKE", color = CarColors.TextPrimary, fontWeight = FontWeight.Bold) }
            CarButton("◀ Indicator", selected = s.leftTurnSignal, accent = CarColors.Green) {
                vm.car.setTurnSignal(!s.leftTurnSignal, false)
            }
            CarButton("Indicator ▶", selected = s.rightTurnSignal, accent = CarColors.Green) {
                vm.car.setTurnSignal(false, !s.rightTurnSignal)
            }
        }
        Text("Try: ignition ON → D → accelerate. Now shift to P while moving - the transmission interlock refuses.",
            color = CarColors.TextSecondary)
    }
}
