from hf import *
from reportlab.lib.styles import ParagraphStyle

RETRIEVED = "27 Sep 2026"

# ---- Part A: questions reported publicly, with company, date and source (retrieved RETRIEVED).
# "Date" = the date shown on the source page for that interview report / article.
SOURCED = [
    ("HARMAN", "Android Developer", "11 Jul 2026", "AmbitionBox",
     "ambitionbox.com/interviews/harman-interview-questions/android-developer",
     ["What are the key components of the MVVM architecture in Android, and how do they interact?",
      "Describe the Android application lifecycle. How do activities transition between states?",
      "What are SOLID principles?", "What is the Android Framework?", "Write a Builder class.",
      "Explain the purpose of the Room persistence library.", "'More in-depth on Android internals'"]),
    ("HARMAN", "Embedded Automotive Developer", "12 Jul 2025", "AmbitionBox",
     "ambitionbox.com/interviews/harman-interview-questions/embedded-automotive-developer",
     ["Smart pointers, bitwise operators", "Multithreading, STL"]),
    ("HARMAN", "Android Developer (Round 1)", "30 Dec 2024", "AmbitionBox",
     "ambitionbox.com/interviews/harman-interview-questions/android-developer",
     ["Explain activity lifecycle", "Explain MVVM architecture"]),
    ("HARMAN", "Android Developer (Round 2)", "7 Jan 2023", "AmbitionBox",
     "ambitionbox.com/interviews/harman-interview-questions/android-developer",
     ["Explain Coroutines, RxJava", "Kotlin: lateinit, val, var, extension function, data class"]),
    ("HARMAN International", "Android role (6 yrs exp.)", "23 Aug 2019", "GeeksforGeeks",
     "geeksforgeeks.org/interview-experiences/harman-international-interview-experience-for-android-role/",
     ["Which lifecycle methods are called when the system kills the process?", "What is a Handler and how does it work internally?",
      "Is a bound service synchronous? Have you used AIDL?", "How does a memory leak happen?",
      "How is ViewModel implemented internally?", "Difference between IntentService and Service; bound vs started service",
      "SharedPreferences apply vs commit", "Activity launch modes"]),
    ("Bosch", "Android Developer", "25 Nov 2025", "Medium (Mobile App Developer)",
     "medium.com/@mobileappdeveloper.koti/bosch-android-developer-interview-experience-d6d15cfe207c",
     ["Why does Bosch prefer Kotlin over Java?", "Coroutines vs Threads: when do you choose which?"]),
    ("Mercedes-Benz R&D India", "Android Developer", "29 Jun 2022", "Medium (Piyush Sinha)",
     "medium.com/@piyush.sinha12345/mercedes-benz-android-developer-interview-experience-9b727087e0a9",
     ["Advantage of MVVM over MVP", "What is a foreground service? Foreground vs background services",
      "How does ViewModel work internally?", "Internal working of LiveData", "Coroutines and their scopes",
      "Fragment lifecycle for add vs replace", "Implement the Observer / Builder pattern"]),
    ("L&T Technology Services", "Android Framework Developer", "5 Mar 2025", "AmbitionBox",
     "ambitionbox.com/interviews/l-and-t-technology-services-interview-questions/android-framework-developer",
     ["Explain Android architecture in brief", "Explain the Android boot process"]),
    ("Tata Elxsi", "Android Developer", "11 Nov 2024", "AmbitionBox",
     "ambitionbox.com/interviews/tata-elxsi-interview-questions/android-developer",
     ["Java program on strings, threads", "Multithreading, collections", "Git commands, Linux commands"]),
    ("KPIT Technologies", "Android Developer", "23 Mar 2023", "AmbitionBox",
     "ambitionbox.com/interviews/kpit-technologies-interview-questions/android-developer",
     ["Difference between interface and abstract class", "What is a singleton class?"]),
    ("Visteon", "Embedded / software (GfG write-up)", "7 May 2024", "GeeksforGeeks",
     "geeksforgeeks.org/interview-experiences/visteon-corporation-interview-experience/",
     ["Setting and clearing a bit (optimised)", "Difference between C and embedded C", "Structure vs union",
      "Microprocessor vs microcontroller; ARM vs x86", "Compilation process in C", "Find a loop in a linked list"]),
    ("Visteon", "Software Developer", "19 Feb 2024", "AmbitionBox",
     "ambitionbox.com/interviews/visteon-interview-questions/android-developer",
     ["C and security, protocol, middleware"]),
]

