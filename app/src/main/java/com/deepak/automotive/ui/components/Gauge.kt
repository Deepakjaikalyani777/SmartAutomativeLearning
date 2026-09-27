package com.deepak.automotive.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.deepak.automotive.ui.theme.CarColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Instrument-cluster style gauge. The needle uses a low-stiffness SPRING so it sweeps like a
 * physical stepper-motor needle instead of jumping between CAN updates (which arrive every 10-100 ms).
 */
@Composable
fun Gauge(
    value: Float,
    max: Float,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    redZoneFrom: Float = Float.MAX_VALUE,
    majorTicks: Int = 10,
) {
    val animated by animateFloatAsState(
        targetValue = value.coerceIn(0f, max),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "needle",
    )
    val startAngle = 135f
    val sweep = 270f
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.06f
            val inset = stroke
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(CarColors.SurfaceHigh, startAngle, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (redZoneFrom < max) {
                val redStart = startAngle + sweep * redZoneFrom / max
                drawArc(CarColors.Red.copy(alpha = 0.6f), redStart, startAngle + sweep - redStart, false, topLeft, arcSize, style = Stroke(stroke))
            }
            drawArc(
                Brush.sweepGradient(listOf(CarColors.Cyan, CarColors.Green, CarColors.Amber, CarColors.Red, CarColors.Cyan)),
                startAngle, sweep * animated / max, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val c = center
            val r = size.minDimension / 2 - inset
            for (i in 0..majorTicks) {
                val a = Math.toRadians((startAngle + sweep * i / majorTicks).toDouble())
                val outer = Offset(c.x + (r - stroke) * cos(a).toFloat(), c.y + (r - stroke) * sin(a).toFloat())
                val inner = Offset(c.x + (r - stroke * 2.2f) * cos(a).toFloat(), c.y + (r - stroke * 2.2f) * sin(a).toFloat())
                drawLine(CarColors.TextSecondary, inner, outer, strokeWidth = 4f)
            }
            val needleAngle = Math.toRadians((startAngle + sweep * animated / max).toDouble())
            val tip = Offset(c.x + (r * 0.78f) * cos(needleAngle).toFloat(), c.y + (r * 0.78f) * sin(needleAngle).toFloat())
            drawLine(Color.White, c, tip, strokeWidth = stroke * 0.35f, cap = StrokeCap.Round)
            drawCircle(CarColors.Surface, radius = stroke * 1.4f, center = c)
            drawCircle(CarColors.Cyan, radius = stroke * 0.7f, center = c)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(animated.toInt().toString(), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = CarColors.TextPrimary)
            Text("$label ($unit)", style = MaterialTheme.typography.bodyMedium, color = CarColors.TextSecondary)
        }
    }
}
