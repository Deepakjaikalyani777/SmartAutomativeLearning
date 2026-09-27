package com.deepak.automotive

import com.deepak.automotive.core.can.CanBus
import com.deepak.automotive.core.can.CanFrame
import org.junit.Assert.assertEquals
import org.junit.Test

class CanBusTest {
    @Test fun lowestIdWinsArbitration() {
        val brake = CanFrame(0x050, byteArrayOf(1), "brake")
        val ivi = CanFrame(0x5F0, byteArrayOf(1), "ivi")
        val speed = CanFrame(0x0F0, byteArrayOf(1), "speed")
        val result = CanBus.arbitrate(listOf(ivi, speed, brake))
        assertEquals(brake, result.winner)
        assertEquals(2, result.losers.size)
    }

    @Test fun speedSignalRoundTrips() {
        val data = CanBus.encodeSpeed(123.45f)
        assertEquals(123.45f, CanBus.decodeSpeed(data), 0.011f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMoreThanEightBytes() {
        CanFrame(0x100, ByteArray(9), "bad")
    }
}
