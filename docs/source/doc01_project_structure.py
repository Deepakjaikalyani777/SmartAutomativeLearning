from hf import *

TREE = """
SampleAutomativeApp/
├── settings.gradle.kts           project name + repositories
├── build.gradle.kts              plugin versions (applied in :app)
├── gradle/libs.versions.toml     version catalog: AGP, Kotlin, Compose, car-app...
├── app/
│   ├── build.gradle.kts          useLibrary("android.car"), minSdk 29, Compose
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     features, car permissions, services
│       │   ├── res/xml/automotive_app_desc.xml   "media" + "template"
│       │   └── java/com/deepak/automotive/
│       │       ├── AutomotiveApp.kt    Application: creates VehicleRepository
│       │       ├── MainActivity.kt     runtime car permissions + Compose root
│       │       ├── core/               PURE logic, no UI
│       │       │   ├── car/            CarAvailability (is this AAOS?)
│       │       │   ├── vehicle/        VehicleState, DataSource(s), physics, VHAL IDs
│       │       │   ├── ux/             UX restrictions (real + simulated)
│       │       │   ├── can/            CAN frame, arbitration, DBC-style signals
│       │       │   ├── security/       SecOC (HMAC), SecureVault (Keystore)
│       │       │   ├── power/          CPMS-like power state machine
│       │       │   ├── ota/            A/B OTA state machine
│       │       │   └── audio/          focus matrix + real AudioFocusRequest
│       │       ├── media/              MediaBrowserService, catalog, client
│       │       ├── carapp/             Car App Library service (charging POI)
│       │       └── ui/
│       │           ├── VehicleViewModel.kt
│       │           ├── theme/          dark car theme, big type
│       │           ├── components/     LessonScaffold, Gauge, CarButton, CodeBlock
│       │           ├── navigation/     Lessons registry + NavHost
│       │           └── screens/        15 animated lesson screens + Home
│       └── test/                       17 JVM unit tests
└── docs/                               these PDFs + their Python sources
"""


