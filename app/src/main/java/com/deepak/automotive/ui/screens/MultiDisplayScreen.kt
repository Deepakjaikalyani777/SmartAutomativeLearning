package com.deepak.automotive.ui.screens

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.components.CodeBlock
import com.deepak.automotive.ui.components.Panel
import com.deepak.automotive.ui.theme.CarColors

private enum class Zone(val label: String, val seat: Offset, val display: String, val user: String, val video: Boolean, val color: Color) {
    DRIVER("Driver", Offset(0.33f, 0.40f), "Cluster + centre display", "User 10 (Deepak)", false, CarColors.Cyan),
    PASSENGER("Front passenger", Offset(0.67f, 0.40f), "Passenger display", "User 11 (Guest)", true, CarColors.Pink),
    REAR_LEFT("Rear left", Offset(0.33f, 0.72f), "Rear-seat screen L", "User 12 (Kid)", true, CarColors.Green),
    REAR_RIGHT("Rear right", Offset(0.67f, 0.72f), "Rear-seat screen R", "User 13", true, CarColors.Amber),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiDisplayScreen() {
    var selected by remember { mutableStateOf(Zone.DRIVER) }
    // The "focus ring" springs from seat to seat.
    val ring by animateOffsetAsState(selected.seat, spring(dampingRatio = 0.55f), label = "ring")

    Panel("Occupant zones (CarOccupantZoneManager)") {
        Canvas(Modifier.fillMaxWidth().height(340.dp)) {
            val w = size.width
            val h = size.height
            val carW = minOf(w * 0.6f, 320f)
            val left = (w - carW) / 2
            drawRoundRect(CarColors.SurfaceHigh, Offset(left, 0f), Size(carW, h), CornerRadius(80f))
            // Displays: cluster, centre, passenger across the dashboard
            drawRoundRect(CarColors.Cyan, Offset(left + carW * 0.15f, h * 0.16f), Size(carW * 0.25f, 14f), CornerRadius(6f))
            drawRoundRect(CarColors.Cyan, Offset(left + carW * 0.42f, h * 0.16f), Size(carW * 0.16f, 14f), CornerRadius(6f))
            drawRoundRect(CarColors.Pink, Offset(left + carW * 0.60f, h * 0.16f), Size(carW * 0.25f, 14f), CornerRadius(6f))
            // Rear-seat screens on the back of the front seats
            drawRoundRect(CarColors.Green, Offset(left + carW * 0.22f, h * 0.56f), Size(carW * 0.22f, 10f), CornerRadius(4f))
            drawRoundRect(CarColors.Amber, Offset(left + carW * 0.56f, h * 0.56f), Size(carW * 0.22f, 10f), CornerRadius(4f))
            Zone.entries.forEach { z ->
                val c = Offset(left + carW * (z.seat.x - 0.5f) * 1.6f + carW / 2, h * z.seat.y)
                drawRoundRect(z.color.copy(alpha = 0.5f), Offset(c.x - 34f, c.y - 34f), Size(68f, 68f), CornerRadius(18f))
            }
            val rc = Offset(left + carW * (ring.x - 0.5f) * 1.6f + carW / 2, h * ring.y)
            drawCircle(Color.White, 52f, rc, style = Stroke(6f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Zone.entries.forEach { z -> CarButton(z.label, selected = z == selected, accent = z.color) { selected = z } }
        }
        Text("${selected.label} → ${selected.display}\nAndroid user: ${selected.user}\nAudio zone: " +
            (if (selected == Zone.DRIVER) "primary (0)" else "secondary zone") +
            "\nVideo while driving: " + if (selected.video) "✅ allowed (not visible to driver)" else "❌ blocked (UX restrictions)",
            style = MaterialTheme.typography.bodyLarge, color = CarColors.TextPrimary)
    }
    Panel("API") {
        CodeBlock(
            """
            val ozm = car.getCarManager(Car.CAR_OCCUPANT_ZONE_SERVICE) as CarOccupantZoneManager
            ozm.allOccupantZones.forEach { zone ->
                val displays = ozm.getAllDisplaysForOccupant(zone)
                val userId   = ozm.getUserForOccupant(zone)      // multi-user, multi-display (MUMD)
            }
            """
        )
    }
}
