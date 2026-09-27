package com.deepak.automotive

import android.app.Application
import com.deepak.automotive.core.vehicle.VehicleRepository

/**
 * Application entry point. Creates the single [VehicleRepository] so every screen
 * observes the same vehicle state (just like every app in a real car observes the same VHAL).
 */
class AutomotiveApp : Application() {

    lateinit var vehicleRepository: VehicleRepository
        private set

    override fun onCreate() {
        super.onCreate()
        vehicleRepository = VehicleRepository(this)
    }

    override fun onTerminate() {
        vehicleRepository.close()
        super.onTerminate()
    }
}
