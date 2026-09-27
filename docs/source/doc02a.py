from hf import *


def ch1():
    s = chapter("1", "Your car is a computer network on wheels", "70-150 computers, one very long ethernet cable... well, almost")
    s.append(P("A modern car contains <b>70 to 150 ECUs</b> (Electronic Control Units): small computers that each own one "
               "job: engine, brakes (ABS/ESC), airbags, doors and lights (Body Control Module), battery management, "
               "cameras, and the one you will work on: the <b>infotainment head unit (IVI)</b>."))
    s += bus_diagram([("Engine ECU", "powertrain"), ("ABS / ESC", "chassis"), ("BCM", "body"),
                      ("Airbag", "safety"), ("Gateway", "firewall"), ("IVI head unit", "Android")],
                     caption="ECUs share buses; a gateway separates domains")
    s.append(P("E/E architecture: three generations", "h2"))
    s += table([
        ["Generation", "Idea", "Example"],
        ["Distributed", "One ECU per function; hundreds of wires; features fixed at the factory", "Most cars before ~2015"],
        ["Domain", "Group ECUs into domains (powertrain, chassis, body, infotainment, ADAS) with a domain controller",
         "VW MEB, many 2018-2023 platforms"],
        ["Zonal / central compute", "A few powerful central computers + zone controllers that handle wiring by "
         "location; Ethernet backbone; software defines features", "Tesla, VW SSP (planned), GM Ultifi-style SDV"],
    ], [30, 90, 50])
    s += box("real", "The <b>Software-Defined Vehicle (SDV)</b> is why companies like Harman, Bosch and KPIT hire so many "
                     "Android and Linux engineers: features (new driving modes, apps, subscriptions like heated seats) "
                     "are now delivered by software and OTA, not by adding hardware.")
    s += box("brain", "If there are 100 ECUs and each is from a different supplier, who guarantees that the brake ECU "
                      "and the infotainment ECU agree on what message 0x0F0 means? (Answer: the OEM's communication "
                      "matrix / DBC file, chapter 9.)")
    s += dumb([
        ("Is the head unit a safety-critical ECU?",
         "Usually no: IVI is 'QM' (quality managed, no ASIL). That is exactly why it runs a big OS like Android and "
         "why it must be <b>isolated</b> from safety ECUs by a gateway. The cluster, when it shows warning lamps, is "
         "safety-relevant."),
        ("What is a Tier-1?",
         "A company that supplies complete systems directly to car makers (OEMs): Harman, Bosch, Continental, Visteon, "
         "Aptiv, Denso. Tier-2 supplies parts/chips to Tier-1s (e.g. Qualcomm, NXP, Renesas)."),
    ])
    s += box("bullets", bullets(["ECU = embedded computer for one function.", "Domains → zonal → central compute.",
                                 "IVI is non-safety (QM); a gateway protects safety domains.",
                                 "OEM = car maker; Tier-1 = system supplier (Harman); Tier-2 = chips."]))
    return s