# ---- answers for Part A questions (short, interview-ready)
ANSWERS_A = [
    ("MVVM components and interaction",
     "View (Activity/Fragment/Composable) observes state from the ViewModel and forwards user events. ViewModel holds "
     "UI state (StateFlow/LiveData), survives configuration changes, calls the Model. Model = repositories + data "
     "sources (Room, network, <b>VHAL</b> in a car). Data flows up as state, events flow down: unidirectional data flow. "
     "In this project: HvacScreen → VehicleViewModel → VehicleRepository → CarApiVehicleDataSource → CarPropertyManager."),
    ("Activity lifecycle",
     "onCreate → onStart → onResume (interactive) → onPause → onStop → onDestroy; onRestart when coming back from "
     "stopped. <b>Car twist:</b> AAOS head units change displays/users, and ActivityBlockingActivity covers non-DO "
     "activities while driving, which pauses your activity. Save state in onStop / ViewModel + SavedStateHandle."),
    ("Which callbacks run when the system kills the process?",
     "None are guaranteed. onSaveInstanceState runs after onStop on API 28+ (before onStop on older versions); a "
     "low-memory kill of a stopped process calls nothing. So persist important data early (onStop) and restore from "
     "SavedStateHandle / disk."),
    ("SOLID",
     "S: single responsibility (VehiclePhysics only does physics). O: open for extension, closed for modification "
     "(add a new VehicleDataSource without editing screens). L: Liskov: any VehicleDataSource can replace another. "
     "I: interface segregation: small interfaces. D: dependency inversion: screens depend on the VehicleDataSource "
     "abstraction, not on CarPropertyManager."),
    ("What is the Android Framework? / Android architecture",
     "Layers: Linux kernel → HAL (hardware interfaces; for cars the Vehicle HAL) → native libraries + ART → Java "
     "framework (ActivityManager, PackageManager, WindowManager... and in AAOS the Car API + CarService) → apps. Apps "
     "call framework APIs; system services run in system_server and talk to apps over Binder."),
    ("Android boot process",
     "Boot ROM → bootloader (verified boot) → kernel → init (reads init.rc, starts ueventd, servicemanager, HALs, "
     "surfaceflinger) → Zygote (preloads classes) → system_server (starts system services) → CarService on AAOS → "
     "BOOT_COMPLETED → launcher. On AAOS, EVS and early audio start before Zygote so the rear camera works quickly."),
    ("Builder class",
     "Use when an object has many optional parameters. In Kotlin, named/default args often replace it, but Java "
     "interop and Android APIs (AudioFocusRequest.Builder, Row.Builder in Car App Library) use it."),
    ("Room",
     "SQLite abstraction: @Entity, @Dao, @Database; compile-time SQL verification; returns Flow for observable "
     "queries; migrations. In a car: store recent destinations or media history <b>per Android user</b>."),
    ("Coroutines vs Threads; RxJava",
     "Threads are OS resources (≈1 MB stack), expensive to create and block. Coroutines are lightweight, suspend "
     "instead of blocking, and use structured concurrency (scopes cancel children). Use threads/dispatchers underneath "
     "(Dispatchers.IO/Default); use coroutines for app logic. RxJava is a reactive stream library; Flow is the "
     "Kotlin-native equivalent. This app's simulator is a coroutine loop; CarService callbacks are bridged into StateFlow."),
    ("Coroutine scopes",
     "GlobalScope (avoid), viewModelScope (cancelled in onCleared), lifecycleScope, custom CoroutineScope(SupervisorJob()+"
     "Dispatcher) for repositories (see VehicleRepository). SupervisorJob: one child failing doesn't cancel siblings."),
    ("Kotlin: lateinit, val, var, extension functions, data class",
     "val = read-only reference; var = mutable; lateinit var = non-null var initialised later (not for primitives; "
     "check ::x.isInitialized). Extension functions add functions to a type without inheritance (resolved statically). "
     "data class generates equals/hashCode/toString/copy/componentN (VehicleState uses copy for immutable updates)."),
    ("Why Kotlin over Java?",
     "Null safety, coroutines, concise syntax, data/sealed classes, extension functions, first-class Compose, "
     "full Java interop, Google's recommended language. For car apps: less boilerplate in state machines and "
     "safer null handling of vehicle properties that may be unavailable."),
    ("Handler internals",
     "A Looper runs a MessageQueue on a thread; Handler posts Messages/Runnables to that queue and handles them on the "
     "Looper's thread. The main thread has a Looper by default. Car API callbacks can be delivered on a Handler you pass "
     "to Car.createCar."),
    ("Bound service synchronous? AIDL?",
     "Binding is asynchronous (onServiceConnected later). Calls through an AIDL interface are synchronous by default "
     "(the caller blocks) unless declared oneway. The whole Car API is AIDL over Binder to CarService, and VHAL itself "
     "is an AIDL HAL."),
    ("Memory leaks",
     "Holding a reference to an Activity/Context beyond its life: static fields, singletons, inner classes/anonymous "
     "listeners, unregistered callbacks. <b>Car example:</b> registering a CarPropertyEventCallback in an Activity and "
     "never calling unregisterCallback keeps the Activity alive."),
    ("ViewModel internals",
     "ViewModelStoreOwner (Activity/Fragment/NavBackStackEntry) owns a ViewModelStore (a map). ViewModelProvider "
     "creates or returns the instance by key. The store is retained across configuration changes (via "
     "NonConfigurationInstances) and cleared (onCleared) when the owner is finished."),
    ("LiveData internals",
     "Holds a value + version; observers are wrapped with a LifecycleBoundObserver and only receive updates when "
     "STARTED/RESUMED; they're removed on DESTROYED. setValue on main thread, postValue from any thread."),
    ("IntentService vs Service; bound vs started; foreground",
     "IntentService (deprecated) ran work on a worker thread and stopped itself; use WorkManager/coroutines. Started "
     "service runs until stopSelf; bound service lives while clients are bound. Foreground service shows a notification "
     "and has a type (e.g. mediaPlayback), which a car media app uses while playing."),
    ("apply vs commit; launch modes",
     "commit writes synchronously and returns a boolean; apply writes async. Launch modes: standard, singleTop, "
     "singleTask, singleInstance (+ singleInstancePerTask)."),
    ("MVVM vs MVP",
     "MVP presenter holds a View reference (interfaces, manual lifecycle handling, leak risk). MVVM ViewModel exposes "
     "observable state and knows nothing about the View; lifecycle-aware and easier to test."),
    ("Interface vs abstract class; singleton",
     "Interface: no state, multiple inheritance of type, default methods allowed. Abstract class: can hold state and "
     "constructors, single inheritance. Singleton in Kotlin: object declaration (thread-safe lazy init by the JVM)."),
    ("C / embedded (Visteon, Harman embedded)",
     "Set bit: x |= (1u &lt;&lt; n); clear: x &amp;= ~(1u &lt;&lt; n); toggle: x ^= (1u &lt;&lt; n). struct = members "
     "have separate memory (size ≥ sum, with padding); union = members share memory (size = largest). Embedded C adds "
     "fixed-point, memory-mapped I/O, volatile, ISR constraints. Compilation: preprocess → compile → assemble → link. "
     "Loop in a list: Floyd's slow/fast pointers. Smart pointers: unique_ptr (sole ownership), shared_ptr (ref count), "
     "weak_ptr (breaks cycles)."),
]


