from hf import *


def ch10():
    s = chapter("10", "Cameras, park assist, EVS and ADAS", "seeing around the car")
    s.append(P("<b>Ultrasonic sensors</b> in the bumpers send a 40-50 kHz ping and time the echo: distance = speed of "
               "sound (≈ 343 m/s) × time / 2. <b>Cameras</b> give rear/surround views. <b>Radar and lidar</b> feed ADAS "
               "features like adaptive cruise and emergency braking (on dedicated ADAS ECUs, not Android)."))
    s += flow_diagram(["Gear → R\n(VHAL)", "evs_manager\n(native)", "EVS HAL\ncamera", "Display\n(overlay)",
                       "Android UI\n(optional)"], labels=["event", "open stream", "frames", "later"])
    s += box("real", "US FMVSS 111 requires a rear-view image within <b>2 seconds</b> of shifting into reverse on all new light "
                     "vehicles since May 2018. That is why AAOS has the Extended View System (EVS): a native stack started "
                     "early by init that can show the camera before the Java framework has finished booting.")
    s += table([
        ["SAE level", "Who drives", "Example"],
        ["0", "Driver; warnings only", "Parking beeps, lane departure warning"],
        ["1", "Driver + one assist", "Adaptive cruise OR lane keep"],
        ["2", "Driver supervises steering + speed assist", "Tesla Autopilot, GM Super Cruise, Ford BlueCruise"],
        ["3", "System drives in limited conditions; driver takes over on request", "Mercedes Drive Pilot"],
        ["4", "No driver needed in a defined area", "Waymo robotaxis"],
        ["5", "Everywhere", "Not available yet"],
    ], [20, 70, 80])
    s += box("app", "Lesson <b>9 Park Assist</b>: ultrasonic arcs expand from the rear bumper toward the wall and change "
                    "green → amber → red; enable beep sound in R and the beep interval shrinks as the obstacle gets closer.")
    s += box("bullets", bullets(["distance = 343 m/s × t / 2.", "EVS = early native camera path.",
                                 "FMVSS 111: image within 2 s.", "ADAS runs on safety ECUs; AAOS only visualises."]))
    return s


def ch11():
    s = chapter("11", "Electric vehicles and charging", "range anxiety is a UX problem")
    s += table([
        ["Property", "Meaning"],
        ["EV_BATTERY_LEVEL (Wh)", "Energy left (divide by INFO_EV_BATTERY_CAPACITY for %)"],
        ["EV_CHARGE_PORT_OPEN / CONNECTED", "Flap open? Cable plugged in?"],
        ["EV_BATTERY_INSTANTANEOUS_CHARGE_RATE (mW)", "Positive = charging, negative = discharging"],
        ["RANGE_REMAINING (m)", "OEM-computed range"],
        ["EV_CHARGE_PERCENT_LIMIT / EV_CHARGE_SWITCH", "Charge target and start/stop charging"],
    ], [75, 95])
    s.append(P("Why fast charging slows down after 80 %", "h2"))
    s.append(P("Lithium-ion cells are charged in two phases: <b>constant current (CC)</b> until the cell voltage reaches "
               "its maximum, then <b>constant voltage (CV)</b> where the current tapers to avoid lithium plating and heat. "
               "So 10 → 80 % may take ~20-30 minutes and 80 → 100 % almost as long."))
    s += box("real", "Google Maps on AAOS (Polestar, Volvo) plans charging stops using live EV_BATTERY_LEVEL and predicts "
                     "arrival SoC; it can pre-condition the battery so it charges faster on arrival. Charging-network "
                     "apps (ChargePoint, PlugShare) use the Car App Library CHARGING category.")
    s += box("app", "Lesson <b>10 EV & Charging</b>: plug in; the battery glows and fills, the dot slides along the CC/CV "
                    "curve and the label switches from 'Fast charging (CC)' to 'Tapering (CV)' at 80 %. Try plugging in "
                    "while driving: rejected.")
    s += box("bullets", bullets(["Battery level in Wh; % = level / capacity.", "CC then CV; 80 % knee.",
                                 "Range = usable energy / consumption, adjusted for HVAC & temperature.",
                                 "Charging apps = Car App Library CHARGING category."]))
    return s


