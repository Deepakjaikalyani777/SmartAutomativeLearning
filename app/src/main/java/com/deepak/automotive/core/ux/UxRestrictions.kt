package com.deepak.automotive.core.ux

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-level copy of android.car.drivingstate.CarUxRestrictions.
 * Driving state (PARKED / IDLING / MOVING) is turned into UX restrictions by CarService using
 * the OEM's car_ux_restrictions_map.xml. Apps never check speed themselves - they obey restrictions.
 */
data class UxRestrictionsState(
    val requiresDistractionOptimization: Boolean,
    val activeRestrictions: Set<Restriction>,
    val maxStringLength: Int,
    val maxCumulativeItems: Int,
    val maxContentDepth: Int,
) {
    enum class Restriction(val flag: Int, val meaning: String) {
        NO_DIALPAD(0x1, "No dial pad"),
        NO_FILTERING(0x2, "No list filtering"),
        LIMIT_STRING_LENGTH(0x4, "Truncate long text"),
        NO_KEYBOARD(0x8, "No on-screen keyboard"),
        NO_VIDEO(0x10, "No video playback"),
        LIMIT_CONTENT(0x20, "Limit list length & depth"),
        NO_SETUP(0x40, "No setup / settings flows"),
        NO_TEXT_MESSAGE(0x80, "No reading text messages"),
        NO_VOICE_TRANSCRIPTION(0x100, "No voice-to-text display"),
    }

    companion object {
        val BASELINE = UxRestrictionsState(false, emptySet(), Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        /** Typical OEM config while moving (matches the AOSP reference defaults). */
        val FULLY_RESTRICTED = UxRestrictionsState(
            requiresDistractionOptimization = true,
            activeRestrictions = Restriction.entries.toSet(),
            maxStringLength = 120,
            maxCumulativeItems = 21,
            maxContentDepth = 3,
        )

        /** Simulated CarService logic: speed -> driving state -> restrictions. */
        fun forSpeed(speedKmh: Float): UxRestrictionsState =
            if (speedKmh > 0.5f) FULLY_RESTRICTED else BASELINE

        fun fromMask(mask: Int, requiresDo: Boolean, maxLen: Int, maxItems: Int, maxDepth: Int) =
            UxRestrictionsState(
                requiresDo,
                Restriction.entries.filter { mask and it.flag != 0 }.toSet(),
                maxLen, maxItems, maxDepth,
            )
    }
}

/**
 * Listens to the real CarUxRestrictionsManager on AAOS.
 * Use only when CarAvailability.isAutomotive() is true.
 */
class CarUxRestrictionsSource(private val context: Context) {
    private val _state = MutableStateFlow(UxRestrictionsState.BASELINE)
    val state: StateFlow<UxRestrictionsState> = _state.asStateFlow()

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    fun start() {
        car = Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER) { c, ready ->
            if (!ready) return@createCar
            val m = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            manager = m
            m.currentCarUxRestrictions?.let { publish(it) }
            m.registerListener { publish(it) }
        }
    }

    fun stop() {
        manager?.unregisterListener()
        car?.disconnect()
        car = null
    }

    private fun publish(r: CarUxRestrictions) {
        _state.value = UxRestrictionsState.fromMask(
            r.activeRestrictions, r.isRequiresDistractionOptimization,
            r.maxRestrictedStringLength, r.maxCumulativeContentItems, r.maxContentDepth,
        )
    }
}
