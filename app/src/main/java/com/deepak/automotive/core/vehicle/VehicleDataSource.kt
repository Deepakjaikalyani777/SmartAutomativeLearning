package com.deepak.automotive.core.vehicle

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over "where vehicle data comes from".
 *
 * Real car  -> [CarApiVehicleDataSource] (CarPropertyManager -> CarService -> VHAL -> ECUs)
 * Phone/lab -> [SimulatedVehicleDataSource] (a tiny physics model)
 *
 * Interview tip: this seam is exactly how Tier-1 suppliers test IVI apps on a bench
 * without a car: swap the data source, keep the UI.
 */
interface VehicleDataSource {
    val state: StateFlow<VehicleState>

    fun setHvacTemperature(zone: HvacZone, celsius: Float)
    fun setFanSpeed(speed: Int)
    fun setAcOn(on: Boolean)

    // Driving controls; only meaningful for the simulator.
    fun setThrottle(percent: Float) {}
    fun setBrake(pressed: Boolean) {}
    fun shift(gear: Gear) {}
    fun setIgnition(ignition: Ignition) {}
    fun setTurnSignal(left: Boolean, right: Boolean) {}
    fun setCharging(charging: Boolean) {}
    fun setRearObstacle(cm: Int) {}

    fun start()
    fun stop()
}
