package com.deepak.automotive.core.power

/**
 * Simplified Car Power Management Service (CPMS) state machine.
 * The Vehicle MCU (always powered, talks CAN) drives the Android SoC through VHAL's
 * AP_POWER_STATE_REQ / AP_POWER_STATE_REPORT properties.
 */
enum class PowerState(val description: String) {
    OFF("SoC unpowered. Only the Vehicle MCU is awake, listening on CAN."),
    WAIT_FOR_VHAL("Android booting; CarService waits for VHAL to come up."),
    ON("Fully running. Screens on, apps usable."),
    SHUTDOWN_PREPARE("Driver left. Apps get a callback to save state; Garage Mode may start."),
    GARAGE_MODE("Car parked & off: runs idle jobs (OTA download, app updates, map sync)."),
    WAIT_FOR_FINISH("Everything done; telling VHAL it is safe to cut power."),
    SUSPEND_TO_RAM("Suspend-to-RAM: DRAM kept alive, boots back in < 2 s on unlock."),
}

enum class PowerEvent(val label: String) {
    UNLOCK("Unlock / door open"),
    VHAL_READY("VHAL ready"),
    IGNITION_OFF("Ignition off"),
    GARAGE_MODE_START("Idle jobs pending"),
    JOBS_DONE("Jobs finished"),
    DEEP_SLEEP("Enter deep sleep"),
    POWER_CUT("Power cut"),
}

object PowerStateMachine {
    fun next(state: PowerState, event: PowerEvent): PowerState = when (state to event) {
        PowerState.OFF to PowerEvent.UNLOCK -> PowerState.WAIT_FOR_VHAL
        PowerState.WAIT_FOR_VHAL to PowerEvent.VHAL_READY -> PowerState.ON
        PowerState.ON to PowerEvent.IGNITION_OFF -> PowerState.SHUTDOWN_PREPARE
        PowerState.SHUTDOWN_PREPARE to PowerEvent.GARAGE_MODE_START -> PowerState.GARAGE_MODE
        PowerState.SHUTDOWN_PREPARE to PowerEvent.JOBS_DONE -> PowerState.WAIT_FOR_FINISH
        PowerState.SHUTDOWN_PREPARE to PowerEvent.UNLOCK -> PowerState.ON  // driver came back
        PowerState.GARAGE_MODE to PowerEvent.JOBS_DONE -> PowerState.WAIT_FOR_FINISH
        PowerState.GARAGE_MODE to PowerEvent.UNLOCK -> PowerState.ON       // garage mode cancelled
        PowerState.WAIT_FOR_FINISH to PowerEvent.DEEP_SLEEP -> PowerState.SUSPEND_TO_RAM
        PowerState.WAIT_FOR_FINISH to PowerEvent.POWER_CUT -> PowerState.OFF
        PowerState.SUSPEND_TO_RAM to PowerEvent.UNLOCK -> PowerState.ON     // fast resume
        PowerState.SUSPEND_TO_RAM to PowerEvent.POWER_CUT -> PowerState.OFF
        else -> state
    }

    fun allowedEvents(state: PowerState): List<PowerEvent> =
        PowerEvent.entries.filter { next(state, it) != state }
}