def ch2():
    s = chapter("2", "Android Automotive OS architecture", "Android, plus a car-shaped layer cake")
    s += stack_diagram(
        ["System apps: Launcher, Media Center, HVAC, Settings, Dialer", "Third-party apps (media, Car App Library)",
         "Car API (android.car): CarPropertyManager, CarAudioManager...", "CarService (com.android.car, system process)",
         "Vehicle HAL: IVehicle AIDL (HIDL before Android 13)", "Vehicle MCU / gateway (often AUTOSAR)",
         "CAN / LIN / Ethernet  →  ECUs"],
        notes=["OEM-customised, privileged", "templates or media only", "Java/Kotlin managers",
               "permissions, fan-out, policies", "written by OEM / Tier-1", "always-on, owns the bus", "real hardware"],
        caption="The AAOS stack. Everything above VHAL is Android; everything below is the car.")
    s += box("app", "Open lesson <b>1 AAOS Architecture</b>. A packet travels down the layers (request: set driver "
                    "temperature) and then back up (event: ECU confirms). Tap a layer to read what it does.")
    s.append(P("Android Auto vs Android Automotive OS", "h2"))
    s += fireside("Android Auto vs Android Automotive OS", [
        ("Android Auto", "I live on your phone. When you plug in or connect wirelessly, I project a car-friendly UI "
                         "onto the car screen. The car is just a monitor + touch + mic for me."),
        ("AAOS", "I <i>am</i> the car's operating system. No phone needed. I boot with the car, I read speed and "
                 "battery from VHAL, I control the climate."),
        ("Android Auto", "But I update with the phone, so I'm always fresh!"),
        ("AAOS", "True, but I update by OTA and I can do things you never can: HVAC, seat settings, EV routing with "
                 "real battery data, rear-seat displays."),
        ("Both", "And developers can write one media app or one Car App Library app that runs on both of us."),
    ])
    s += table([
        ["", "Android Auto", "Android Automotive OS"],
        ["Runs on", "Phone (projection)", "Head unit (embedded)"],
        ["Needs a phone", "Yes", "No"],
        ["Vehicle data (VHAL)", "Very limited (via host)", "Full, permission-controlled"],
        ["App types", "Media, messaging, Car App Library", "Same + system apps built by OEM"],
        ["Updated by", "Play Store on the phone", "OTA from the OEM + Play Store in car"],
        ["Examples", "Almost any car with a USB port", "Polestar 2, Volvo EX30/XC40, GM, Renault, Honda Accord 2023"],
    ], [34, 63, 73])
    s.append(P("GAS or no GAS?", "h2"))
    s.append(P("<b>Google Automotive Services (GAS)</b> = Google Maps, Assistant, Play Store licensed by the OEM "
               "(\"Google built-in\"). OEMs without GAS (e.g. some Chinese OEMs, or those choosing their own ecosystem) "
               "ship AAOS with their own maps, voice and app store."))
    s.append(P("Boot in 30 seconds", "h2"))
    s += flow_diagram(["Boot ROM", "Bootloader\n(verified)", "Kernel", "init +\nnative svcs", "EVS / early\naudio",
                       "Zygote\nsystem_server", "CarService\n+ VHAL", "Launcher"], box_h=32)
    s += box("watch", ["The rear camera and warning chimes cannot wait for Zygote. They are started early from native "
                       "services (EVS, early audio) so they work within legal time limits.",
                       "AAOS runs a <b>headless system user</b> (user 0) in the background; the driver uses a "
                       "secondary user (10, 11...). Code that assumes user 0 is the foreground user breaks on AAOS."])
    s += dumb([
        ("Is CarService an app?", "It is a privileged system app (package com.android.car / updatable car module) running "
                                  "in its own process, bound by system_server at boot. Apps talk to it through "
                                  "binder via the Car API managers."),
        ("Why not let apps talk to VHAL directly?", "Permissions, multiplexing (one VHAL, many clients), rate control, "
                                                    "and survivability: CarService can restart without apps crashing."),
    ])
    s += box("bullets", bullets(["Apps → Car API → CarService → VHAL → MCU → buses.",
                                 "Android Auto projects; AAOS is the OS.", "GAS = Google apps licensed by the OEM.",
                                 "Safety-relevant things (camera, chimes) start early, outside Java.",
                                 "Headless system user 0; drivers are secondary users."]))
    return s


