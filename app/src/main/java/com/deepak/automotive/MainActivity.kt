package com.deepak.automotive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.deepak.automotive.core.car.CarAvailability
import com.deepak.automotive.ui.navigation.AppNavHost
import com.deepak.automotive.ui.theme.AutomotiveTheme

class MainActivity : ComponentActivity() {

    private val requestCarPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* UI shows errors */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (CarAvailability.isAutomotive(this)) {
            // "dangerous" car permissions need a runtime prompt, exactly like CAMERA on a phone.
            requestCarPermissions.launch(arrayOf(CAR_SPEED, CAR_ENERGY))
        }
        setContent {
            AutomotiveTheme { AppNavHost(initialLesson = intent.getStringExtra(EXTRA_LESSON)) }
        }
    }

    companion object {
        const val EXTRA_LESSON = "lesson"
        private const val CAR_SPEED = "android.car.permission.CAR_SPEED"
        private const val CAR_ENERGY = "android.car.permission.CAR_ENERGY"
    }
}