def ch12():
    s = chapter("12", "Power management", "a car is 'off' for 95 % of its life")
    s += state_diagram(
        ["OFF", "WAIT_FOR_VHAL", "ON", "SHUTDOWN_PREPARE", "GARAGE_MODE", "WAIT_FOR_FINISH", "SUSPEND_TO_RAM"],
        [("OFF", "WAIT_FOR_VHAL", "unlock"), ("WAIT_FOR_VHAL", "ON", "vhal ready"),
         ("ON", "SHUTDOWN_PREPARE", "ignition off"), ("SHUTDOWN_PREPARE", "GARAGE_MODE", "jobs pending"),
         ("GARAGE_MODE", "WAIT_FOR_FINISH", "done"), ("WAIT_FOR_FINISH", "SUSPEND_TO_RAM", "sleep")],
        cols=3, caption="Simplified CPMS states (see PowerStateMachine.kt)")
    s.append(P("The <b>Vehicle MCU</b> is always powered and decides when the Android SoC sleeps or wakes. It talks "
               "to CarPowerManagementService through the VHAL properties AP_POWER_STATE_REQ and AP_POWER_STATE_REPORT."))
    s += table([
        ["Concept", "What it means"],
        ["Suspend-to-RAM", "Keep DRAM powered, everything else off; resume in ~1-2 s instead of a cold boot"],
        ["Suspend-to-disk (hibernation)", "Write RAM to storage; zero drain, slower resume"],
        ["Garage Mode", "After the driver leaves: run idle JobScheduler jobs (updates, sync) for a limited time"],
        ["Power policies", "CarPowerPolicy turns components (audio, display, Wi-Fi, BT) on/off per state"],
        ["Silent mode", "Boot with screens/audio off, e.g. for an OTA at night"],
    ], [50, 120])
    s += box("brain", "The 12 V battery has maybe 60 Ah. If the head unit drew 2 A all night it would be half empty by "
                      "morning. What would the driver experience?")
    s += box("app", "Lesson <b>11 Power</b>: only valid events are shown for the current state; the active state pulses. "
                    "Go ON → Ignition off → Idle jobs pending → Jobs finished → Enter deep sleep → Unlock.")
    s += box("bullets", bullets(["The MCU is the boss of power.", "Suspend-to-RAM = fast wake.",
                                 "Garage Mode runs idle jobs after the drive.", "Listen to CarPowerManager to save state."]))
    return s


def ch13():
    s = chapter("13", "Over-the-air (OTA) updates", "update a car without a workshop")
    s += flow_diagram(["OEM backend\n(signed pkg)", "TCU / Wi-Fi\ndownload", "Verify\nsignature", "Write\ninactive slot",
                       "Wait: parked\n+ user OK", "Reboot to\nnew slot", "Mark success\nor ROLLBACK"])
    s += table([
        ["Term", "Meaning"],
        ["A/B (seamless) update", "Two copies of partitions; install to the inactive one while driving"],
        ["update_engine", "Android daemon that applies A/B payloads (full or delta)"],
        ["AVB + dm-verity", "Verified boot checks the new slot's signature and every block at runtime"],
        ["Virtual A/B", "Snapshots save storage space instead of full duplicate partitions"],
        ["Uptane", "Secure update framework for ECUs (director + image repositories, compromise-resilient)"],
        ["UNECE R156 / SUMS", "EU-mandated Software Update Management System for OEMs"],
    ], [50, 120])
    s += box("real", "Tesla, Polestar, Ford and GM ship new features and fixes by OTA, sometimes including changes to "
                     "driving features, not only infotainment. A recall that once meant a workshop visit can now be an "
                     "overnight software update.")
    s += box("app", "Lesson <b>12 OTA</b>: start an update; the inactive slot glows amber while written, then the slots "
                    "swap. Turn on 'Simulate a broken image' to watch the rollback. Drive during READY_TO_REBOOT: the "
                    "reboot waits until you park.")
    s += box("watch", "Never reboot a head unit while the car is moving: the driver loses the rear camera, navigation and "
                      "warnings. Always gate on driving state and ask the user.")
    s += box("bullets", bullets(["Install to inactive slot; reboot later.", "Verified boot + rollback = no bricking.",
                                 "Download in Garage Mode.", "R156 makes SUMS mandatory in the EU."]))
    return s


