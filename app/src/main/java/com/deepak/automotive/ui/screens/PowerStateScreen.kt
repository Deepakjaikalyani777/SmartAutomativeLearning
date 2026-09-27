package com.deepak.automotive.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepak.automotive.core.power.PowerState
import com.deepak.automotive.core.power.PowerStateMachine
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.CodeBlock
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PowerStateScreen() {
    var state by remember { mutableStateOf(PowerState.ON) }
    val log = remember { mutableStateListOf("Car is ON") }
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(1f, 1.06f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s")

    Panel("CPMS state machine (driven by Vehicle MCU via VHAL AP_POWER_STATE_REQ)") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PowerState.entries.forEach { st ->
                val active = st == state
                val bg by animateColorAsState(if (active) CarColors.Amber.copy(alpha = 0.3f) else CarColors.SurfaceHigh, label = "st")
                Column(
                    Modifier
                        .fillMaxWidth()
                        .scale(if (active) pulse else 1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .border(2.dp, if (active) CarColors.Amber else Color.Transparent, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Text(st.name, style = MaterialTheme.typography.titleMedium, color = if (active) CarColors.Amber else CarColors.TextPrimary)
                    if (active) Text(st.description, color = CarColors.TextPrimary)
                }
            }
        }
    }
    Panel("Events you can fire now") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PowerStateMachine.allowedEvents(state).forEach { ev ->
                CarButton(ev.label, accent = CarColors.Amber) {
                    val next = PowerStateMachine.next(state, ev)
                    log.add(0, "${state.name} --${ev.name}--> ${next.name}")
                    state = next
                }
            }
        }
        log.take(6).forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = CarColors.Green) }
    }
    Panel("App side") {
        CodeBlock(
            """
            // Garage Mode: schedule heavy work for when the car is parked & off
            val job = JobInfo.Builder(OTA_DOWNLOAD, ComponentName(ctx, OtaJob::class.java))
                .setRequiresDeviceIdle(true)          // idle = Garage Mode in AAOS
                .setRequiredNetworkType(NETWORK_TYPE_UNMETERED)
                .build()
            jobScheduler.schedule(job)
            """
        )
    }
}
