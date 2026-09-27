package com.deepak.automotive.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deepak.automotive.ui.theme.CarColors

/** Big, glove-friendly button (AAOS minimum touch target ~76dp). */
@Composable
fun CarButton(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    accent: Color = CarColors.Cyan,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (selected) accent else CarColors.SurfaceHigh, label = "btn")
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = if (selected) Color.Black else CarColors.TextPrimary,
        ),
        modifier = modifier.defaultMinSize(minHeight = 64.dp, minWidth = 76.dp),
    ) { Text(text) }
}