def ch3():
    s = chapter("3", "The Vehicle HAL and vehicle properties", "everything is a property")
    s.append(P("VHAL exposes the car as a list of <b>properties</b>. Each has a 32-bit ID, a value type, an area, "
               "an access mode and a change mode. Google defines hundreds of standard ones "
               "(<font name='Mono'>VehiclePropertyIds</font>); OEMs add <b>vendor</b> properties."))
    s += bits_diagram([("group (28-31)", "0x1", 1), ("area (24-27)", "1", 1), ("type (16-23)", "60", 1.4),
                       ("unique id (0-15)", "0207", 2.2)],
                      caption="PERF_VEHICLE_SPEED = 0x11600207 = SYSTEM | GLOBAL | FLOAT | 0x0207")
    s += table([
        ["Field", "Values you must know"],
        ["Group", "SYSTEM 0x10000000 (Google standard), VENDOR 0x20000000 (OEM-specific)"],
        ["Area type", "GLOBAL 0x01, WINDOW 0x03, MIRROR 0x04, SEAT 0x05, DOOR 0x06, WHEEL 0x07 (shifted << 24)"],
        ["Value type", "STRING 0x10, BOOLEAN 0x20, INT32 0x40, INT32_VEC 0x41, INT64 0x50, FLOAT 0x60, FLOAT_VEC 0x61, "
                       "BYTES 0x70, MIXED 0xE0 (shifted << 16)"],
        ["Access", "READ, WRITE, READ_WRITE"],
        ["Change mode", "STATIC (read once, e.g. INFO_MAKE), ON_CHANGE (gear, doors), CONTINUOUS (speed, rpm: "
                        "subscribe with a sample rate between minSampleRate and maxSampleRate)"],
    ], [30, 140])
    s.append(P("Using CarPropertyManager", "h2"))
    s.append(code("""
val car = Car.createCar(context, null, CAR_WAIT_TIMEOUT_WAIT_FOREVER) { car, ready ->
    if (!ready) return@createCar                       // CarService died
    val pm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
    // read once
    val make = pm.getProperty<String>(VehiclePropertyIds.INFO_MAKE, 0).value
    // subscribe (API 35+: subscribePropertyEvents)
    pm.registerCallback(callback, VehiclePropertyIds.PERF_VEHICLE_SPEED,
                        CarPropertyManager.SENSOR_RATE_UI)
    // write (needs the property's write permission)
    pm.setProperty(Float::class.java, VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                   areaId, 21.5f)
}
"""))
    s += box("real", "When you change the temperature knob in a Polestar 2, the HVAC app calls setProperty on "
                     "HVAC_TEMPERATURE_SET. The Tier-1's VHAL implementation turns it into a signal in a CAN frame "
                     "to the climate ECU, which moves the blend-door actuator. When the ECU confirms, VHAL emits an "
                     "ON_CHANGE event and every subscribed UI updates.")
    s += box("app", "Lesson <b>3 Vehicle HAL</b>: each row flashes when a change event arrives (CONTINUOUS rows flash "
                    "constantly while you drive in the Cluster lesson). Tap a row to split its hex ID into coloured fields.")
    s += box("pencil", ["Decode these IDs (answers at the end of the book):",
                        "(a) 0x15600503   (b) 0x11400400   (c) 0x16200B02   (d) 0x21400001"])
    s += dumb([
        ("What happens if I subscribe to a CONTINUOUS property at 100 Hz but maxSampleRate is 10?",
         "The rate is clamped to what the config allows. Always read CarPropertyConfig first."),
        ("How do I test without a car?",
         "AAOS emulator Extended Controls (Car data), the VHAL emulator/reference VHAL, or "
         "<font name='Mono'>adb shell cmd car_service inject-vhal-event &lt;propId&gt; &lt;value&gt;</font>. "
         "In unit tests, fake the data source (like this app's SimulatedVehicleDataSource)."),
        ("AIDL or HIDL VHAL?", "Android 13 introduced the AIDL VHAL (android.hardware.automotive.vehicle.IVehicle); HIDL "
                               "(2.0) is legacy. New projects implement AIDL."),
    ])
    s += box("watch", "Units! Speed is m/s, temperatures °C, fuel in millilitres, EV battery in Wh, distance in mm. "
                      "Converting at the UI layer (not in VHAL) keeps the platform SI-consistent.")
    s += box("bullets", bullets(["Property = ID + type + area + access + change mode.", "ID bits: group|area|type|id.",
                                 "STATIC read once; ON_CHANGE events; CONTINUOUS with rate.",
                                 "Vendor properties use group 0x2.", "Re-subscribe after CarService restart."]))
    return s


