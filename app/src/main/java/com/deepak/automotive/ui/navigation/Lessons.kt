package com.deepak.automotive.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.deepak.automotive.ui.theme.CarColors

data class LessonNotes(val what: String, val realLife: String, val brainPower: String, val interviewTip: String)

data class Lesson(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val notes: LessonNotes,
)

/** The learning path, in the order an interviewer usually builds up the topic. */
object Lessons {
    val all = listOf(
        Lesson(
            "architecture", "AAOS Architecture", "From app to ECU, layer by layer", Icons.Default.Layers, CarColors.Cyan,
            LessonNotes(
                what = "Android Automotive OS (AAOS) is full Android running ON the car's head unit. Extra layers are added: Car API (android.car), CarService, and the Vehicle HAL (VHAL) that bridges Android to the vehicle network (CAN, LIN, Automotive Ethernet).",
                realLife = "Volvo XC40 Recharge, Polestar 2, GM (Chevrolet Silverado EV), Renault Megane E-Tech and Honda Accord run AAOS with Google built-in. Harman, Bosch, Visteon and Aptiv build head units on top of it for OEMs.",
                brainPower = "Why can't an app talk to the CAN bus directly? What would happen if a buggy music app could send CAN frames?",
                interviewTip = "Draw the stack from top to bottom: Apps → Car API/Car Manager → CarService (binder) → VHAL (AIDL IVehicle) → Vehicle MCU / gateway → CAN/LIN/Ethernet → ECUs. Mention AAOS vs Android Auto (projection) in the first minute.",
            ),
        ),
        Lesson(
            "cluster", "Instrument Cluster", "Speed, RPM, gear, tell-tales", Icons.Default.Dashboard, CarColors.Green,
            LessonNotes(
                what = "The cluster shows safety-relevant driving data. In AAOS it can be a separate display driven by ClusterHomeService / a cluster activity, while safety tell-tales usually run on a certified RTOS / hypervisor partition (e.g. QNX) for ASIL compliance.",
                realLife = "Tesla Model 3 has no cluster (everything on the centre screen); Mercedes MBUX Hyperscreen renders the cluster on QNX and infotainment on Linux/Android side by side under one hypervisor.",
                brainPower = "The gauge needle animates with a spring. Why not just jump to the new value each time a CAN frame arrives?",
                interviewTip = "Say: 'Cluster = safety critical (ISO 26262), IVI = QM/non-safety.' Tell-tales like brake/ABS warnings must not depend on Android booting.",
            ),
        ),
        Lesson(
            "vhal", "Vehicle HAL & Properties", "CarPropertyManager in action", Icons.Default.Memory, CarColors.Violet,
            LessonNotes(
                what = "Every piece of vehicle data is a VHAL property with an ID, a value type, an area (global / seat / door / window / mirror / wheel), an access mode (READ, WRITE, READ_WRITE) and a change mode (STATIC, ON_CHANGE, CONTINUOUS). Apps use CarPropertyManager to get, set and subscribe.",
                realLife = "When you turn the climate knob in a Polestar 2, the UI calls setProperty(HVAC_TEMPERATURE_SET, area=ROW_1_LEFT). VHAL turns it into a CAN signal the HVAC ECU understands.",
                brainPower = "PERF_VEHICLE_SPEED is 0x11600207. Can you split it into group, area, type and ID? (Tap the decoder below.)",
                interviewTip = "Know: CONTINUOUS props need a sample rate (min/max in config); ON_CHANGE props fire only on change; STATIC read once. OEM-specific properties use VENDOR group 0x20000000.",
            ),
        ),
        Lesson(
            "hvac", "HVAC / Climate", "Dual-zone climate with area IDs", Icons.Default.AcUnit, CarColors.Blue,
            LessonNotes(
                what = "HVAC = Heating, Ventilation, Air Conditioning. In AAOS it is a set of area-based VHAL properties (HVAC_TEMPERATURE_SET, HVAC_FAN_SPEED, HVAC_AC_ON, HVAC_SEAT_TEMPERATURE...). Changing them needs CONTROL_CAR_CLIMATE (signature|privileged).",
                realLife = "Driver sets 20°C and passenger 24°C in a dual-zone Audi / Hyundai: same property, two area IDs. Remote pre-conditioning from a phone app goes cloud → TCU (telematics unit) → vehicle bus → HVAC ECU.",
                brainPower = "Why is changing HVAC a privileged permission when reading speed is only 'dangerous'?",
                interviewTip = "Explain area IDs as a bitmask (VehicleAreaSeat ROW_1_LEFT=0x1, ROW_1_RIGHT=0x4). The OEM can group seats into one area (e.g. 0x31) - always read areaIds from CarPropertyConfig.",
            ),
        ),
        Lesson(
            "uxr", "Driver Distraction", "UX restrictions while moving", Icons.Default.Block, CarColors.Red,
            LessonNotes(
                what = "CarUxRestrictionsManager tells apps what they may show based on driving state (PARKED, IDLING, MOVING). While moving: no video, no keyboard, limited list length and string length. Activities must be marked distractionOptimized to stay visible.",
                realLife = "In a Volvo on the move, YouTube stops and the keyboard in Maps is disabled; passengers must use voice. In the US this follows NHTSA visual-manual distraction guidelines (2-second glance rule, 12 s total task time).",
                brainPower = "Drag speed above 0: which items disappear from the list? Why is truncating strings a safety feature?",
                interviewTip = "Apps react to restrictions, not to speed. The OEM configures restrictions per driving state in car_ux_restrictions_map.xml. ActivityBlockingActivity covers non-DO activities.",
            ),
        ),
        Lesson(
            "media", "Media in the Car", "MediaBrowserService + MediaSession", Icons.Default.LibraryMusic, CarColors.Pink,
            LessonNotes(
                what = "Car media apps expose a content tree via MediaBrowserService and playback control via MediaSession. The car (AAOS Media Center or Android Auto) draws the UI, so it is consistent, safe and works with steering-wheel buttons.",
                realLife = "Spotify, Audible and YouTube Music look identical in a Volvo, a GM and a Renault because the OEM's media template draws them - Spotify only supplies browse tree + session.",
                brainPower = "Why doesn't the car let Spotify draw its own UI while driving?",
                interviewTip = "Mention: onGetRoot (validate caller), onLoadChildren (tree), content style hints (grid/list), PlaybackState actions (buttons shown), audio focus with USAGE_MEDIA, and Media3 MediaLibraryService as the modern equivalent.",
            ),
        ),
        Lesson(
            "audio", "Car Audio & Focus", "Ducking, zones and the focus matrix", Icons.Default.VolumeUp, CarColors.Orange,
            LessonNotes(
                what = "AAOS maps AudioAttributes usages to audio contexts, then to volume groups and output buses (devices) on the car's amplifier/DSP. CarAudioService arbitrates focus with an interaction matrix: EXCLUSIVE, CONCURRENT (duck) or REJECT.",
                realLife = "Music ducks (gets quieter) when Google Maps says 'turn left'; a phone call pauses music completely; a seat-belt chime always plays. Rear-seat passengers can have their own audio zone (headphones) in a BMW 7 series.",
                brainPower = "Should a navigation prompt be allowed to interrupt a phone call? What should happen to the music?",
                interviewTip = "Explain audio zones (primary + rear zones), dynamic routing via car_audio_configuration.xml, and that ducking in AAOS is often done by the external amplifier, not by Android mixing.",
            ),
        ),
        Lesson(
            "can", "CAN Bus", "Frames, IDs and arbitration", Icons.Default.Cable, CarColors.Amber,
            LessonNotes(
                what = "Controller Area Network: a 2-wire differential bus (CAN_H / CAN_L) shared by ECUs. Frames have an 11-bit (or 29-bit) ID and up to 8 bytes (64 in CAN FD). Lower ID = higher priority, decided by non-destructive bitwise arbitration.",
                realLife = "Brake ECU (ID 0x050) always wins against the infotainment ECU (0x5F0). A DBC file tells engineers which bits of which frame hold 'vehicle speed'. Mechanics read the bus with an OBD-II dongle.",
                brainPower = "Two ECUs start sending at the same time. Why is no data lost and no retransmission needed for the winner?",
                interviewTip = "Know: 500 kbps HS-CAN, 120 Ω termination at both ends, CAN FD (up to 64 bytes, higher data rate), LIN for cheap sensors, FlexRay for chassis, Automotive Ethernet (100/1000BASE-T1) for cameras and ADAS. CAN has no authentication → SecOC.",
            ),
        ),
        Lesson(
            "park", "Park Assist & EVS", "Ultrasonic sensors & rear camera", Icons.Default.Sensors, CarColors.Green,
            LessonNotes(
                what = "Park assist uses ultrasonic sensors (distance by echo time) and cameras. AAOS has the Extended View System (EVS) so the rear-view camera shows within ~2 seconds of power-on, even before Android's UI is ready.",
                realLife = "Shift into R and the camera appears instantly with beeps getting faster as you approach the wall - a legal requirement in the US (FMVSS 111) since 2018.",
                brainPower = "Why can't the rear camera wait for the Android launcher to finish booting?",
                interviewTip = "EVS: native evs_manager + HAL, runs early in boot, independent of the Java framework. Mention latency budgets and the 'reverse gear → GEAR_SELECTION property → EVS app' trigger.",
            ),
        ),
        Lesson(
            "ev", "EV & Charging", "Battery, range and charging curve", Icons.Default.BatteryChargingFull, CarColors.Green,
            LessonNotes(
                what = "EV properties include EV_BATTERY_LEVEL (Wh), INFO_EV_BATTERY_CAPACITY, EV_CHARGE_PORT_CONNECTED, EV_BATTERY_INSTANTANEOUS_CHARGE_RATE. Charging apps use the Car App Library CHARGING category.",
                realLife = "Google Maps in a Polestar plans route stops at chargers using live battery level from VHAL. DC fast charging is quick until ~80 % and then slows (constant current → constant voltage).",
                brainPower = "Why does charging from 80 % to 100 % take almost as long as 10 % to 80 %?",
                interviewTip = "Talk about range estimation (consumption × remaining energy, adjusted for temperature and HVAC), and that charging apps must use templates on AAOS for Play Store distribution.",
            ),
        ),
        Lesson(
            "power", "Power Management", "Suspend-to-RAM, Garage Mode", Icons.Default.PowerSettingsNew, CarColors.Amber,
            LessonNotes(
                what = "Car Power Management Service (CPMS) follows the Vehicle MCU's requests via VHAL (AP_POWER_STATE_REQ). States: ON, SHUTDOWN_PREPARE, WAIT_FOR_VHAL, SUSPEND (to RAM / disk). Garage Mode runs idle jobs after the driver leaves.",
                realLife = "Your car downloads a system update overnight in Garage Mode, then resumes in under 2 seconds when you unlock it the next morning (suspend-to-RAM), instead of a 30-second cold boot.",
                brainPower = "What would happen to the 12 V battery if Android stayed fully ON all night?",
                interviewTip = "Mention CarPowerManager listeners, JobScheduler constraints for Garage Mode (idle + charging), silent mode, and that power policies (CarPowerPolicy) turn components like audio/display on/off.",
            ),
        ),
        Lesson(
            "ota", "OTA Updates", "A/B seamless updates & rollback", Icons.Default.SystemUpdate, CarColors.Cyan,
            LessonNotes(
                what = "Over-The-Air updates for Android (A/B partitions, update_engine) and for ECUs (via Uptane-style secure update frameworks). The new image is written to the inactive slot while the car keeps working; a failed boot rolls back.",
                realLife = "Tesla and Ford BlueCruise add features by OTA; UNECE R156 legally requires a Software Update Management System (SUMS) for cars sold in the EU since 2022/2024.",
                brainPower = "What must the car check before rebooting into the new slot? (Hint: is anyone driving?)",
                interviewTip = "Say: signed packages (verified by AVB), download in Garage Mode, install to inactive slot, reboot only when parked & user-approved, mark boot successful, automatic rollback. Also mention delta updates to save bandwidth.",
            ),
        ),
        Lesson(
            "security", "Automotive Security", "Secure boot, SecOC, Keystore", Icons.Default.Security, CarColors.Red,
            LessonNotes(
                what = "Defence in depth: secure/verified boot chain, SELinux, Android permissions, signature-level car permissions, Keystore/TEE/HSM for keys, gateway firewalls between buses, SecOC message authentication, and an intrusion detection system.",
                realLife = "The 2015 Jeep Cherokee hack (Miller & Valasek) reached the CAN bus through the head unit's cellular connection → 1.4 million vehicles recalled. That is why IVI and powertrain buses are now separated by a secure gateway.",
                brainPower = "An attacker replays yesterday's valid 'unlock doors' frame. Which SecOC field stops it?",
                interviewTip = "Quote the standards: ISO/SAE 21434 (cybersecurity engineering), UNECE R155 (CSMS, mandatory in EU from July 2024), TARA (threat analysis). Know STRIDE and the difference between safety (ISO 26262) and security.",
            ),
        ),
        Lesson(
            "zones", "Multi-Display & Users", "Occupant zones, cluster, passengers", Icons.Default.ViewQuilt, CarColors.Violet,
            LessonNotes(
                what = "CarOccupantZoneManager maps occupants (driver, front passenger, rear) to displays, audio zones and Android users. AAOS supports multiple users (profiles) and, since Android 13/14, concurrent users on passenger displays (MUMD).",
                realLife = "Mercedes EQS passenger screen plays video while driving because it is not visible to the driver (camera-based driver gaze protection). Rear-seat entertainment screens run their own user.",
                brainPower = "Why may the passenger display show video while the centre display must block it?",
                interviewTip = "Mention: headless system user 0 in AAOS, guest user, user switching on driver change (key fob → profile), display to occupant zone mapping in config_occupant_zones.",
            ),
        ),
        Lesson(
            "carapp", "Car App Library", "Templates for Android Auto & AAOS", Icons.Default.AccountTree, CarColors.Pink,
            LessonNotes(
                what = "androidx.car.app lets 3rd-party apps build navigation, POI (parking/charging), IoT, weather, messaging apps using templates (ListTemplate, PaneTemplate, NavigationTemplate, MapWithContentTemplate...). The host renders them.",
                realLife = "ChargePoint, PlugShare, Waze and Google Home in the car are built with Car App Library templates. This project's ChargingStationCarAppService is a working example - run it on the Desktop Head Unit (DHU).",
                brainPower = "Templates limit you to ~6 list rows and 5 screens of depth. Why is that good for the driver?",
                interviewTip = "Know CarAppService → Session → Screen → Template, HostValidator for security, app categories in the manifest, and the task-depth limit. Test with the DHU for Android Auto or the AAOS emulator.",
            ),
        ),
    )

    fun byRoute(route: String) = all.first { it.route == route }
}