def qa(q, a, level=None):
    tag = f"  <font name='Sans' size='8' color='#6B7280'>[{level}]</font>" if level else ""
    return [KeepTogether([P("<b>Q.</b> " + q + tag, "q"), P(a, "a")])]


# ---- Part B: AAOS topic bank (not tied to a specific company/date)
BANK = {
    "Platform & architecture": [
        ("What is AAOS and how does it differ from Android Auto?",
         "AAOS is Android running natively on the head unit with access to vehicle data via VHAL; Android Auto runs on "
         "the phone and projects to the car display. Same media and Car App Library APIs work on both.", "basic"),
        ("Walk me through the AAOS stack.",
         "Apps → Car API managers (android.car) → CarService (binder, permissions, policies) → VHAL (AIDL IVehicle) → "
         "vehicle MCU/gateway → CAN/LIN/Ethernet → ECUs. Mention CarService restart handling.", "basic"),
        ("What is CarService?",
         "The privileged system component that implements the Car API: property, UX restrictions, audio, power, "
         "occupant zones, input, watchdog. It sits between apps and VHAL, enforces car permissions and multiplexes "
         "subscriptions.", "basic"),
        ("What happens when CarService crashes?",
         "Apps using Car.createCar with a lifecycle listener get ready=false; managers are invalid. When it restarts "
         "the listener fires with ready=true; re-fetch managers and re-register callbacks.", "intermediate"),
        ("What is GAS?",
         "Google Automotive Services: Google Maps, Assistant, Play Store and other Google apps licensed to OEMs "
         "('Google built-in'). AAOS can ship without GAS.", "basic"),
        ("Why does AAOS have a headless system user?",
         "User 0 runs system services only; drivers use secondary users so profiles can switch (driver change, guest) "
         "without rebooting, and data stays separated.", "intermediate"),
        ("How is the AAOS boot optimised for cars?",
         "Early native services (EVS camera, early audio chimes), suspend-to-RAM instead of cold boot, deferred "
         "non-critical services, pre-created users, and boot-time budgets from the OEM.", "advanced"),
    ],
    "VHAL & properties": [
        ("What is a VHAL property? Describe its attributes.",
         "32-bit ID (group|area|type|id), value type, area type & area IDs, access (READ/WRITE/READ_WRITE), change mode "
         "(STATIC/ON_CHANGE/CONTINUOUS), min/max sample rate, required permissions.", "basic"),
        ("Decode 0x11600207.", "SYSTEM (0x1), GLOBAL (0x1), FLOAT (0x60), id 0x0207 → PERF_VEHICLE_SPEED in m/s.", "intermediate"),
        ("ON_CHANGE vs CONTINUOUS?",
         "ON_CHANGE fires only when the value changes (gear, door). CONTINUOUS is sampled at a requested rate within "
         "config limits (speed, rpm).", "basic"),
        ("How do you add an OEM-specific property?",
         "Define a VENDOR group ID (0x2...), implement it in the VHAL, add SELinux/permission (often "
         "CAR_VENDOR_EXTENSION or custom), expose to privileged apps; document in the OEM's property list.", "advanced"),
        ("What are area IDs? Give an HVAC example.",
         "For area properties the same ID exists per area. HVAC_TEMPERATURE_SET with area ROW_1_LEFT (0x1) and "
         "ROW_1_RIGHT (0x4), or OEM groupings like 0x31. Read areaIds from CarPropertyConfig.", "intermediate"),
        ("AIDL vs HIDL VHAL?",
         "HIDL (2.0) was used until Android 12; Android 13 introduced the stable AIDL VHAL (IVehicle) with better "
         "versioning and batch APIs. New work targets AIDL.", "intermediate"),
        ("How do you test VHAL-dependent code without hardware?",
         "Emulator car data controls, reference/fake VHAL, 'cmd car_service inject-vhal-event', and abstraction "
         "layers faked in unit tests.", "intermediate"),
        ("A property returns STATUS_UNAVAILABLE. What do you do?",
         "Treat as a normal state (e.g. HVAC off, sensor not ready). Show a disabled/placeholder UI; don't crash; "
         "listen for the next event.", "intermediate"),
    ],
    "Driver distraction & UI": [
        ("How does an app know it must restrict UI while driving?",
         "Register with CarUxRestrictionsManager; react to flags (NO_VIDEO, NO_KEYBOARD, LIMIT_CONTENT...) and limits. "
         "Don't read speed yourself.", "basic"),
        ("What does distractionOptimized mean?",
         "Manifest meta-data declaring an activity follows DO guidelines and may run while moving; otherwise it's "
         "blocked by ActivityBlockingActivity. Honoured for system/pre-installed apps.", "intermediate"),
        ("Who configures the restrictions?",
         "The OEM, in car_ux_restrictions_map.xml per driving state (and per display).", "intermediate"),
        ("Design guidelines for car UI?",
         "Large touch targets (~76dp), high contrast, day/night themes, glanceable (≤2 s glances), minimal text, "
         "voice alternatives, rotary/knob support, consistent with OEM theming (RROs).", "basic"),
        ("What are RROs in AAOS?",
         "Runtime Resource Overlays: OEMs restyle system UI and car-ui-lib components without changing app code.", "advanced"),
    ],
    "Media & audio": [
        ("How does a media app integrate with the car?",
         "MediaBrowserService (tree) + MediaSession (control); the car's media UI renders it. Validate callers in "
         "onGetRoot, set content-style hints, declare PlaybackState actions, support search.", "basic"),
        ("Explain audio focus in AAOS.",
         "Usage maps to a context; CarAudioService uses an interaction matrix: exclusive, concurrent (duck) or reject. "
         "Apps request focus with correct AudioAttributes and handle loss/duck callbacks.", "intermediate"),
        ("What is an audio zone?",
         "An independent set of output devices and volume groups (e.g. rear-seat headphones) mapped to an occupant zone.", "intermediate"),
        ("Why fixed volume on AAOS?",
         "Gain is applied by the external amplifier/DSP per bus; Android outputs at fixed volume.", "advanced"),
    ],
    "Power, OTA, multi-user": [
        ("Describe car power states.",
         "ON, SHUTDOWN_PREPARE (save state, Garage Mode), WAIT_FOR_VHAL, suspend-to-RAM/disk, OFF; driven by the "
         "vehicle MCU via AP_POWER_STATE_REQ.", "intermediate"),
        ("What is Garage Mode?",
         "After ignition off, AAOS stays up briefly to run idle-constrained JobScheduler jobs (updates, sync) before "
         "suspending.", "basic"),
        ("How are OTA updates made safe?",
         "Signed packages, A/B slots, install while driving, reboot when parked with consent, verified boot + "
         "rollback, R156-compliant SUMS.", "basic"),
        ("What is MUMD?",
         "Multi-user multi-display (Android 14+): concurrent users on passenger/rear displays, each with its own apps "
         "and audio zone.", "advanced"),
    ],
    "Vehicle networks & security": [
        ("How does CAN arbitration work?",
         "Wired-AND bus; nodes send IDs MSB-first; dominant 0 wins; losers back off without corrupting the winner's "
         "frame. Lower ID = higher priority.", "basic"),
        ("CAN vs CAN FD vs Ethernet?",
         "CAN: 8 bytes, ≤1 Mbps. CAN FD: 64 bytes, faster data phase. Automotive Ethernet: 100M-multi-gig for "
         "cameras/backbone, SOME/IP, DoIP.", "basic"),
        ("What is a DBC file?",
         "The communication matrix describing frames (ID, DLC, sender) and signals (start bit, length, endianness, "
         "factor, offset, unit).", "basic"),
        ("Name some UDS services.",
         "0x10 session, 0x11 reset, 0x19 read DTC, 0x22 read DID, 0x27 security access, 0x29 authentication, 0x2E "
         "write DID, 0x31 routine control, 0x34/0x36/0x37 download.", "intermediate"),
        ("What is SecOC?",
         "AUTOSAR message authentication: truncated MAC + freshness value appended to PDUs to prevent spoofing, "
         "tampering and replay.", "intermediate"),
        ("R155 vs R156 vs ISO/SAE 21434?",
         "R155: cybersecurity management system (regulation). R156: software update management system. ISO/SAE 21434: "
         "the engineering standard describing how (TARA, etc.).", "intermediate"),
        ("How would you secure a car app's backend token?",
         "Android Keystore (AES-GCM), per user, never in code/logs; TLS with pinning; short-lived tokens + refresh.", "basic"),
    ],
    "Car App Library": [
        ("Explain the Car App Library lifecycle.",
         "Host binds CarAppService → createHostValidator → onCreateSession → Session.onCreateScreen → Screen stack via "
         "ScreenManager → onGetTemplate; invalidate() to refresh.", "basic"),
        ("Why templates?",
         "Host renders them, so they're distraction-optimized, OEM-styled, and consistent across Android Auto & AAOS.", "basic"),
        ("What limits do templates impose?",
         "List length (often 6 while driving), task depth (~5 screens), refresh quotas, content types per category.", "intermediate"),
    ],
}

