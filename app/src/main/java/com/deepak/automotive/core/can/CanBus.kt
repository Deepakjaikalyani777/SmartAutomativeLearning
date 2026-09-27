package com.deepak.automotive.core.can

/**
 * Classic CAN 2.0A frame: 11-bit identifier, up to 8 data bytes.
 * The ID is also the PRIORITY: lower ID = higher priority (dominant 0 bits win arbitration).
 */
data class CanFrame(val id: Int, val data: ByteArray, val sender: String) {
    init {
        require(id in 0..0x7FF) { "Standard CAN ID must be 11 bits" }
        require(data.size <= 8) { "Classic CAN carries at most 8 bytes (CAN FD: 64)" }
    }
    val dlc: Int get() = data.size
    fun idBits(): String = id.toString(2).padStart(11, '0')

    override fun equals(other: Any?) =
        other is CanFrame && id == other.id && data.contentEquals(other.data) && sender == other.sender
    override fun hashCode() = 31 * (31 * id + data.contentHashCode()) + sender.hashCode()
}

/** Result of one arbitration round, bit by bit, for the animation. */
data class ArbitrationResult(val winner: CanFrame, val losers: List<CanFrame>, val decidedAtBit: Int)

object CanBus {

    /**
     * Bitwise arbitration. Every node sends its ID MSB-first; the bus is a wired-AND so a
     * dominant 0 overwrites a recessive 1. A node that sends 1 but reads 0 backs off.
     */
    fun arbitrate(contenders: List<CanFrame>): ArbitrationResult {
        require(contenders.isNotEmpty())
        var alive = contenders
        var decidedAt = 10
        for (bit in 10 downTo 0) {
            val busLevel = alive.minOf { (it.id shr bit) and 1 } // wired-AND: any 0 wins
            alive = alive.filter { (it.id shr bit) and 1 == busLevel }
            if (alive.size == 1) { decidedAt = 10 - bit; break }
        }
        val winner = alive.first()
        return ArbitrationResult(winner, contenders - winner, decidedAt)
    }

    // ---- Signal packing, the job a DBC file describes ----

    /** Speed signal: 16 bit, factor 0.01 km/h, little-endian at byte 0. (like a DBC entry) */
    fun encodeSpeed(speedKmh: Float): ByteArray {
        val raw = (speedKmh / 0.01f).toInt().coerceIn(0, 0xFFFF)
        return byteArrayOf((raw and 0xFF).toByte(), (raw shr 8 and 0xFF).toByte())
    }

    fun decodeSpeed(data: ByteArray): Float {
        val raw = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
        return raw * 0.01f
    }

    /** Well-known demo IDs used across the app's CAN lesson. */
    object Ids {
        const val BRAKE = 0x050       // safety critical -> lowest ID -> highest priority
        const val ENGINE_RPM = 0x0C0
        const val VEHICLE_SPEED = 0x0F0
        const val DOOR_STATUS = 0x2A0
        const val IVI_MEDIA = 0x5F0   // infotainment -> lowest priority
    }
}
