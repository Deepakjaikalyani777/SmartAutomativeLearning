package com.deepak.automotive.core.vehicle

/** Gear positions, mirroring android.car.VehicleGear. */
enum class Gear(val label: String) { PARK("P"), REVERSE("R"), NEUTRAL("N"), DRIVE("D") }

/** Ignition states, mirroring android.car.VehicleIgnitionState. */
enum class Ignition { LOCK, OFF, ACC, ON, START }

/** Where the numbers on screen come from. */
enum class DataSourceType { SIMULATOR, CAR_API }

/**
 * One immutable snapshot of the car. Every UI screen renders a VehicleState, which keeps
 * the screens pure and easy to test (unidirectional data flow).
 */
data class VehicleState(
    val speedKmh: Float = 0f,
    val rpm: Float = 800f,
    val gear: Gear = Gear.PARK,
    val ignition: Ignition = Ignition.ON,
    val parkingBrakeOn: Boolean = true,
    val fuelPercent: Float = 72f,
    val evBatteryPercent: Float = 64f,
    val isCharging: Boolean = false,
    val outsideTempC: Float = 31f,
    val driverTempC: Float = 22f,
    val passengerTempC: Float = 22f,
    val fanSpeed: Int = 2,
    val acOn: Boolean = true,
    val nightMode: Boolean = false,
    val leftTurnSignal: Boolean = false,
    val rightTurnSignal: Boolean = false,
    val rearObstacleCm: Int = 250,
    val source: DataSourceType = DataSourceType.SIMULATOR,
    val lastError: String? = null,
) {
    val isMoving: Boolean get() = speedKmh > 0.5f
    /** Rough EV range: 64 kWh pack, 16 kWh/100km. */
    val evRangeKm: Int get() = (evBatteryPercent / 100f * 64f / 16f * 100f).toInt()
}

/** HVAC zones map to AAOS VehicleAreaSeat area IDs. */
enum class HvacZone(val areaId: Int, val label: String) {
    DRIVER(0x0001, "ROW_1_LEFT"),      // VehicleAreaSeat.SEAT_ROW_1_LEFT
    PASSENGER(0x0004, "ROW_1_RIGHT"),  // VehicleAreaSeat.SEAT_ROW_1_RIGHT
}
