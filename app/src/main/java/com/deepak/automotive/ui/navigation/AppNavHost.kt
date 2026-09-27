package com.deepak.automotive.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.deepak.automotive.ui.components.LessonScaffold
import com.deepak.automotive.ui.screens.ArchitectureScreen
import com.deepak.automotive.ui.screens.AudioFocusScreen
import com.deepak.automotive.ui.screens.CanBusScreen
import com.deepak.automotive.ui.screens.CarAppLibraryScreen
import com.deepak.automotive.ui.screens.ClusterScreen
import com.deepak.automotive.ui.screens.DriverDistractionScreen
import com.deepak.automotive.ui.screens.EvChargingScreen
import com.deepak.automotive.ui.screens.HomeScreen
import com.deepak.automotive.ui.screens.HvacScreen
import com.deepak.automotive.ui.screens.MediaScreen
import com.deepak.automotive.ui.screens.MultiDisplayScreen
import com.deepak.automotive.ui.screens.OtaScreen
import com.deepak.automotive.ui.screens.ParkAssistScreen
import com.deepak.automotive.ui.screens.PowerStateScreen
import com.deepak.automotive.ui.screens.SecurityScreen
import com.deepak.automotive.ui.screens.VhalScreen

private val screens: Map<String, @Composable () -> Unit> = mapOf(
    "architecture" to { ArchitectureScreen() },
    "cluster" to { ClusterScreen() },
    "vhal" to { VhalScreen() },
    "hvac" to { HvacScreen() },
    "uxr" to { DriverDistractionScreen() },
    "media" to { MediaScreen() },
    "audio" to { AudioFocusScreen() },
    "can" to { CanBusScreen() },
    "park" to { ParkAssistScreen() },
    "ev" to { EvChargingScreen() },
    "power" to { PowerStateScreen() },
    "ota" to { OtaScreen() },
    "security" to { SecurityScreen() },
    "zones" to { MultiDisplayScreen() },
    "carapp" to { CarAppLibraryScreen() },
)

/** [initialLesson] lets you jump straight to a lesson: adb shell am start -n ... --es lesson can */
@Composable
fun AppNavHost(initialLesson: String? = null) {
    val nav = rememberNavController()
    LaunchedEffect(initialLesson) {
        if (initialLesson != null && Lessons.all.any { it.route == initialLesson }) nav.navigate(initialLesson)
    }
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { slideIntoContainer(SlideDirection.Left, tween(350)) + fadeIn() },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.Right, tween(350)) + fadeOut() },
    ) {
        composable("home") { HomeScreen(onOpen = { nav.navigate(it) }) }
        Lessons.all.forEach { lesson ->
            composable(lesson.route) {
                LessonScaffold(lesson, onBack = { nav.popBackStack() }) {
                    screens.getValue(lesson.route)()
                }
            }
        }
    }
}