SYSTEM_DESIGN = [
    ("Design a media streaming app for AAOS (e.g. for Harman's OEM customer)",
     ["<b>Clarify</b>: GAS or not, offline support, voice, multiple users, rear-seat zone.",
      "<b>Architecture</b>: Media3 MediaLibraryService + ExoPlayer; repository with cache (Room) per user; download "
      "worker (WorkManager, runs in Garage Mode).",
      "<b>Car integration</b>: browse tree ≤ 4 levels, content style hints, search (voice), PlaybackState actions, "
      "custom actions (like/shuffle), error states with resolution intents (sign-in on phone).",
      "<b>Distraction</b>: no own UI while driving; settings/sign-in only when parked (NO_SETUP).",
      "<b>Audio</b>: USAGE_MEDIA focus, handle duck/transient loss; resume after calls.",
      "<b>Security</b>: validate callers in onGetRoot, tokens in Keystore, per-user data.",
      "<b>Testing</b>: MediaController test harness (like MediaClient.kt), emulator, DHU for Android Auto."]),
    ("Design the HVAC system app",
     ["Privileged system app (priv-app + allowlist) with CONTROL_CAR_CLIMATE.",
      "Reads areaIds per property; builds zones dynamically (2-zone, 3-zone, 4-zone cars).",
      "Optimistic UI + reconcile with ON_CHANGE events; handle HVAC_POWER_ON gating and unavailable status.",
      "Debounce knob rotations (don't flood VHAL); respect min/max from config.",
      "Night mode, rotary input, accessibility; unit tests with fake data source; HIL with CANoe."]),
    ("Design an OTA client for the head unit",
     ["Check-in with backend (vehicle identity + current versions), download over Wi-Fi/cellular policy, resumable.",
      "Verify signature and hash; apply via update_engine to inactive slot while driving.",
      "Ask user; install/reboot only when parked (driving state + power policy); Garage Mode or scheduled time.",
      "Report status for R156 audit; automatic rollback if boot not marked successful; delta payloads."]),
]


