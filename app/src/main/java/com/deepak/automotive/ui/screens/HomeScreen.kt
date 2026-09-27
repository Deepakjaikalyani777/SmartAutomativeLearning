package com.deepak.automotive.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepak.automotive.core.vehicle.DataSourceType
import com.deepak.automotive.ui.VehicleViewModel
import com.deepak.automotive.ui.components.CarButton
import com.deepak.automotive.ui.navigation.Lesson
import com.deepak.automotive.ui.navigation.Lessons
import com.deepak.automotive.ui.theme.CarColors
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(onOpen: (String) -> Unit, vm: VehicleViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(280.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(CarColors.Background)
            .statusBarsPadding(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Automotive Academy", style = MaterialTheme.typography.headlineMedium, color = CarColors.TextPrimary)
                Text(
                    "Learn Android Automotive OS by playing with it. Every tile is a live, animated demo.",
                    style = MaterialTheme.typography.bodyLarge, color = CarColors.TextSecondary,
                )
                DrivingCarBanner()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CarButton("Simulator", selected = state.source == DataSourceType.SIMULATOR) {
                        vm.useSource(DataSourceType.SIMULATOR)
                    }
                    CarButton(
                        if (vm.isAutomotive) "Real Car API" else "Car API (AAOS only)",
                        selected = state.source == DataSourceType.CAR_API,
                        enabled = vm.isAutomotive,
                    ) { vm.useSource(DataSourceType.CAR_API) }
                }
            }
        }
        itemsIndexed(Lessons.all, key = { _, l -> l.route }) { index, lesson ->
            LessonTile(index, lesson) { onOpen(lesson.route) }
        }
    }
}

/** Staggered entrance: each tile slides up and fades in slightly after the previous one. */
@Composable
private fun LessonTile(index: Int, lesson: Lesson, onClick: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 45L)
        progress.animateTo(1f, tween(450))
    }
    Row(
        Modifier
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * 60f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(CarColors.Surface)
            .clickable(onClick = onClick)
            .padding(20.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(lesson.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(lesson.icon, contentDescription = null, tint = lesson.color, modifier = Modifier.size(32.dp))
        }
        Column {
            Text("${index + 1}. ${lesson.title}", style = MaterialTheme.typography.titleMedium, color = CarColors.TextPrimary)
            Text(lesson.subtitle, style = MaterialTheme.typography.bodyMedium, color = CarColors.TextSecondary)
        }
    }
}

/** A tiny car driving along a road with moving lane markings (infinite transition). */
@Composable
private fun DrivingCarBanner() {
    val t = rememberInfiniteTransition(label = "road")
    val lane by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "lane")
    val bob by t.animateFloat(-2f, 2f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "bob")
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CarColors.Surface)
    ) {
        val roadY = size.height * 0.75f
        drawRect(CarColors.SurfaceHigh, Offset(0f, roadY - 6f), Size(size.width, 36f))
        val dash = 80f
        var x = -dash * 2 * lane
        while (x < size.width) {
            drawRect(CarColors.Amber, Offset(x, roadY + 10f), Size(dash, 4f))
            x += dash * 2
        }
        val carX = size.width * 0.35f
        val carY = roadY - 34f + bob
        drawRoundRect(CarColors.Cyan, Offset(carX, carY), Size(150f, 34f), CornerRadius(14f))
        drawRoundRect(CarColors.Cyan, Offset(carX + 30f, carY - 24f), Size(80f, 30f), CornerRadius(14f))
        drawCircle(CarColors.Background, 14f, Offset(carX + 32f, carY + 34f))
        drawCircle(CarColors.Background, 14f, Offset(carX + 118f, carY + 34f))
        drawCircle(CarColors.Amber, 6f, Offset(carX + 146f, carY + 12f))
    }
}
