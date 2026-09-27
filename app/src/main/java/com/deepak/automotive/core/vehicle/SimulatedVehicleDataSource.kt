package com.deepak.automotive.core.vehicle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A software "car". Plays the role the VHAL emulator plays on the AAOS emulator:
 * it produces a stream of property changes at a fixed rate (10 Hz here).
 */
class SimulatedVehicleDataSource(private val scope: CoroutineScope) : VehicleDataSource {

    private val _state = MutableStateFlow(VehicleState())
    override val state: StateFlow<VehicleState> = _state.asStateFlow()

    @Volatile private var throttle = 0f
    @Volatile private var brake = false
    private var job: Job? = null

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            val tickMs = 100L
            while (isActive) {
                _state.update { VehiclePhysics.step(it, throttle, brake, tickMs / 1000f) }
                delay(tickMs)
            }
        }
    }

    override fun stop() { job?.cancel(); job = null }

    override fun setHvacTemperature(zone: HvacZone, celsius: Float) = _state.update {
        val c = celsius.coerceIn(16f, 30f)
        when (zone) {
            HvacZone.DRIVER -> it.copy(driverTempC = c)
            HvacZone.PASSENGER -> it.copy(passengerTempC = c)
        }
    }

    override fun setFanSpeed(speed: Int) = _state.update { it.copy(fanSpeed = speed.coerceIn(0, 6)) }
    override fun setAcOn(on: Boolean) = _state.update { it.copy(acOn = on) }
    override fun setThrottle(percent: Float) { throttle = percent.coerceIn(0f, 100f) }
    override fun setBrake(pressed: Boolean) { brake = pressed }

    override fun shift(gear: Gear) = _state.update {
        // Safety interlock, like a real transmission ECU: no P/R while moving fast.
        if ((gear == Gear.PARK || gear == Gear.REVERSE) && it.speedKmh > 3f) {
            it.copy(lastError = "Shift to ${gear.label} rejected: vehicle moving")
        } else {
            it.copy(gear = gear, parkingBrakeOn = gear == Gear.PARK, lastError = null)
        }
    }

    override fun setIgnition(ignition: Ignition) = _state.update {
        it.copy(ignition = ignition, rpm = if (ignition == Ignition.ON) 800f else 0f)
    }

    override fun setTurnSignal(left: Boolean, right: Boolean) =
        _state.update { it.copy(leftTurnSignal = left, rightTurnSignal = right) }

    override fun setCharging(charging: Boolean) = _state.update {
        if (charging && it.isMoving) it.copy(lastError = "Cannot charge while driving")
        else it.copy(isCharging = charging, lastError = null)
    }

    override fun setRearObstacle(cm: Int) = _state.update { it.copy(rearObstacleCm = cm.coerceIn(0, 300)) }
}
