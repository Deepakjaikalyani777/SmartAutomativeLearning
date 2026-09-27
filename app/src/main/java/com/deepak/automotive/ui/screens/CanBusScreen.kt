package com.deepak.automotive.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.core.can.ArbitrationResult
import com.deepak.automotive.core.can.CanBus
import com.deepak.automotive.core.can.CanFrame
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Ecu(val name: String, val id: Int, val color: Color)

private val ecus = listOf(
    Ecu("ABS / Brake", CanBus.Ids.BRAKE, CarColors.Red),
    Ecu("Engine", CanBus.Ids.ENGINE_RPM, CarColors.Amber),
    Ecu("Speed", CanBus.Ids.VEHICLE_SPEED, CarColors.Green),
    Ecu("Body (doors)", CanBus.Ids.DOOR_STATUS, CarColors.Blue),
    Ecu("IVI (Harman)", CanBus.Ids.IVI_MEDIA, CarColors.Violet),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CanBusScreen(vm: VehicleViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val trace = remember { mutableStateListOf<String>() }
    var sender by remember { mutableIntStateOf(2) }
    val travel = remember { Animatable(0f) }

    // A live "candump": every 400 ms the speed ECU broadcasts vehicle speed.
    LaunchedEffect(Unit) {
        while (true) {
            sender = 2
            val frame = CanFrame(CanBus.Ids.VEHICLE_SPEED, CanBus.encodeSpeed(s.speedKmh), "Speed")
            trace.add(0, "can0  %03X  [%d]  %s   → %.2f km/h".format(frame.id, frame.dlc,
                frame.data.joinToString(" ") { "%02X".format(it) }, CanBus.decodeSpeed(frame.data)))
            if (trace.size > 8) trace.removeAt(trace.lastIndex)
            travel.snapTo(0f)
            travel.animateTo(1f, tween(380, easing = LinearEasing))
            delay(20)
        }
    }

    Panel("The bus (a frame is broadcast to EVERY node)") {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val busY1 = size.height * 0.45f
            val busY2 = busY1 + 14f
            drawLine(CarColors.Amber, Offset(0f, busY1), Offset(size.width, busY1), 5f)   // CAN_H
            drawLine(CarColors.Blue, Offset(0f, busY2), Offset(size.width, busY2), 5f)    // CAN_L
            drawRect(CarColors.TextSecondary, Offset(0f, busY1 - 8f), Size(12f, 30f))     // 120 Ω
            drawRect(CarColors.TextSecondary, Offset(size.width - 12f, busY1 - 8f), Size(12f, 30f))
            val step = size.width / (ecus.size + 1)
            ecus.forEachIndexed { i, e ->
                val x = step * (i + 1)
                val top = i % 2 == 0
                val boxY = if (top) 0f else size.height - 44f
                drawLine(e.color, Offset(x, if (top) 44f else boxY), Offset(x, if (top) busY1 else busY2), 3f)
                drawRoundRect(e.color.copy(alpha = if (i == sender) 1f else 0.4f), Offset(x - 50f, boxY), Size(100f, 44f), CornerRadius(10f))
            }
            // Frame travelling outwards from the sender in both directions.
            val sx = step * (sender + 1)
            val spread = travel.value * size.width
            listOf(sx - spread, sx + spread).forEach { fx ->
                if (fx in 0f..size.width) drawCircle(ecus[sender].color, 12f, Offset(fx, busY1 + 7f))
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            ecus.forEach { Text("${it.name}\n0x%03X".format(it.id), color = it.color, fontSize = 12.sp) }
        }
    }

    Panel("candump (live speed frames, DBC: 16 bit LE, factor 0.01)") {
        trace.forEach { Text(it, fontFamily = FontFamily.Monospace, color = CarColors.Green, fontSize = 14.sp) }
    }

    ArbitrationDemo()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArbitrationDemo() {
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<ArbitrationResult?>(null) }
    var revealedBits by remember { mutableIntStateOf(0) }
    val choices = remember { mutableStateListOf(0, 4) }

    Panel("Arbitration: who wins when ECUs talk at the same time?") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ecus.forEachIndexed { i, e ->
                CarButton(e.name, selected = i in choices, accent = e.color) {
                    if (i in choices) choices.remove(i) else choices.add(i)
                }
            }
            CarButton("▶ Transmit together", accent = CarColors.Green, selected = true, enabled = choices.size >= 2) {
                scope.launch {
                    val frames = choices.map { CanFrame(ecus[it].id, byteArrayOf(1), ecus[it].name) }
                    result = CanBus.arbitrate(frames)
                    revealedBits = 0
                    repeat(11) { delay(250); revealedBits++ }
                }
            }
        }
        val r = result
        if (r != null) {
            (listOf(r.winner) + r.losers).forEach { f ->
                val bits = f.idBits()
                // A loser drops out at the first bit where it sends recessive 1 but reads dominant 0.
                val winnerBits = r.winner.idBits()
                val lostAt = if (f == r.winner) 99 else bits.indices.first { bits[it] != winnerBits[it] }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(f.sender.padEnd(14), fontFamily = FontFamily.Monospace, color = CarColors.TextPrimary, fontSize = 14.sp)
                    bits.forEachIndexed { i, b ->
                        val shown = i < revealedBits
                        val out = i > lostAt && shown
                        Text(if (shown) b.toString() else "·",
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                            color = when { out -> CarColors.TextSecondary.copy(alpha = 0.3f); i == lostAt && shown -> CarColors.Red; else -> CarColors.Green },
                            modifier = Modifier.padding(2.dp).clip(RoundedCornerShape(4.dp)).background(CarColors.SurfaceHigh).padding(horizontal = 4.dp))
                    }
                }
            }
            if (revealedBits >= 11) {
                Text("🏆 ${r.winner.sender} (0x%03X) wins - the lowest ID sends a dominant 0 first. Losers retry automatically after the frame."
                    .format(r.winner.id), color = CarColors.Green, fontWeight = FontWeight.Bold)
            }
        }
    }
}
