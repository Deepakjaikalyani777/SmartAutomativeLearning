package com.deepak.automotive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.core.ota.AbOtaUpdater
import com.deepak.automotive.core.ota.OtaPhase
import com.deepak.automotive.core.ota.OtaState
import com.deepak.automotive.core.ota.Slot
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OtaScreen(vm: VehicleViewModel = viewModel()) {
    val vehicle by vm.state.collectAsStateWithLifecycle()
    var ota by remember { mutableStateOf(OtaState()) }
    var failBoot by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var phaseProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val nextVersion = "v1.${(ota.versions.values.maxOf { it.removePrefix("v1.").toIntOrNull() ?: 0 }) + 1}"

    Panel("Partitions") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Slot.entries.forEach { slot ->
                val active = ota.active == slot
                val writing = !active && ota.phase == OtaPhase.INSTALLING
                val border by animateColorAsState(
                    when { active -> CarColors.Green; writing -> CarColors.Amber; else -> CarColors.SurfaceHigh }, label = "slot")
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(CarColors.SurfaceHigh)
                        .border(3.dp, border, RoundedCornerShape(18.dp)).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Slot ${slot.name}", style = MaterialTheme.typography.titleLarge, color = CarColors.TextPrimary)
                    Text(ota.versions[slot] ?: "-", color = CarColors.Cyan, style = MaterialTheme.typography.headlineMedium)
                    Text(if (active) "ACTIVE (running)" else if (writing) "WRITING…" else "inactive", color = border)
                }
            }
        }
        AnimatedContent(ota.phase, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "phase") { ph ->
            Text(ph.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = when (ph) { OtaPhase.SUCCESS -> CarColors.Green; OtaPhase.ROLLED_BACK -> CarColors.Red; else -> CarColors.Amber })
        }
        val p by animateFloatAsState(phaseProgress, label = "prog")
        LinearProgressIndicator(progress = { p }, color = CarColors.Cyan,
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(failBoot, { failBoot = it })
            Text("  Simulate a broken image (boot fails)", color = CarColors.TextPrimary)
        }
        CarButton("Start OTA to $nextVersion", enabled = !running, accent = CarColors.Cyan, selected = true) {
            running = true
            scope.launch {
                ota = AbOtaUpdater.advance(ota, nextVersion, !failBoot)
                while (ota.phase != OtaPhase.SUCCESS && ota.phase != OtaPhase.ROLLED_BACK) {
                    if (ota.phase == OtaPhase.READY_TO_REBOOT) {
                        // Safety gate: never reboot the head unit while driving.
                        while (vehicle.isMoving) delay(300)
                    }
                    for (i in 1..10) { phaseProgress = i / 10f; delay(120) }
                    ota = AbOtaUpdater.advance(ota, nextVersion, !failBoot)
                    phaseProgress = 0f
                }
                phaseProgress = 1f
                running = false
            }
        }
        if (vehicle.isMoving && ota.phase == OtaPhase.READY_TO_REBOOT) {
            Text("⏳ Vehicle is moving - reboot postponed until parked.", color = CarColors.Amber)
        }
    }
    Panel("Why A/B?") {
        Text("• Update installs while you drive (inactive slot)\n• Reboot takes seconds\n• Failed boot → bootloader " +
            "tries the old slot (rollback)\n• dm-verity + AVB verify every block of the new slot", color = Color.White)
    }
}