def ch4():
    s = chapter("4", "HVAC and area properties", "same property, different seat")
    s.append(P("Area properties exist once per <b>area ID</b>. For HVAC the area type is SEAT, so area IDs are bitmasks "
               "of VehicleAreaSeat: ROW_1_LEFT = 0x0001, ROW_1_CENTER = 0x0002, ROW_1_RIGHT = 0x0004, "
               "ROW_2_LEFT = 0x0010, ROW_2_CENTER = 0x0020, ROW_2_RIGHT = 0x0040..."))
    s += flow_diagram(["HVAC UI\n(driver +0.5°C)", "CarPropertyManager\nsetProperty", "CarService\nperm check",
                       "VHAL\nset()", "CAN frame\nto HVAC ECU", "Blend door\nmoves"],
                      labels=["area 0x1", "CONTROL_CAR_CLIMATE", "AIDL", "signal", "actuator"])
    s += box("watch", "The OEM decides how seats are grouped. On a 2-zone car the driver area might be 0x31 "
                      "(ROW_1_LEFT | ROW_2_LEFT | ROW_2_CENTER). Never hard-code 0x1: read "
                      "<font name='Mono'>getCarPropertyConfig(id).areaIds</font> and pick the one containing your seat bit. "
                      "This app does exactly that in CarApiVehicleDataSource.setHvac().")
    s += table([
        ["HVAC property", "Type / area", "Real-life use"],
        ["HVAC_TEMPERATURE_SET", "FLOAT / SEAT", "Dual-zone temperature"],
        ["HVAC_FAN_SPEED", "INT32 / SEAT", "Blower level"],
        ["HVAC_AC_ON", "BOOLEAN / SEAT", "Compressor on/off"],
        ["HVAC_DEFROSTER", "BOOLEAN / WINDOW", "Front/rear defrost"],
        ["HVAC_SEAT_TEMPERATURE", "INT32 / SEAT", "Heated (+) / ventilated (-) seats"],
        ["HVAC_POWER_ON", "BOOLEAN / SEAT", "Master switch; other HVAC props may become unavailable when off"],
    ], [52, 40, 78])
    s += box("brain", "Reading speed only needs CAR_SPEED (dangerous), but writing temperature needs "
                      "CONTROL_CAR_CLIMATE (signature|privileged). Why would Google make <i>writing</i> the climate "
                      "stricter than <i>reading</i> speed?")
    s += box("real", "Remote pre-conditioning (cool the car from your phone before you get in) travels "
                     "phone → OEM cloud → telematics unit (TCU) → vehicle bus → HVAC ECU, often while the head unit is "
                     "asleep. AAOS is not even involved; the TCU and the HVAC ECU are.")
    s += box("app", "Lesson <b>4 HVAC</b>: the fan's rotation speed and airflow particles follow HVAC_FAN_SPEED; the zone "
                    "card fades from blue to red with temperature. On a real AAOS build you'll see the SecurityException "
                    "message because the app is not privileged.")
    s += box("bullets", bullets(["Area ID = seat bitmask for HVAC.", "Read areaIds from config; never hard-code.",
                                 "Writing HVAC is signature|privileged.", "HVAC_POWER_ON gates other HVAC properties."]))
    return s


def ch5():
    s = chapter("5", "The instrument cluster and functional safety", "the screen behind the steering wheel")
    s.append(P("The <b>cluster</b> shows speed, rpm, gear, range and <b>tell-tales</b> (warning lamps: brake, ABS, airbag, "
               "seat belt, engine). Some tell-tales are legally required (UN R121) and safety-relevant."))
    s += stack_diagram(["Cluster UI (safety tell-tales)  |  Android IVI apps", "RTOS / QNX / safety Linux  |  AAOS guest",
                        "Hypervisor (QNX Hypervisor, COQOS, ACRN, Gunyah...)", "One SoC (e.g. Qualcomm SA8295P)"],
                       notes=["two worlds, one screen set", "ASIL-B side vs QM side", "strong isolation",
                              "the 'cockpit domain controller'"],
                       caption="A typical digital cockpit: safety and Android share one chip, isolated by a hypervisor")
    s.append(P("ISO 26262 in one table", "h2"))
    s += table([
        ["Level", "Meaning", "Example"],
        ["QM", "Quality managed, no safety requirement", "Media player, navigation UI"],
        ["ASIL A", "Lowest safety integrity", "Rear lights"],
        ["ASIL B", "Low-medium", "Cluster tell-tales, rear camera display"],
        ["ASIL C", "Medium-high", "Adaptive cruise control"],
        ["ASIL D", "Highest", "Braking, steering, airbag deployment"],
    ], [22, 70, 78])
    s += box("real", "Mercedes MBUX Hyperscreen and many Harman/Visteon digital cockpits run cluster + IVI + passenger "
                     "display on a single chip with a hypervisor. If Android crashes, the speedometer keeps working.")
    s += box("app", "Lesson <b>2 Instrument Cluster</b>: needles use a low-stiffness spring so they sweep smoothly between "
                    "10 Hz samples; the gear letter slides via AnimatedContent; indicators blink with an infinite transition. "
                    "Try shifting to P at 60 km/h: the simulated transmission interlock refuses.")
    s += dumb([
        ("Can I write the cluster in Android?", "AAOS supports a cluster display (ClusterHomeService, cluster activity), "
                                               "often used for navigation maps in the cluster. The legally required "
                                               "tell-tales usually stay on the safety side."),
        ("ASIL vs SIL?", "SIL is from IEC 61508 (general industry). ASIL is the automotive adaptation in ISO 26262."),
    ])
    s += box("bullets", bullets(["Tell-tales are safety-relevant; IVI is QM.", "Hypervisors isolate safety from Android.",
                                 "ASIL A-D + QM (ISO 26262).", "Animate needles with springs for realism."]))
    return s


