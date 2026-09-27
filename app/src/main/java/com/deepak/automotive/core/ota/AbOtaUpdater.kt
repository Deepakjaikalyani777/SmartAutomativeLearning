package com.deepak.automotive.core.ota

/**
 * A/B (seamless) OTA update model. Two copies of every partition: the car keeps running
 * on the active slot while the update is written to the inactive one.
 * A failed boot rolls back automatically - a car must never be "bricked" by an update.
 */
enum class Slot { A, B; fun other() = if (this == A) B else A }

enum class OtaPhase(val label: String) {
    IDLE("Up to date"),
    DOWNLOADING("Downloading package (in background / Garage Mode)"),
    VERIFYING("Verifying signature & hash"),
    INSTALLING("Writing inactive slot"),
    READY_TO_REBOOT("Waiting for safe moment (parked, ignition off)"),
    BOOTING_NEW_SLOT("Booting new slot"),
    SUCCESS("New slot marked successful"),
    ROLLED_BACK("Boot failed - rolled back to previous slot"),
}

data class OtaState(
    val active: Slot = Slot.A,
    val versions: Map<Slot, String> = mapOf(Slot.A to "v1.0", Slot.B to "v0.9"),
    val phase: OtaPhase = OtaPhase.IDLE,
    val progress: Float = 0f,
)

object AbOtaUpdater {
    fun advance(state: OtaState, newVersion: String, bootSucceeds: Boolean): OtaState = when (state.phase) {
        OtaPhase.IDLE, OtaPhase.SUCCESS, OtaPhase.ROLLED_BACK ->
            state.copy(phase = OtaPhase.DOWNLOADING, progress = 0f)
        OtaPhase.DOWNLOADING -> state.copy(phase = OtaPhase.VERIFYING)
        OtaPhase.VERIFYING -> state.copy(phase = OtaPhase.INSTALLING)
        OtaPhase.INSTALLING -> state.copy(
            phase = OtaPhase.READY_TO_REBOOT,
            versions = state.versions + (state.active.other() to newVersion),
        )
        OtaPhase.READY_TO_REBOOT -> state.copy(phase = OtaPhase.BOOTING_NEW_SLOT)
        OtaPhase.BOOTING_NEW_SLOT ->
            if (bootSucceeds) state.copy(active = state.active.other(), phase = OtaPhase.SUCCESS)
            else state.copy(phase = OtaPhase.ROLLED_BACK) // bootloader falls back to old slot
    }
}
