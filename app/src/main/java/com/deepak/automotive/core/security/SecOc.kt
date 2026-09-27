package com.deepak.automotive.core.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * AUTOSAR SecOC (Secure Onboard Communication) in miniature.
 *
 * CAN has NO sender authentication: any node can send ID 0x050 "brake". SecOC appends a
 * truncated MAC + freshness value so receivers can reject spoofed or replayed frames.
 *
 *   Secured PDU = payload | freshness (counter) | truncated MAC( key, id | payload | freshness )
 */
class SecOc(key: ByteArray, private val macBytes: Int = 4) {

    private val keySpec = SecretKeySpec(key, "HmacSHA256")
    private var txCounter = 0L
    private var lastRxCounter = -1L

    data class SecuredPdu(val id: Int, val payload: ByteArray, val freshness: Long, val mac: ByteArray)

    enum class Verdict { ACCEPTED, REJECTED_BAD_MAC, REJECTED_REPLAY }

    fun protect(id: Int, payload: ByteArray): SecuredPdu {
        val fv = ++txCounter
        return SecuredPdu(id, payload, fv, mac(id, payload, fv))
    }

    fun verify(pdu: SecuredPdu): Verdict {
        if (!mac(pdu.id, pdu.payload, pdu.freshness).contentEquals(pdu.mac)) return Verdict.REJECTED_BAD_MAC
        if (pdu.freshness <= lastRxCounter) return Verdict.REJECTED_REPLAY
        lastRxCounter = pdu.freshness
        return Verdict.ACCEPTED
    }

    private fun mac(id: Int, payload: ByteArray, freshness: Long): ByteArray {
        val m = Mac.getInstance("HmacSHA256")
        m.init(keySpec)
        m.update(byteArrayOf((id shr 8).toByte(), id.toByte()))
        m.update(payload)
        m.update(ByteArray(8) { i -> (freshness shr (56 - 8 * i)).toByte() })
        return m.doFinal().copyOf(macBytes) // truncated: CAN frames have little room
    }
}