def ch6():
    s = chapter("6", "Driver distraction and UX restrictions", "eyes on the road, not on your app")
    s += flow_diagram(["VHAL: speed,\ngear, brake", "CarDrivingState\nPARKED/IDLING/MOVING",
                       "car_ux_restrictions\n_map.xml (OEM)", "CarUxRestrictions\nflags + limits", "Your app\nadapts UI"])
    s += table([
        ["Restriction flag", "What your UI must do"],
        ["NO_VIDEO", "Stop/hide video; show a 'not available while driving' message"],
        ["NO_KEYBOARD", "Disable text input; offer voice"],
        ["LIMIT_STRING_LENGTH", "Truncate text to getMaxRestrictedStringLength() (often 120)"],
        ["LIMIT_CONTENT", "Cap items (getMaxCumulativeContentItems) and depth (getMaxContentDepth)"],
        ["NO_SETUP", "No account sign-in / settings flows"],
        ["NO_TEXT_MESSAGE", "Do not show message bodies; read them aloud instead"],
        ["NO_DIALPAD / NO_FILTERING / NO_VOICE_TRANSCRIPTION", "Hide dial pad / list filters / live transcription"],
    ], [70, 100])
    s.append(code("""
val m = car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE)
        as CarUxRestrictionsManager
m.registerListener { r ->
    val noVideo = CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO
    videoAllowed = r.activeRestrictions and noVideo == 0
    maxItems     = r.maxCumulativeContentItems
}
"""))
    s += box("real", "US NHTSA visual-manual guidelines: single glances ≤ 2 s, total task ≤ 12 s. That is where limits "
                     "like '6 list items' and 'no keyboard while moving' come from. Europe uses similar ESoP guidelines.")
    s += box("watch", ["Apps must react to <b>restrictions</b>, not to speed. The OEM may restrict even at 0 km/h "
                       "(IDLING in gear) or allow more on a passenger screen.",
                       "An activity without <font name='Mono'>distractionOptimized=true</font> is covered by the "
                       "ActivityBlockingActivity as soon as the car moves."])
    s += box("app", "Lesson <b>5 Driver Distraction</b>: drag speed above zero; restriction chips bounce in, the video "
                    "gets a lock overlay, the keyboard disables, the list collapses and the message truncates. On AAOS "
                    "with Real Car API selected it listens to the real CarUxRestrictionsManager.")
    s += box("bullets", bullets(["Driving state → OEM map → restrictions.", "Obey flags + limits, not raw speed.",
                                 "Non-DO activities get blocked while moving.", "Templates are DO by design."]))
    return s


