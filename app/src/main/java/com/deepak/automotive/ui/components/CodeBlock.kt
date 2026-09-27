package com.deepak.automotive.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepak.automotive.ui.theme.CarColors

@Composable
fun CodeBlock(code: String) {
    Text(
        code.trimIndent(),
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = CarColors.Green,
        softWrap = false,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF02040A))
            .horizontalScroll(rememberScrollState())
            .padding(16.dp),
    )
}