def ch14():
    s = chapter("14", "Multiple displays, users and occupant zones", "one car, many screens, many people")
    s.append(P("<b>CarOccupantZoneManager</b> maps each occupant (driver, front passenger, rear seats) to displays, an "
               "audio zone and an Android user. Since Android 14, AAOS supports <b>multi-user multi-display (MUMD)</b>: "
               "several users logged in concurrently on different screens."))
    s += table([
        ["Zone", "Display", "User", "Video while moving?"],
        ["Driver", "Cluster + centre", "Current driver user (e.g. 10)", "No (UX restrictions)"],
        ["Front passenger", "Passenger display", "Visitor or passenger user", "Yes, if not visible to driver"],
        ["Rear left / right", "Rear-seat screens", "Separate users", "Yes"],
    ], [34, 40, 50, 46])
    s += box("real", "Mercedes EQS/EQE passenger screens play video while driving; a driver-facing camera dims the passenger "
                     "screen if the driver looks at it. Driver profiles switch automatically based on the key fob or phone key.")
    s += box("app", "Lesson <b>14 Multi-Display</b>: the focus ring springs from seat to seat, showing each zone's display, "
                    "user, audio zone and video policy.")
    s += box("bullets", bullets(["Occupant zone → displays + audio zone + user.", "User 0 is headless system user.",
                                 "MUMD: concurrent users (Android 14+).", "Restrictions can differ per display."]))
    return s


def ch15():
    s = chapter("15", "The Car App Library", "templates: safe by construction")
    s += stack_diagram(["Host (Android Auto on phone / AAOS templates host)", "CarAppService (+ HostValidator)",
                        "Session (car screen lifecycle)", "ScreenManager stack of Screens", "Screen.onGetTemplate()"],
                       notes=["draws everything", "who may bind?", "created per connection", "max depth ~5",
                              "ListTemplate, PaneTemplate..."])
    s += table([
        ["Category", "Typical templates", "Example apps"],
        ["Navigation", "NavigationTemplate, MapWithContentTemplate", "Waze, Sygic"],
        ["POI: charging / parking", "PlaceListMapTemplate, ListTemplate, PaneTemplate", "ChargePoint, SpotHero"],
        ["IoT", "GridTemplate, ListTemplate", "Google Home (garage door)"],
        ["Weather, messaging, media (newer API levels)", "ListTemplate, MessageTemplate...", "Weather apps, chat"],
    ], [50, 70, 50])
    s += box("watch", ["Templates have limits (rows per list, steps per task, refresh quota). Design for them.",
                       "Use a strict HostValidator in release builds; ALLOW_ALL is for debugging only.",
                       "On AAOS, template apps also need the app-automotive artifact and CarAppActivity."])
    s += box("app", "Lesson <b>15 Car App Library</b> simulates a host: push screens with row taps (slide animation) until "
                    "the depth limit blocks you. Then run the real ChargingStationCarAppService on the Desktop Head Unit.")
    s += box("bullets", bullets(["CarAppService → Session → Screen → Template.", "Host renders; app supplies models.",
                                 "Categories declared in the manifest.", "Test on DHU / AAOS emulator."]))
    return s


def ch16():
    s = chapter("16", "Connectivity: Bluetooth, projection, telematics, V2X")
    s += table([
        ["Tech", "What", "Interview detail"],
        ["Bluetooth HFP", "Hands-free calls", "Car is HF unit, phone is AG; SCO audio"],
        ["A2DP + AVRCP", "Stream music + remote control & metadata", "Car is A2DP sink"],
        ["PBAP / MAP", "Phonebook and messages", "Needs user consent on phone"],
        ["Android Auto / CarPlay", "Phone projection (USB or wireless over Wi-Fi)", "Competes with native AAOS apps"],
        ["TCU (telematics)", "Cellular modem ECU: eCall, remote lock, OTA, fleet data", "eCall mandatory in EU since 2018"],
        ["V2X (C-V2X / DSRC)", "Vehicle-to-vehicle/infrastructure messages", "Signed with PKI certificates; low latency"],
        ["Digital Key (CCC 3.0)", "Phone as key via NFC, BLE, UWB", "UWB ranging stops relay attacks"],
    ], [34, 66, 70])
    s += box("brain", "Why does the car need UWB for a phone key when BLE already works? (Hint: an attacker with two "
                      "radios standing near you and near your car.)")
    return s