def ch7():
    s = chapter("7", "Media apps in the car", "you bring the music, the car brings the UI")
    s += stack_diagram(["Car Media Center / Android Auto media UI", "MediaBrowser + MediaController (client side)",
                        "Your MediaBrowserService (or Media3 MediaLibraryService)", "Your MediaSession + player (ExoPlayer)"],
                       notes=["OEM designs it", "binder IPC", "browse tree", "playback + audio focus"])
    s += table([
        ["Callback / API", "Job"],
        ["onGetRoot(package, uid, hints)", "Validate the caller; return root id + extras (content style hints)"],
        ["onLoadChildren(parentId)", "Return browsable (folders) and playable (tracks) items"],
        ["MediaSession.Callback", "onPlay, onPause, onPlayFromMediaId, onSkipToNext, onPlayFromSearch (voice!)"],
        ["PlaybackState actions", "Declare which buttons the car shows; set position + speed for the seek bar"],
        ["Content style hints", "CONTENT_STYLE_BROWSABLE_HINT / PLAYABLE_HINT: grid vs list"],
    ], [55, 115])
    s += box("real", "Spotify, YouTube Music, Audible, Pocket Casts all look 'native' in Volvo, GM, Renault because the "
                     "OEM's media template draws them. Steering-wheel next/prev buttons are routed as key events to the "
                     "active MediaSession.")
    s += box("app", "Lesson <b>6 Media</b> connects to this app's own CarMediaBrowserService with MediaBrowserCompat "
                    "(exactly like the car). Tap a track: the equalizer animates, the title slides in and the seek bar "
                    "is extrapolated from PlaybackState (position + update time × speed).")
    s += dumb([
        ("Media3 or MediaBrowserServiceCompat?", "New apps should use Media3 (MediaLibraryService + MediaSession + "
                                                  "ExoPlayer). The concepts are the same; this app uses the compat API "
                                                  "because it maps 1:1 to what the car calls."),
        ("How does voice work ('Play jazz on Spotify')?", "The assistant calls onPlayFromSearch(query, extras). Support it: "
                                                          "cars rely on voice while driving."),
    ])
    s += box("bullets", bullets(["MediaBrowserService = tree; MediaSession = control.", "Validate callers in onGetRoot.",
                                 "Actions decide which buttons appear.", "Support search for voice."]))
    return s


def ch8():
    s = chapter("8", "Car audio: contexts, zones and focus", "who gets the speakers?")
    s += flow_diagram(["AudioAttributes\nusage", "Audio context\n(MUSIC, NAV...)", "Volume group", "Output bus\n(device)",
                       "Amplifier / DSP", "Speakers"], box_h=32)
    s.append(P("CarAudioService arbitrates focus using an <b>interaction matrix</b>:"))
    s += table([
        ["Holder ↓ / Requester →", "Music", "Navigation", "Call", "Voice assistant"],
        ["Music", "exclusive", "concurrent (duck)", "exclusive", "exclusive"],
        ["Navigation", "concurrent", "exclusive", "exclusive", "exclusive"],
        ["Call", "reject", "concurrent (duck)", "exclusive", "reject"],
        ["Voice assistant", "reject", "concurrent", "exclusive", "exclusive"],
    ], [40, 30, 34, 30, 36])
    s.append(P("(Simplified from the AOSP defaults; OEMs customise it.)", "small"))
    s += box("real", "Maps says 'turn left' → music ducks. A call comes in → music pauses. Seat-belt chime → always "
                     "plays, often on a separate safety path. BMW 7 Series rear seats can listen to a different stream "
                     "on headphones: a second <b>audio zone</b>.")
    s += box("watch", "In many cars ducking is done by the <b>external amplifier/DSP</b>, not by Android's mixer. That "
                      "is why AAOS uses fixed-volume output buses and lets the hardware handle gain.")
    s += box("app", "Lesson <b>7 Car Audio</b>: mixer bars animate to show exclusive (music → 0), concurrent (music ducks "
                    "to 30 %) and reject. It also makes a real AudioFocusRequest so you can inspect "
                    "<font name='Mono'>adb shell dumpsys car_service</font> / <font name='Mono'>dumpsys audio</font>.")
    s += box("bullets", bullets(["Usage → context → volume group → bus.", "Exclusive / concurrent / reject.",
                                 "Zones = independent audio per occupant.", "Always request focus with the right usage."]))
    return s


