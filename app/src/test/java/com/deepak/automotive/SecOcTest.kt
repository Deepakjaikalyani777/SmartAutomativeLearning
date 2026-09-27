package com.deepak.automotive

import com.deepak.automotive.core.security.SecOc
import org.junit.Assert.assertEquals
import org.junit.Test

class SecOcTest {
    private val key = ByteArray(16) { it.toByte() }

    @Test fun acceptsFreshAuthenticFrame() {
        val tx = SecOc(key); val rx = SecOc(key)
        assertEquals(SecOc.Verdict.ACCEPTED, rx.verify(tx.protect(0x2A0, byteArrayOf(1))))
    }

    @Test fun rejectsReplay() {
        val tx = SecOc(key); val rx = SecOc(key)
        val pdu = tx.protect(0x2A0, byteArrayOf(1))
        rx.verify(pdu)
        assertEquals(SecOc.Verdict.REJECTED_REPLAY, rx.verify(pdu))
    }

    @Test fun rejectsForgedMac() {
        val rx = SecOc(key)
        val forged = SecOc.SecuredPdu(0x2A0, byteArrayOf(1), 5, byteArrayOf(0, 0, 0, 0))
        assertEquals(SecOc.Verdict.REJECTED_BAD_MAC, rx.verify(forged))
    }

    @Test fun rejectsTamperedPayload() {
        val tx = SecOc(key); val rx = SecOc(key)
        val pdu = tx.protect(0x2A0, byteArrayOf(1))
        assertEquals(SecOc.Verdict.REJECTED_BAD_MAC, rx.verify(pdu.copy(payload = byteArrayOf(0))))
    }
}
