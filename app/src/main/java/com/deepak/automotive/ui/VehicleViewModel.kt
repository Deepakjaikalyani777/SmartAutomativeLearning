package com.deepak.automotive.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.deepak.automotive.AutomotiveApp
import com.deepak.automotive.core.vehicle.DataSourceType
import com.deepak.automotive.core.vehicle.VehicleDataSource
import com.deepak.automotive.core.vehicle.VehicleState
import kotlinx.coroutines.flow.StateFlow

/**
 * MVVM: screens observe [state] (StateFlow survives rotation / display changes) and send
 * commands through [car]. Screens never see whether data is simulated or real.
 */
class VehicleViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as AutomotiveApp).vehicleRepository

    val state: StateFlow<VehicleState> = repository.state
    val isAutomotive: Boolean = repository.isAutomotive
    val car: VehicleDataSource get() = repository.source

    fun useSource(type: DataSourceType) = repository.useSource(type)
}
