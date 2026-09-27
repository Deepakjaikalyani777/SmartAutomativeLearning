package com.deepak.automotive.core.vehicle

import kotlin.math.max
import kotlin.math.min

/**
 * Pure, deterministic physics used by the simulator. Kept free of Android classes so it
 * can be unit tested on the JVM.
 */
object VehiclePhysics {
    const val MAX_SPEED_KMH = 220f
    private val gearRatios = floatArrayOf(3.6f, 2.1f, 1.4f, 1.0f, 0.8f, 0.65f)

    /** Advance [state] by [dtSeconds] given throttle 0..100 and brake. */
    fun step(state: VehicleState, throttle: Float, brake: Boolean, dtSeconds: Float): VehicleState {
        val canMove = state.ignition == Ignition.ON && !state.parkingBrakeOn &&
            (state.gear == Gear.DRIVE || state.gear == Gear.REVERSE)

        val accel = if (canMove) throttle / 100f * 12f else 0f          // km/h per second
        val drag = 0.6f + state.speedKmh * 0.012f                         // rolling + air
        val braking = if (brake) 25f else 0f
        val limit = if (state.gear == Gear.REVERSE) 25f else MAX_SPEED_KMH
        val speed = (state.speedKmh + (accel - drag - braking) * dtSeconds).coerceIn(0f, limit)

        val rpm = when {
            state.ignition != Ignition.ON -> 0f
            !canMove || speed < 1f -> 800f + throttle * 25f
            else -> {
                val ratio = gearRatios[virtualGearIndex(speed)]
                min(7000f, max(900f, speed * ratio * 28f + throttle * 8f))
            }
        }
        val fuelUse = if (state.ignition == Ignition.ON) (0.0004f + throttle * 0.00004f) * dtSeconds else 0f
        val battery = if (state.isCharging) {
            // CC/CV charging: fast until 80 %, then tapers (see EV lesson).
            val rate = if (state.evBatteryPercent < 80f) 2.5f else 0.6f
            min(100f, state.evBatteryPercent + rate * dtSeconds)
        } else state.evBatteryPercent - speed * 0.00008f * dtSeconds

        return state.copy(
            speedKmh = speed,
            rpm = rpm,
            fuelPercent = max(0f, state.fuelPercent - fuelUse),
            evBatteryPercent = battery.coerceIn(0f, 100f),
        )
    }

    /** Automatic gearbox: pick an internal gear from speed. */
    fun virtualGearIndex(speedKmh: Float): Int = when {
        speedKmh < 20 -> 0
        speedKmh < 40 -> 1
        speedKmh < 60 -> 2
        speedKmh < 85 -> 3
        speedKmh < 115 -> 4
        else -> 5
    }
}