def story():
    s = cover("Interview Q&A", "Automotive / AAOS interviews: sourced questions + answer bank",
              "\"Interviewers don't want definitions. They want to hear that you've been in the car.\"",
              ["part A: real reports with company, date & source", "part B: the AAOS topic bank",
               "part C: system design + your 60-second pitch"])
    s += toc()

    s += chapter("", "Read this first: how the questions were collected")
    s.append(P(f"Part A lists questions that candidates <b>publicly reported</b> on AmbitionBox, GeeksforGeeks and Medium. "
               f"They were retrieved on {RETRIEVED}. For each one the book gives the company, the role, the <b>date shown "
               "on the source</b> (usually when the report was posted, which can be after the interview) and the source "
               "link. These are candidate reports, not official question banks."))
    s += box("watch", ["An honest observation from the sources: the <b>publicly posted</b> Harman, Bosch, KPIT, Tata Elxsi, "
                       "Mercedes-Benz R&D and LTTS reports mostly contain <b>Android fundamentals, Kotlin, design patterns, "
                       "C/embedded and DSA</b>. Deep AAOS questions (VHAL, CarService, UX restrictions) are asked in "
                       "automotive roles but are rarely written up with dates.",
                       "So Part B is a <b>topic bank</b> covering those AAOS areas. It is <b>not</b> attributed to any "
                       "company or date, because inventing that would mislead you."])

    s += chapter("A", "Questions reported by candidates (with company, date, source)")
    rows = [["Company", "Role", "Date on source", "Reported questions", "Source"]]
    for comp, role, date, site, url, qs in SOURCED:
        rows.append([f"<b>{comp}</b>", role, date, "<br/>".join("• " + q for q in qs),
                     f"{site}<br/><font size='6.5' color='#6B7280'>{url}</font>"])
    s += table(rows, [24, 26, 20, 64, 36])
    s.append(P("Model answers (with a car twist)", "h2"))
    s.append(hand("tip: always end a fundamentals answer with 'in a car this matters because...'"))
    for q, a in ANSWERS_A:
        s += qa(q, a)

    s += chapter("B", "AAOS topic bank", "not tied to a specific company or date: these are the areas to master")
    s.append(P("Difficulty tags: <b>basic</b> (any Android dev moving to automotive), <b>intermediate</b> (2-5 years), "
               "<b>advanced</b> (platform/framework roles at Tier-1s and OEMs)."))
    for area, items in BANK.items():
        s.append(P(area, "h2"))
        for q, a, lvl in items:
            s += qa(q, a, lvl)

    s += chapter("C", "System design rounds")
    for title, points in SYSTEM_DESIGN:
        s.append(P(title, "h2"))
        s += bullets(points)
    s += box("interview", "Structure every design answer: clarify → constraints (driving, safety, power, users) → "
                          "components → data flow → security → testing → trade-offs. Drawing the AAOS stack in the first "
                          "minute shows domain fluency.")

    s += chapter("D", "Talk about this project")
    s.append(P("<b>Resume line:</b> Built an Android Automotive OS learning app (Kotlin, Jetpack Compose) that reads live "
               "vehicle data via CarPropertyManager with a simulator fallback, reacts to CarUxRestrictions, exposes a "
               "MediaBrowserService and a Car App Library charging app, and demonstrates CAN arbitration, SecOC, A/B OTA "
               "and power state machines with unit tests."))
    s.append(P("<b>60-second pitch:</b> 'I wanted to understand AAOS end to end, so I built an app where every concept is "
               "a live demo. The core is a VehicleDataSource interface with two implementations: a physics simulator for "
               "phones and a Car API implementation that subscribes to VHAL properties through CarPropertyManager and "
               "re-subscribes when CarService restarts. The UI is Compose with MVVM and StateFlow. For HVAC I look up area "
               "IDs from CarPropertyConfig instead of hard-coding them, and handle the SecurityException because "
               "CONTROL_CAR_CLIMATE is privileged. I also modelled CAN arbitration, SecOC replay protection and A/B OTA "
               "rollback as pure Kotlin state machines with JVM tests.'"))
    s += box("pencil", ["Practise out loud:",
                        "1. Explain what happens between turning the HVAC knob and the blend door moving.",
                        "2. Explain why your app can read speed but cannot change the temperature on a real car.",
                        "3. Explain what your MediaBrowserService does when the steering-wheel 'next' button is pressed."])

    s += chapter("E", "Behavioural questions for Tier-1 / OEM roles")
    s += bullets([
        "<b>Working with OEM customers</b>: requirements change late; talk about clarifying specs and change requests.",
        "<b>Quality mindset</b>: ASPICE processes, code reviews, traceability from requirement to test.",
        "<b>Debugging on target</b>: logcat, dumpsys car_service, bugreports, CAN traces; reproducing on HIL benches.",
        "<b>Safety culture</b>: 'I would rather delay a release than ship something that can distract the driver.'",
    ])
    s += box("bullets", bullets([
        "Fundamentals (lifecycle, MVVM, coroutines, Kotlin) dominate public reports; know them cold.",
        "Always add a car-specific angle to fundamental answers.",
        "Master the AAOS stack, VHAL properties, UX restrictions, media, audio focus.",
        "Know CAN basics, UDS, SecOC, R155/R156/21434.",
        "Use this project as your story.",
    ]))
    return s


if __name__ == "__main__":
    build("../04_Interview_QA.pdf", "04 Interview Q&A - Automotive Academy", "Interview preparation", story())