def story():
    s = cover("Project Structure", "A guided tour of the Automotive Academy codebase",
              "\"Show me your folder structure and I'll tell you how you think.\" (every tech lead ever)",
              ["core/ has no Android UI: it's the brain", "ui/ only renders state", "one seam: VehicleDataSource"])
    s += toc()

    s += chapter("1", "The big picture", "four rings, one direction of dependencies")
    s.append(P("The app is small (about 4,100 lines of Kotlin) but it is organised like a production IVI app. "
               "Dependencies point <b>inwards</b>: UI knows about core, core never knows about UI."))
    s += stack_diagram(
        ["ui/screens + ui/components  (Jetpack Compose)", "ui/VehicleViewModel  (MVVM, StateFlow)",
         "core/vehicle/VehicleRepository  (single source of truth)",
         "VehicleDataSource  (interface = the seam)",
         "SimulatedVehicleDataSource   |   CarApiVehicleDataSource",
         "VehiclePhysics (pure)   |   android.car CarPropertyManager"],
        notes=["draws pixels, animates", "survives config changes", "picks sim or real car",
               "swap implementations here", "fake car vs real car", "no Android needed / real VHAL"],
        caption="Data flows up as immutable VehicleState; commands flow down as method calls")
    s += box("brain", "Why do you think the screens never import <font name='Mono'>android.car.*</font> directly? "
                      "Think about (1) running on a phone, (2) unit testing, (3) a CarService crash.")
    s.append(CondPageBreak(150 * mm))
    s.append(P("Folder tree", "h2"))
    s.append(code(TREE))

    s += chapter("2", "Gradle & the manifest", "where the car-specific magic is declared")
    s.append(P("<b>app/build.gradle.kts</b> has one line you will not see in phone apps:"))
    s.append(code('android {\n    compileSdk = 36\n    // compiles against platforms/android-36/optional/android.car.jar\n    useLibrary("android.car")\n}'))
    s.append(P("That jar is a <i>stub</i>: at runtime the real classes are provided by the AAOS system image. On a phone "
               "they don't exist, which is why the manifest says <font name='Mono'>&lt;uses-library android:name=\"android.car\" "
               "android:required=\"false\"/&gt;</font> and why every Car API call is guarded by "
               "<font name='Mono'>CarAvailability.isAutomotive()</font>."))
    s += table([
        ["Manifest element", "Why it is there"],
        ["uses-feature android.hardware.type.automotive (required=false)",
         "Marks the app as AAOS-capable while still installing on phones for learning."],
        ["uses-feature wifi / portrait / landscape required=false",
         "Play Store filters AAOS apps that require hardware a head unit may lack."],
        ["uses-permission android.car.permission.*",
         "CAR_SPEED & CAR_ENERGY are dangerous (runtime prompt); CAR_INFO/POWERTRAIN normal; CONTROL_CAR_CLIMATE is "
         "signature|privileged (we request it to demonstrate the SecurityException)."],
        ["meta-data com.google.android.gms.car.application → automotive_app_desc.xml",
         "Declares we provide <b>media</b> and <b>template</b> experiences."],
        ["activity MainActivity + meta-data distractionOptimized=true",
         "Allows the activity while driving; honoured for system apps. It is the learning playground."],
        ["service CarMediaBrowserService (MediaBrowserService action)",
         "Lets the car's Media Center and Android Auto browse & control our content."],
        ["service ChargingStationCarAppService (CarAppService, category CHARGING)",
         "Car App Library entry point rendered by the host as templates."],
    ], [70, 100])
    s += box("watch", "Forgetting <font name='Mono'>android:required=\"false\"</font> on <font name='Mono'>uses-library "
                      "android.car</font> makes the APK refuse to install on phones with "
                      "INSTALL_FAILED_MISSING_SHARED_LIBRARY. On AAOS it always exists.")

    s += chapter("3", "core/vehicle: the heart", "a VehicleState goes in, a VehicleState comes out")
    s += table([
        ["File", "Responsibility", "Pattern"],
        ["VehicleState.kt", "Immutable snapshot: speed, rpm, gear, fuel, EV SoC, HVAC, signals, source, lastError",
         "Value object / UDF state"],
        ["VehicleDataSource.kt", "Interface: state StateFlow + commands (setHvacTemperature, shift, setThrottle...)",
         "Strategy / Port"],
        ["SimulatedVehicleDataSource.kt", "10 Hz coroutine loop that applies VehiclePhysics; safety interlocks",
         "Adapter (fake)"],
        ["VehiclePhysics.kt", "Pure function step(state, throttle, brake, dt) → new state", "Pure function, testable"],
        ["CarApiVehicleDataSource.kt", "Car.createCar → CarPropertyManager.registerCallback / setProperty, area lookup",
         "Adapter (real)"],
        ["VehicleRepository.kt", "Owns both sources; flatMapLatest to expose the active one as a single StateFlow",
         "Repository"],
        ["VhalPropertyId.kt", "Decodes 32-bit property IDs; catalog with change mode, access, permission",
         "Domain model"],
    ], [42, 88, 40])
    s.append(P("How a speed change reaches the gauge (real car)", "h3"))
    s += flow_diagram(["ECU\nwheel speed", "CAN frame\n0x0F0", "VHAL\nPERF_VEHICLE_SPEED", "CarService",
                       "CarApiVehicle\nDataSource", "Repository\nStateFlow", "Gauge\nspring anim"],
                      labels=["bus", "decode", "AIDL", "binder cb", "update{}", "collect"])
    s.append(code("""
// CarApiVehicleDataSource.onPropertyChanged (simplified)
// VHAL speed is m/s, the UI wants km/h
VehiclePropertyIds.PERF_VEHICLE_SPEED -> s.copy(speedKmh = (v as Float) * 3.6f)
"""))
    s += dumb([
        ("Why StateFlow and not LiveData?",
         "StateFlow is Kotlin-first, works in pure JVM tests, supports operators like flatMapLatest, and Compose "
         "collects it with collectAsStateWithLifecycle(). LiveData would tie core/ to Android."),
        ("Why does CarApiVehicleDataSource re-subscribe inside the lifecycle listener?",
         "Because CarService can crash and restart. createCar(..., WAIT_FOREVER, listener) calls you again with "
         "ready=true and <b>all old managers are dead</b>. Caching a manager across restarts is a classic AAOS bug."),
        ("Why is speed multiplied by 3.6?",
         "VHAL reports PERF_VEHICLE_SPEED in metres per second (SI units). Always check the unit in "
         "VehiclePropertyIds docs; an interviewer may ask exactly this."),
    ])

    s += chapter("4", "The other core packages")
    s += table([
        ["Package", "What to read", "Concept it teaches"],
        ["core/ux", "UxRestrictionsState + CarUxRestrictionsSource", "Driver distraction; restrictions come from CarService"],
        ["core/can", "CanFrame, CanBus.arbitrate, encodeSpeed/decodeSpeed", "Priority by ID, wired-AND bus, DBC scaling"],
        ["core/security", "SecOc (HMAC-SHA256 truncated MAC + freshness), SecureVault (AES-GCM in AndroidKeyStore)",
         "Message authentication, replay protection, hardware-backed keys"],
        ["core/power", "PowerStateMachine.next(state, event)", "CPMS, Garage Mode, suspend-to-RAM"],
        ["core/ota", "AbOtaUpdater.advance(state, version, bootOk)", "A/B slots, verify, rollback"],
        ["core/audio", "FocusMatrix.interaction + AudioFocusRequester", "Exclusive / concurrent (duck) / reject"],
        ["core/car", "CarAvailability.isAutomotive", "FEATURE_AUTOMOTIVE check before touching android.car"],
    ], [28, 72, 70])
    s += box("interview", "Notice that power, OTA, CAN and SecOC logic are <b>pure Kotlin functions</b> with unit tests. "
                          "In interviews at Tier-1s, saying \"I keep state machines pure and test them on the JVM, then "
                          "wire them to Android at the edges\" signals senior-level thinking.")

    s += chapter("5", "media/ and carapp/: plugging into the car's own UI")
    s += stack_diagram(["Car Media Center / Android Auto (OEM UI)", "MediaBrowser (client)  ·  MediaController",
                        "CarMediaBrowserService: onGetRoot / onLoadChildren", "MediaSessionCompat + Callback",
                        "MediaCatalog (in-memory tree)"],
                       notes=["we never draw this", "MediaClient.kt copies it", "validate caller here",
                              "play / pause / skip", "replace with your backend"])
    s.append(P("<b>MediaClient.kt</b> connects to our own service exactly like the car does. The Media lesson screen "
               "is therefore a working <i>test harness</i> for the service."))
    s.append(P("<b>ChargingStationCarAppService.kt</b>: CarAppService → Session → StationListScreen (ListTemplate) → "
               "StationDetailScreen (PaneTemplate) with a Navigate action that fires "
               "<font name='Mono'>CarContext.ACTION_NAVIGATE</font> with a geo: URI."))
    s += box("watch", "HostValidator.ALLOW_ALL_HOSTS_VALIDATOR is used <b>only in debug builds</b>. In release the "
                      "service restricts hosts to an allowlist of package names + signing certificates. Shipping "
                      "ALLOW_ALL is a real security finding.")

    s += chapter("6", "ui/: Compose, lessons and animations")
    s += table([
        ["File", "Role"],
        ["navigation/Lessons.kt", "Registry of 15 lessons: route, title, icon, colour, Head First notes"],
        ["navigation/AppNavHost.kt", "NavHost with slide/fade transitions; wraps every lesson in LessonScaffold"],
        ["components/LessonScaffold.kt", "Header + collapsible notes panel (AnimatedVisibility) + scrollable demo"],
        ["components/Gauge.kt", "Canvas gauge; needle uses spring() so it sweeps like a stepper motor"],
        ["components/CarButton.kt", "76dp-friendly buttons with animated selection colour"],
        ["screens/*", "One file per lesson; each observes VehicleViewModel.state"],
    ], [55, 115])
    s.append(P("Animation toolbox used (learn these names for Compose interviews)", "h3"))
    s += table([
        ["API", "Used in", "Why"],
        ["animateFloatAsState + spring", "Gauge, fuel bar, temperature", "Smooth value changes between samples"],
        ["rememberInfiniteTransition", "Home road, turn signals, equalizer, ultrasonic waves", "Looping ambient motion"],
        ["AnimatedContent", "Gear letter, track title, OTA phase, Car App screens", "Enter/exit when content changes"],
        ["AnimatedVisibility", "Notes panel, video lock overlay, errors", "Show/hide with expand/fade/scale"],
        ["Animatable", "Home tile stagger, CAN frame travel, VHAL row flash", "Imperative, coroutine-driven"],
        ["withInfiniteAnimationFrameMillis", "HVAC fan", "Frame-accurate physics (rotation ∝ fan speed)"],
        ["animateColorAsState / lerp", "Zones, slots, restrictions", "Colour communicates state"],
        ["animateContentSize", "UX restricted list/text", "Layout grows/shrinks smoothly"],
    ], [50, 70, 50])

    s += chapter("7", "Tests")
    s += table([
        ["Test class", "What it proves"],
        ["CanBusTest", "Lowest ID wins; speed signal round-trips; >8 bytes rejected"],
        ["SecOcTest", "Fresh authentic frame accepted; replay, forged MAC and tampered payload rejected"],
        ["StateMachinesTest", "Garage Mode → suspend → fast resume; invalid event ignored; OTA success & rollback"],
        ["VehicleTest", "VHAL ID decoding; no motion in Park; acceleration in Drive; UX restrictions; focus matrix"],
    ], [45, 125])
    s += box("interview", "Next level (mention it!): instrumentation tests on an AAOS emulator using "
                          "<font name='Mono'>adb shell cmd car_service inject-vhal-event 0x11600207 20</font> to fake "
                          "speed, Compose UI tests for restricted states, and CTS/ CTS-Verifier for OEM builds.")

    s += chapter("8", "Exercises: extend the app", "the best way to own a codebase is to change it")
    s += box("pencil", bullets([
        "<b>Add a property</b>: add TIRE_PRESSURE (area WHEEL) to VhalCatalog and show it in the VHAL screen. "
        "Hint: decode its ID first; area type should be WHEEL.",
        "<b>Seat heating</b>: add HVAC_SEAT_TEMPERATURE to VehicleDataSource with levels -3..3 and a heated-seat animation.",
        "<b>Night mode</b>: switch AutomotiveTheme to a dimmer palette when state.nightMode is true.",
        "<b>New restriction</b>: hide the Media browse tree beyond depth 3 when LIMIT_CONTENT is active.",
        "<b>Car App Library</b>: add a MapWithContentTemplate screen (requires car API level 7).",
        "<b>CAN FD</b>: allow 64-byte frames when a flag is set and adjust the validation + tests.",
    ]))
    s.append(P("Answers / hints", "h3"))
    s += bullets([
        "TIRE_PRESSURE is 0x17600309: group SYSTEM (1), area WHEEL (7), type FLOAT (0x60), id 0x0309.",
        "Seat temperature is an INT32 SEAT-area property; negative values mean cooling.",
        "Collect nightMode in AutomotiveTheme and swap colour schemes; AAOS also has UiModeManager night mode.",
        "Use ux.maxContentDepth from UxRestrictionsState.",
    ])
    s += box("bullets", bullets([
        "core/ = pure logic and Android adapters; ui/ = Compose only.",
        "VehicleDataSource is the seam between simulator and real VHAL.",
        "useLibrary(\"android.car\") + uses-library required=false + runtime feature check.",
        "Media and templates: the car draws the UI; we provide data and callbacks.",
        "State machines are pure and unit tested.",
    ]))
    return s


if __name__ == "__main__":
    build("../01_Project_Structure.pdf", "01 Project Structure - Automotive Academy", "Codebase tour", story())
