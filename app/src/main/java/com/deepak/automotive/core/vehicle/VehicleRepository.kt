package com.deepak.automotive.core.vehicle

import android.content.Context
import com.deepak.automotive.core.car.CarAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Single source of truth for vehicle data. Screens never talk to the Car API directly;
 * they talk to the repository, which can switch between the simulator and the real car.
 */
class VehicleRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isAutomotive: Boolean = CarAvailability.isAutomotive(context)

    private val simulator = SimulatedVehicleDataSource(scope)
    private val carApi: VehicleDataSource? =
        if (isAutomotive) CarApiVehicleDataSource(context.applicationContext) else null

    private val _active = MutableStateFlow<VehicleDataSource>(simulator)
    val activeSource: StateFlow<VehicleDataSource> = _active.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<VehicleState> = _active
        .flatMapLatest { it.state }
        .stateIn(scope, SharingStarted.Eagerly, VehicleState())

    /** Convenience accessor used by screens to send commands. */
    val source: VehicleDataSource get() = _active.value

    init { simulator.start() }

    fun useSource(type: DataSourceType) {
        val next = when (type) {
            DataSourceType.SIMULATOR -> simulator
            DataSourceType.CAR_API -> carApi ?: return
        }
        next.start()
        _active.value = next
    }

    fun close() {
        simulator.stop()
        carApi?.stop()
        scope.cancel()
    }
}
