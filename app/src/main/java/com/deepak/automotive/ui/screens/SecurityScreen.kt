package com.deepak.automotive.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.deepak.automotive.core.can.CanBus
import com.deepak.automotive.core.security.SecOc
import com.deepak.automotive.core.security.SecureVault
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val bootChain = listOf(
    "Boot ROM (immutable, holds OEM public key hash / fuses)",
    "Bootloader (signed, verified by ROM)",
    "vbmeta (Android Verified Boot descriptors)",
    "boot / kernel (hash checked by AVB)",
    "system / vendor (dm-verity, block-by-block at runtime)",
    "Apps (APK signatures, SELinux domains, permissions)",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SecurityScreen() {
    SecureBootDemo()
    SecOcDemo()
    KeystoreDemo()
    Panel("Car permission protection levels") {
        listOf(
            Triple("normal", "CAR_INFO, CAR_POWERTRAIN", "Granted at install - low risk"),
            Triple("dangerous", "CAR_SPEED, CAR_ENERGY", "User grants at runtime (dialog)"),
            Triple("signature", "CAR_CONTROL_AUDIO_VOLUME…", "Only apps signed with the platform/OEM key"),
            Triple("privileged", "CONTROL_CAR_CLIMATE, CONTROL_CAR_DOORS", "Pre-installed in /system/priv-app + allowlist XML"),
        ).forEach { (level, example, meaning) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(level, color = CarColors.Red, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.25f))
                Text(example, color = CarColors.Cyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.weight(0.4f))
                Text(meaning, color = CarColors.TextPrimary, modifier = Modifier.weight(0.35f))
            }
        }
    }
}

@Composable
private fun SecureBootDemo() {
    var tamperAt by remember { mutableIntStateOf(-1) }
    var verified by remember { mutableIntStateOf(-1) }
    var halted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Panel("Chain of trust (each stage verifies the NEXT before running it)") {
        bootChain.forEachIndexed { i, stage ->
            val ok = i <= verified
            val failed = halted && i == verified + 1
            val color by animateColorAsState(when { failed -> CarColors.Red; ok -> CarColors.Green; else -> CarColors.SurfaceHigh }, label = "boot")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.25f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(when { failed -> "✖ "; ok -> "✔ "; else -> "○ " }, color = color, fontSize = 22.sp)
                Text(stage + if (i == tamperAt) "  ← tampered" else "", color = CarColors.TextPrimary)
            }
        }
        AnimatedVisibility(halted, enter = fadeIn() + scaleIn()) {
            Text("Boot halted: signature mismatch. The car shows a warning / boots the other slot. Tampered code never runs.",
                color = CarColors.Red, fontWeight = FontWeight.Bold)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("Boot normally", accent = CarColors.Green, selected = true) {
                tamperAt = -1; scope.launch { runBoot(-1, { verified = it }, { halted = it }) }
            }
            CarButton("Tamper with kernel & boot", accent = CarColors.Red, selected = true) {
                tamperAt = 3; scope.launch { runBoot(3, { verified = it }, { halted = it }) }
            }
        }
    }
}

private suspend fun runBoot(tamperAt: Int, setVerified: (Int) -> Unit, setHalted: (Boolean) -> Unit) {
    setHalted(false); setVerified(-1)
    for (i in bootChain.indices) {
        delay(450)
        if (i == tamperAt) { setHalted(true); return }
        setVerified(i)
    }
}

@Composable
private fun SecOcDemo() {
    val key = remember { ByteArray(16) { it.toByte() } }
    val sender = remember { SecOc(key) }
    val receiver = remember { SecOc(key) }
    var lastValid by remember { mutableStateOf<SecOc.SecuredPdu?>(null) }
    var verdict by remember { mutableStateOf("Send a frame") }
    var verdictColor by remember { mutableStateOf(CarColors.TextSecondary) }
    val travel = remember { Animatable(0f) }
    var frameColor by remember { mutableStateOf(CarColors.Green) }
    val scope = rememberCoroutineScope()

    fun send(pdu: SecOc.SecuredPdu, color: Color) = scope.launch {
        frameColor = color
        travel.snapTo(0f); travel.animateTo(1f, tween(900))
        val v = receiver.verify(pdu)
        verdict = "${v.name}  (freshness=${pdu.freshness}, MAC=${pdu.mac.joinToString("") { "%02X".format(it) }})"
        verdictColor = if (v == SecOc.Verdict.ACCEPTED) CarColors.Green else CarColors.Red
    }

    Panel("SecOC: authenticated 'unlock doors' frame") {
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            drawRoundRect(CarColors.Blue, Offset(0f, 20f), Size(170f, 50f), CornerRadius(12f))
            drawRoundRect(CarColors.Violet, Offset(size.width - 170f, 20f), Size(170f, 50f), CornerRadius(12f))
            drawLine(CarColors.Amber, Offset(170f, 45f), Offset(size.width - 170f, 45f), 4f)
            val x = 170f + (size.width - 340f) * travel.value
            drawRoundRect(frameColor, Offset(x - 30f, 30f), Size(60f, 30f), CornerRadius(8f))
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Body ECU / phone key", color = CarColors.Blue); Text("Door ECU (verifier)", color = CarColors.Violet)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("Legit unlock", accent = CarColors.Green, selected = true) {
                val pdu = sender.protect(CanBus.Ids.DOOR_STATUS, byteArrayOf(0x01))
                lastValid = pdu; send(pdu, CarColors.Green)
            }
            CarButton("Attacker spoof (no key)", accent = CarColors.Red, selected = true) {
                send(SecOc.SecuredPdu(CanBus.Ids.DOOR_STATUS, byteArrayOf(0x01), 999, byteArrayOf(0, 0, 0, 0)), CarColors.Red)
            }
            CarButton("Replay old frame", accent = CarColors.Orange, selected = true, enabled = lastValid != null) {
                lastValid?.let { send(it, CarColors.Orange) }
            }
        }
        Text(verdict, color = verdictColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KeystoreDemo() {
    val vault = remember { runCatching { SecureVault() }.getOrNull() }
    var cipher by remember { mutableStateOf<String?>(null) }
    var plain by remember { mutableStateOf<String?>(null) }
    var hw by remember { mutableStateOf<Boolean?>(null) }

    Panel("Android Keystore: protect a Digital Car Key") {
        if (vault == null) { Text("Keystore unavailable on this device", color = CarColors.Red); return@Panel }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CarButton("Encrypt key token", accent = CarColors.Cyan, selected = true) {
                cipher = vault.encrypt("DIGITAL_KEY:VIN=MA3FB11S00R123456;exp=2026-12-31")
                plain = null; hw = vault.isHardwareBacked()
            }
            CarButton("Decrypt", enabled = cipher != null, accent = CarColors.Green, selected = true) {
                plain = cipher?.let(vault::decrypt)
            }
        }
        cipher?.let { Text("Stored (AES-256-GCM): ${it.take(64)}…", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = CarColors.Amber) }
        plain?.let { Text("Decrypted: $it", fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = CarColors.Green) }
        hw?.let { Text(if (it) "🔐 Key is inside secure hardware (TEE/StrongBox)" else "Key is software-backed on this device/emulator",
            color = CarColors.TextSecondary) }
    }
}