def ch9():
    s = chapter("9", "Vehicle networks and diagnostics", "CAN, LIN, FlexRay, Ethernet, UDS")
    s += table([
        ["Bus", "Speed", "Payload", "Used for"],
        ["LIN", "≤ 20 kbps", "8 bytes", "Cheap sensors/actuators: window switches, mirrors, seat motors"],
        ["CAN (HS)", "125 kbps - 1 Mbps (500 k typical)", "8 bytes", "Powertrain, body, chassis messages"],
        ["CAN FD", "up to ~5-8 Mbps data phase", "64 bytes", "More data + room for SecOC MACs"],
        ["CAN XL", "up to ~20 Mbps", "2048 bytes", "Bridge between CAN and Ethernet worlds"],
        ["FlexRay", "10 Mbps, time-triggered", "254 bytes", "Deterministic chassis (older premium cars)"],
        ["Automotive Ethernet", "100BASE-T1 / 1000BASE-T1 / multi-gig", "large", "Cameras, ADAS, backbone, OTA, DoIP"],
    ], [28, 42, 22, 78])
    s += bus_diagram([("Brake", "0x050"), ("Engine", "0x0C0"), ("Speed", "0x0F0"), ("Body", "0x2A0"), ("IVI", "0x5F0")],
                     caption="Lower ID = higher priority. The brake always wins.")
    s.append(P("Arbitration, bit by bit", "h2"))
    s.append(P("All nodes start sending their ID at the same time, MSB first. The bus is a <b>wired-AND</b>: a dominant 0 "
               "overrides a recessive 1. A node that sends 1 but reads 0 knows it lost and becomes a receiver. "
               "The winner never notices the collision, so no bandwidth is wasted."))
    s += bits_diagram([("brake 0x050", "000 0101 0000", 1), ("speed 0x0F0", "000 1111 0000", 1)],
                      caption="They differ at bit 3 (from MSB): brake sends 0, speed sends 1 → speed backs off")
    s.append(P("Signals and DBC files", "h2"))
    s.append(P("A frame's 8 bytes carry several <b>signals</b>. The DBC file (communication matrix) says: signal "
               "VehicleSpeed is in frame 0x0F0, start bit 0, length 16, little-endian, factor 0.01, offset 0, unit km/h. "
               "Physical value = raw × factor + offset."))
    s.append(code("""
raw   = 0x3039          // bytes 39 30 on the wire (little-endian)
speed = 12345 * 0.01    // = 123.45 km/h     (see CanBus.encodeSpeed / decodeSpeed)
"""))
    s.append(P("Diagnostics", "h2"))
    s += table([
        ["Standard", "What", "Example"],
        ["OBD-II (SAE J1979 / ISO 15031)", "Legally required emissions diagnostics via the OBD port",
         "Mode 01 PID 0x0D = vehicle speed; DTC P0300 misfire"],
        ["UDS (ISO 14229)", "Unified Diagnostic Services for all ECUs",
         "0x10 session control, 0x22 read DID, 0x27 security access, 0x29 authentication, 0x2E write DID, "
         "0x31 routine, 0x34/0x36/0x37 flash download, 0x19 read DTCs"],
        ["ISO-TP (ISO 15765-2)", "Transport layer to send > 8 bytes over CAN", "First frame + consecutive frames"],
        ["DoIP (ISO 13400)", "Diagnostics over IP (Ethernet)", "Fast ECU flashing at the factory/OTA"],
    ], [42, 58, 70])
    s += box("real", "A cheap Bluetooth OBD-II dongle + the Torque app reads live engine data from almost any car made "
                     "after 2001 (EU) / 1996 (US). Workshops flash ECUs using UDS 0x34/0x36/0x37 after unlocking with 0x27.")
    s += box("app", "Lesson <b>8 CAN Bus</b>: a frame travels from the Speed ECU in both directions along CAN_H/CAN_L; the "
                    "candump shows the live encoded speed. In the arbitration demo pick ECUs and watch the IDs revealed "
                    "bit by bit; losers turn grey at the bit where they lost.")
    s += box("pencil", ["(e) Frames 0x2A0 and 0x2A4 start together. Which wins, and at which bit?",
                        "(f) Raw speed bytes on the wire are E8 03. What is the speed with factor 0.01?"])
    s += box("bullets", bullets(["LIN < CAN < CAN FD < Ethernet.", "Lowest ID wins non-destructively.",
                                 "DBC maps bits to physical signals.", "UDS services: 0x10, 0x22, 0x27, 0x2E, 0x19, 0x34-37.",
                                 "CAN has no authentication → SecOC (book 03)."]))
    return s


def chapters():
    return ch1() + ch2() + ch3() + ch4() + ch5() + ch6() + ch7() + ch8() + ch9()
