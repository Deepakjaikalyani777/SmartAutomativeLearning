package com.deepak.automotive

import com.deepak.automotive.core.ota.AbOtaUpdater
import com.deepak.automotive.core.ota.OtaPhase
import com.deepak.automotive.core.ota.OtaState
import com.deepak.automotive.core.ota.Slot
import com.deepak.automotive.core.power.PowerEvent
import com.deepak.automotive.core.power.PowerState
import com.deepak.automotive.core.power.PowerStateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

class StateMachinesTest {
    @Test fun garageModeThenSuspendThenFastResume() {
        var s = PowerState.ON
        s = PowerStateMachine.next(s, PowerEvent.IGNITION_OFF)
        s = PowerStateMachine.next(s, PowerEvent.GARAGE_MODE_START)
        assertEquals(PowerState.GARAGE_MODE, s)
        s = PowerStateMachine.next(s, PowerEvent.JOBS_DONE)
        s = PowerStateMachine.next(s, PowerEvent.DEEP_SLEEP)
        assertEquals(PowerState.SUSPEND_TO_RAM, s)
        assertEquals(PowerState.ON, PowerStateMachine.next(s, PowerEvent.UNLOCK))
    }

    @Test fun invalidEventIsIgnored() {
        assertEquals(PowerState.OFF, PowerStateMachine.next(PowerState.OFF, PowerEvent.JOBS_DONE))
    }

    private fun runOta(bootOk: Boolean): OtaState {
        var s = OtaState()
        do { s = AbOtaUpdater.advance(s, "v2.0", bootOk) } while (s.phase != OtaPhase.SUCCESS && s.phase != OtaPhase.ROLLED_BACK)
        return s
    }

    @Test fun successfulOtaSwitchesSlot() {
        val s = runOta(bootOk = true)
        assertEquals(Slot.B, s.active)
        assertEquals("v2.0", s.versions[Slot.B])
    }

    @Test fun failedBootRollsBack() {
        val s = runOta(bootOk = false)
        assertEquals(OtaPhase.ROLLED_BACK, s.phase)
        assertEquals(Slot.A, s.active)
    }
}