def ch17():
    s = chapter("17", "AUTOSAR, SDV and how cars are tested")
    s += table([
        ["", "AUTOSAR Classic", "AUTOSAR Adaptive"],
        ["Runs on", "Microcontrollers (RTOS, static)", "Microprocessors (POSIX, e.g. Linux/QNX)"],
        ["Communication", "Signals over CAN/LIN/FlexRay (COM stack)", "Service-oriented over Ethernet (SOME/IP, DDS)"],
        ["Config", "Static at build time", "Dynamic, updatable"],
        ["Used for", "Brakes, engine, body", "Central compute, ADAS, gateways"],
    ], [30, 70, 70])
    s += table([
        ["Test level", "What", "Tools"],
        ["Unit", "Pure logic on JVM", "JUnit, MockK, Robolectric"],
        ["SIL", "Software-in-the-loop: whole stack with simulated vehicle", "AAOS emulator + fake VHAL"],
        ["HIL", "Hardware-in-the-loop: real head unit + simulated car network", "Vector CANoe, dSPACE"],
        ["Compliance", "Android compatibility", "CTS, VTS, CTS-Verifier, GAS test suites"],
        ["Vehicle", "Real drives, EMC, climate chambers", "Test fleets"],
    ], [26, 80, 64])
    s += box("interview", "If asked 'how would you test this feature?', walk the pyramid: unit → SIL on emulator with "
                          "injected VHAL events → HIL with CANoe replaying real traces → in-vehicle validation.")
    return s


def answers():
    s = chapter("", "Sharpen your pencil: answers")
    s += bullets([
        "(a) 0x15600503 = SYSTEM | SEAT | FLOAT | 0x0503 → HVAC_TEMPERATURE_SET.",
        "(b) 0x11400400 = SYSTEM | GLOBAL | INT32 | 0x0400 → GEAR_SELECTION.",
        "(c) 0x16200B02 = SYSTEM | DOOR | BOOLEAN | 0x0B02 → DOOR_LOCK.",
        "(d) 0x21400001 = VENDOR | GLOBAL | INT32 | 0x0001 → an OEM-specific property.",
        "(e) 0x2A0 = 010 1010 0000 and 0x2A4 = 010 1010 0100. They differ at the bit with value 4 (the 9th bit sent). "
        "0x2A0 sends dominant 0 there and wins.",
        "(f) Little-endian E8 03 → 0x03E8 = 1000 → 1000 × 0.01 = 10.00 km/h.",
    ])
    s += chapter("", "Glossary")
    s += table([
        ["Term", "Meaning"],
        ["AAOS", "Android Automotive OS"], ["ADAS", "Advanced Driver Assistance Systems"],
        ["ASIL", "Automotive Safety Integrity Level (ISO 26262)"], ["BCM", "Body Control Module"],
        ["CAN", "Controller Area Network"], ["CPMS", "Car Power Management Service"],
        ["DBC", "CAN database file describing frames/signals"], ["DHU", "Desktop Head Unit (Android Auto emulator)"],
        ["DO", "Distraction Optimized"], ["DoIP", "Diagnostics over IP"], ["ECU", "Electronic Control Unit"],
        ["EVS", "Extended View System (cameras)"], ["GAS", "Google Automotive Services"],
        ["HIL / SIL", "Hardware / Software in the loop testing"], ["IVI", "In-Vehicle Infotainment"],
        ["MCU", "Microcontroller (vehicle MCU is always on)"], ["OEM", "Car maker"],
        ["SDV", "Software-Defined Vehicle"], ["SecOC", "Secure Onboard Communication"],
        ["TCU", "Telematics Control Unit"], ["Tier-1", "Direct supplier to OEM (Harman, Bosch...)"],
        ["UDS", "Unified Diagnostic Services (ISO 14229)"], ["UXR", "UX Restrictions"],
        ["VHAL", "Vehicle Hardware Abstraction Layer"],
    ], [30, 140])
    return s


def chapters():
    return ch10() + ch11() + ch12() + ch13() + ch14() + ch15() + ch16() + ch17() + answers()
