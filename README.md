# Automotive Academy: Android Automotive OS learning app

A Kotlin + Jetpack Compose sample app for learning Android Automotive OS (AAOS) concepts. Each concept is a live, animated lesson.

- Runs on **phones** (built-in vehicle simulator) and on **AAOS** (real `CarPropertyManager` / `CarUxRestrictionsManager`).
- 15 lessons: Architecture, Cluster, VHAL, HVAC, Driver Distraction, Media, Audio Focus, CAN Bus, Park Assist/EVS,
  EV Charging, Power, OTA, Security, Multi-Display, Car App Library.
- Includes a real `MediaBrowserService` (car media UI) and a Car App Library `CarAppService` (charging POI).

## Build & run
```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.deepak.automotive/.MainActivity --es lesson vhal   # open a lesson directly
```

## Docs (Head First style, PDF)
| File | Contents |
|------|----------|
| `docs/00_Start_Here.pdf` | Setup, lesson map, 14-day plan |
| `docs/01_Project_Structure.pdf` | Codebase tour and exercises |
| `docs/02_Automotive_Concepts.pdf` | AAOS, VHAL, CAN, UX restrictions, media, audio, power, OTA, EVS, EV... |
| `docs/03_Automotive_Security.pdf` | Real hacks, R155/R156, ISO/SAE 21434, secure boot, SecOC, checklist |
| `docs/04_Interview_QA.pdf` | Sourced company questions with dates + AAOS topic bank + system design |

Regenerate the PDFs with `cd docs/source && python3 build_all.py` (needs `reportlab` and macOS system fonts).
