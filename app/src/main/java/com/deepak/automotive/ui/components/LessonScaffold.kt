package com.deepak.automotive.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepak.automotive.ui.navigation.Lesson
import com.deepak.automotive.ui.theme.CarColors

/**
 * Every lesson screen = a live, animated demo + a "Head First" notes panel
 * (What is it? / Real life / Brain Power / Interview tip).
 */
@Composable
fun LessonScaffold(lesson: Lesson, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CarColors.Background)
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.size(64.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CarColors.TextPrimary)
            }
            Icon(lesson.icon, contentDescription = null, tint = lesson.color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(lesson.title, style = MaterialTheme.typography.headlineMedium, color = CarColors.TextPrimary)
                Text(lesson.subtitle, style = MaterialTheme.typography.bodyMedium, color = CarColors.TextSecondary)
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeadFirstNotes(lesson)
            content()
            Spacer(Modifier.size(32.dp))
        }
    }
}

@Composable
private fun HeadFirstNotes(lesson: Lesson) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CarColors.Surface)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📘  Head First notes", style = MaterialTheme.typography.titleMedium, color = lesson.color,
                modifier = Modifier.weight(1f))
            Icon(Icons.Default.ExpandMore, contentDescription = if (expanded) "Collapse" else "Expand",
                tint = CarColors.TextSecondary, modifier = Modifier.rotate(arrow))
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Note("🧠 What is it?", lesson.notes.what, CarColors.TextPrimary)
                Note("🚗 Real life", lesson.notes.realLife, CarColors.Green)
                Note("💪 Brain Power", lesson.notes.brainPower, CarColors.Amber)
                Note("🎯 Interview tip", lesson.notes.interviewTip, CarColors.Cyan)
            }
        }
    }
}

@Composable
private fun Note(title: String, body: String, color: Color) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = CarColors.TextPrimary)
    }
}

/** A titled rounded panel used by demos. */
@Composable
fun Panel(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CarColors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = CarColors.TextPrimary)
        content()
    }
}
