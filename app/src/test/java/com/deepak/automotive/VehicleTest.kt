package com.deepak.automotive

import com.deepak.automotive.core.audio.CarAudioUsage
import com.deepak.automotive.core.audio.FocusMatrix
import com.deepak.automotive.core.audio.Interaction
import com.deepak.automotive.core.ux.UxRestrictionsState
import com.deepak.automotive.core.vehicle.Gear
import com.deepak.automotive.core.vehicle.VehiclePhysics
import com.deepak.automotive.core.vehicle.VehicleState
import com.deepak.automotive.core.vehicle.VhalPropertyId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleTest {
    @Test fun decodesVehicleSpeedPropertyId() {
        val id = VhalPropertyId(0x11600207)
        assertEquals("SYSTEM", id.groupName)
        assertEquals("GLOBAL", id.areaName)
        assertEquals("FLOAT", id.typeName)
        assertEquals(0x0207, id.uniqueId)
    }

    @Test fun decodesHvacSeatProperty() {
        val id = VhalPropertyId(0x15600503)
        assertEquals("SEAT", id.areaName)
    }

    @Test fun carDoesNotMoveInPark() {
        var s = VehicleState(gear = Gear.PARK)
        repeat(50) { s = VehiclePhysics.step(s, 100f, false, 0.1f) }
        assertEquals(0f, s.speedKmh, 0.001f)
    }

    @Test fun carAcceleratesInDrive() {
        var s = VehicleState(gear = Gear.DRIVE, parkingBrakeOn = false)
        repeat(50) { s = VehiclePhysics.step(s, 100f, false, 0.1f) }
        assertTrue(s.speedKmh > 30f)
    }

    @Test fun movingIsFullyRestricted() {
        assertTrue(UxRestrictionsState.forSpeed(30f).requiresDistractionOptimization)
        assertEquals(UxRestrictionsState.BASELINE, UxRestrictionsState.forSpeed(0f))
    }

    @Test fun navigationDucksMusicAndCallRejectsMusic() {
        assertEquals(Interaction.CONCURRENT_DUCK, FocusMatrix.interaction(CarAudioUsage.MEDIA, CarAudioUsage.NAVIGATION))
        assertEquals(Interaction.REJECT, FocusMatrix.interaction(CarAudioUsage.CALL, CarAudioUsage.MEDIA))
    }
}
