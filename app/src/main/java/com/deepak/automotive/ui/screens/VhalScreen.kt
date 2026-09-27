package com.deepak.automotive.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.core.vehicle.ChangeMode
import com.deepak.automotive.core.vehicle.VehicleState
import com.deepak.automotive.core.vehicle.VhalCatalog
import com.deepak.automotive.core.vehicle.VhalPropertyId
import com.deepak.automotive.core.vehicle.VhalPropertyInfo
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.CodeBlock
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VhalScreen(vm: VehicleViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf(VhalCatalog.properties.first()) }

    Panel("Live properties (rows flash when a change event arrives)") {
        Text("Tip: open Cluster, drive, and come back - CONTINUOUS rows flash constantly, ON_CHANGE rows only on change.",
            color = CarColors.TextSecondary)
        VhalCatalog.properties.forEach { p ->
            PropertyRow(p, valueOf(p, s), selected == p) { selected = p }
        }
    }

    Panel("Property ID decoder") {
        val id = VhalPropertyId(selected.id)
        Text(selected.name, style = MaterialTheme.typography.titleLarge, color = CarColors.Violet)
        AnimatedContent(id, transitionSpec = { (scaleIn() + fadeIn()) togetherWith fadeOut() }, label = "id") { pid ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val hex = "%08X".format(pid.raw)
                Row {
                    Text("0x", fontFamily = FontFamily.Monospace, fontSize = 36.sp, color = CarColors.TextSecondary)
                    Segment(hex.substring(0, 1), CarColors.Cyan)
                    Segment(hex.substring(1, 2), CarColors.Green)
                    Segment(hex.substring(2, 4), CarColors.Amber)
                    Segment(hex.substring(4, 8), CarColors.Pink)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Legend("Group", pid.groupName, CarColors.Cyan)
                    Legend("Area", pid.areaName, CarColors.Green)
                    Legend("Type", pid.typeName, CarColors.Amber)
                    Legend("ID", "0x%04X".format(pid.uniqueId), CarColors.Pink)
                }
                Text("Change mode: ${selected.changeMode} · Access: ${selected.access} · Permission: ${selected.permission} · Unit: ${selected.unit}",
                    color = CarColors.TextPrimary)
            }
        }
    }

    Panel("The code (CarApiVehicleDataSource.kt)") {
        CodeBlock(
            """
            val car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { car, ready ->
                if (!ready) return@createCar          // CarService died: drop managers
                val pm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
                pm.registerCallback(object : CarPropertyEventCallback {
                    override fun onChangeEvent(v: CarPropertyValue<*>) {
                        val kmh = (v.value as Float) * 3.6f   // VHAL speed is m/s
                    }
                    override fun onErrorEvent(propId: Int, areaId: Int) {}
                }, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_UI)
            }
            """
        )
    }
}

@Composable
private fun PropertyRow(p: VhalPropertyInfo, value: String, isSelected: Boolean, onClick: () -> Unit) {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(value) {
        flash.snapTo(1f)
        flash.animateTo(0f, tween(700))
    }
    val bg = lerp(if (isSelected) CarColors.SurfaceHigh else CarColors.Background, CarColors.Violet.copy(alpha = 0.5f), flash.value)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, color = CarColors.TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(VhalPropertyId(p.id).hex(), color = CarColors.TextSecondary, fontFamily = FontFamily.Monospace)
        }
        Badge(p.changeMode)
        Text(value, color = CarColors.Cyan, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.8f))
    }
}

@Composable
private fun Badge(mode: ChangeMode) {
    val c = when (mode) {
        ChangeMode.STATIC -> CarColors.TextSecondary
        ChangeMode.ON_CHANGE -> CarColors.Green
        ChangeMode.CONTINUOUS -> CarColors.Amber
    }
    Text(mode.name, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c).padding(horizontal = 8.dp, vertical = 4.dp))
}

@Composable
private fun Segment(text: String, color: Color) {
    Text(text, fontFamily = FontFamily.Monospace, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier.padding(horizontal = 2.dp).clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)))
}

@Composable
private fun Legend(label: String, value: String, color: Color) {
    Column {
        Text(label, color = color, fontWeight = FontWeight.Bold)
        Text(value, color = CarColors.TextPrimary)
    }
}

private fun valueOf(p: VhalPropertyInfo, s: VehicleState): String = when (p.name) {
    "PERF_VEHICLE_SPEED" -> "%.2f".format(s.speedKmh / 3.6f)
    "ENGINE_RPM" -> s.rpm.toInt().toString()
    "GEAR_SELECTION" -> s.gear.name
    "PARKING_BRAKE_ON" -> s.parkingBrakeOn.toString()
    "IGNITION_STATE" -> s.ignition.name
    "FUEL_LEVEL" -> (s.fuelPercent * 500).toInt().toString()
    "EV_BATTERY_LEVEL" -> (s.evBatteryPercent * 640).toInt().toString()
    "ENV_OUTSIDE_TEMPERATURE" -> "%.1f".format(s.outsideTempC)
    "NIGHT_MODE" -> s.nightMode.toString()
    "HVAC_TEMPERATURE_SET" -> "L %.1f / R %.1f".format(s.driverTempC, s.passengerTempC)
    "HVAC_FAN_SPEED" -> s.fanSpeed.toString()
    "INFO_MAKE" -> "Academy Motors"
    else -> "locked"
}
