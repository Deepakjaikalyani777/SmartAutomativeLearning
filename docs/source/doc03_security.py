from hf import *


def story():
    s = cover("Automotive Security", "Protecting a 2-tonne computer that drives at 120 km/h",
              "\"In a phone, a bug leaks your photos. In a car, a bug can steer.\"",
              ["attacks that really happened", "standards you must name in interviews",
               "secure coding checklist for AAOS apps"])
    s += toc()

    s += chapter("1", "Why car security is different")
    s += table([
        ["Phone", "Car"],
        ["Replaced every 2-4 years", "On the road 15-20 years; crypto must age well"],
        ["Worst case: data theft", "Worst case: physical harm (safety + security are linked)"],
        ["One vendor controls most of the stack", "Dozens of suppliers, ECUs and firmware images"],
        ["Rarely physically attacked", "Attacker can touch the OBD port, bus wiring, key fob radio"],
        ["Updates are easy", "Updates must never brick; some ECUs cannot be updated OTA at all"],
    ], [85, 85])
    s.append(P("Attack surfaces", "h2"))
    s += stack_diagram(["Long range: cellular (TCU), backend/cloud APIs, OEM mobile app",
                        "Short range: Bluetooth, Wi-Fi, key fob / UWB, TPMS sensors, V2X",
                        "Physical: OBD-II port, USB, SD card, CAN wiring, debug ports (JTAG/UART)",
                        "Supply chain: third-party libraries, ECU firmware, developer machines"],
                       notes=["remote, scalable = worst", "needs proximity", "needs access to the car",
                              "hits every car at once"])
    s += box("brain", "Which attack surface scares an OEM most, and why? Think about <i>scale</i>: one exploit vs one car.")

    s += chapter("2", "Hacks that changed the industry", "learn from other people's recalls")
    s += table([
        ["Year", "What happened", "Lesson"],
        ["2015", "Charlie Miller & Chris Valasek remotely controlled a Jeep Cherokee (steering, brakes at low speed) via "
                 "the Uconnect head unit's cellular connection. FCA recalled ~1.4 million vehicles.",
         "Head unit must not be able to reach safety buses; add a secure gateway; firewall the modem."],
        ["2015", "ADAC found BMW ConnectedDrive used unencrypted HTTP; doors could be unlocked by a fake base station. "
                 "~2.2 million cars patched over the air.", "Use TLS with certificate validation everywhere."],
        ["2016", "Keen Security Lab remotely attacked a Tesla Model S through the browser/Wi-Fi and reached CAN.",
         "Browser/rendering engines are attack surface; code signing for firmware; fast OTA response."],
        ["2016", "NissanConnect EV API allowed controlling climate of other Leafs knowing only the VIN.",
         "Backend APIs need real authentication; VINs are not secrets."],
        ["2022-2024", "Researchers (e.g. Sam Curry et al.) found OEM web portals/APIs that allowed locating, unlocking "
                      "or starting cars of many brands; a 2024 Kia issue needed only the licence plate.",
         "The cloud is part of the vehicle attack surface."],
        ["Ongoing", "Relay attacks on keyless entry: two radios extend the key fob range, the car opens.",
         "Distance bounding with UWB (CCC Digital Key 3.0); motion-sensing fobs."],
    ], [18, 97, 55])
    s += box("interview", "Tell the Jeep story in 30 seconds and end with the architecture lesson: 'That's why the IVI is "
                          "isolated behind a secure gateway and why CAN messages to safety ECUs need SecOC.'")

    s += chapter("3", "Standards and regulations", "names you must drop in the interview")
    s += table([
        ["Standard", "What it requires", "Since"],
        ["UNECE R155", "Cybersecurity Management System (CSMS) for the OEM organisation + vehicle type approval",
         "EU: new types July 2022, all new vehicles July 2024"],
        ["UNECE R156", "Software Update Management System (SUMS), safe & secure OTA", "Same timeline as R155"],
        ["ISO/SAE 21434", "Cybersecurity engineering across the lifecycle: TARA, requirements, verification, "
                          "incident response", "Published 2021"],
        ["ISO 26262", "Functional safety (not security, but they must be coordinated)", "2011, 2nd ed. 2018"],
        ["ISO 24089", "Software update engineering", "2023"],
        ["China GB 44495-2024", "Mandatory vehicle cybersecurity requirements for China", "Applies from 2026"],
    ], [34, 90, 46])
    s += fireside("Functional Safety vs Cybersecurity", [
        ("Safety (ISO 26262)", "I worry about things going wrong by accident: a sensor fails, a bit flips, a bug in the "
                               "braking code. I ask: 'what if this fails?'"),
        ("Security (ISO/SAE 21434)", "I worry about someone making things go wrong on purpose. I ask: 'what if someone "
                                     "attacks this?'"),
        ("Safety", "My failures follow statistics. Yours don't: attackers pick the worst moment."),
        ("Security", "And if I lose, you lose too. A spoofed brake message is my problem and your hazard."),
        ("Both", "That's why OEMs run HARA and TARA side by side and share findings."),
    ])
    s.append(P("TARA: Threat Analysis and Risk Assessment (ISO/SAE 21434)", "h2"))
    s += flow_diagram(["Item &\nassets", "Damage\nscenarios", "Threat\nscenarios\n(STRIDE)", "Attack\npaths",
                       "Feasibility\n+ impact", "Risk\nvalue", "Treatment:\nreduce / accept"], box_h=40)
    s += box("pencil", ["Do a mini-TARA for the asset <b>'unlock doors' CAN message</b>:",
                        "1. Damage scenario?  2. STRIDE category?  3. One attack path?  4. A treatment?",
                        "(Answers at the end of the book.)"])

    s += chapter("4", "Android platform security on AAOS")
    s.append(P("Verified boot: the chain of trust", "h2"))
    s += flow_diagram(["Boot ROM\n(fused key hash)", "Bootloader\n(signed)", "vbmeta\n(AVB)", "boot / kernel",
                       "system/vendor\n(dm-verity)", "Apps\n(APK sig)"],
                      labels=["verifies", "verifies", "hash", "verity", "PackageManager"])
    s.append(P("Each stage verifies the next <i>before</i> running it. The root of trust is in hardware (ROM + fuses). "
               "AVB also stores a <b>rollback index</b> so an attacker cannot install an older, vulnerable, but "
               "properly signed image."))
    s += box("app", "Lesson <b>13 Security</b> → 'Tamper with kernel & boot': the chain ticks green until the kernel, "
                    "turns red and halts. Tampered code never runs.")
    s.append(P("Permissions: four protection levels", "h2"))
    s += table([
        ["Level", "Granted how", "Car examples"],
        ["normal", "At install, automatically", "CAR_INFO, CAR_POWERTRAIN, CAR_EXTERIOR_ENVIRONMENT"],
        ["dangerous", "User dialog at runtime", "CAR_SPEED, CAR_ENERGY (Android 14+), location, microphone"],
        ["signature", "Only apps signed with the same key as the definer (platform/OEM)", "Many CarService internals"],
        ["privileged (signature|privileged)", "Signature OR pre-installed in /system/priv-app + allowlisted in "
                                              "privapp-permissions XML", "CONTROL_CAR_CLIMATE, CONTROL_CAR_DOORS, CAR_VENDOR_EXTENSION"],
    ], [38, 70, 62])
    s += bullets([
        "<b>SELinux</b> (enforcing) confines every process, including the VHAL and CarService, to a policy domain.",
        "<b>App sandbox</b>: each app has its own Linux UID; each Android user has separate storage.",
        "<b>Keystore / TEE / StrongBox</b>: keys generated and used inside secure hardware; the app only gets handles.",
        "<b>Seccomp, ASLR, CFI, hardened malloc</b>: exploit mitigations inherited from Android.",
    ])
    s.append(code("""
// core/security/SecureVault.kt - AES-256-GCM key that never leaves the Keystore
KeyGenParameterSpec.Builder(alias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .build()
"""))
    s += box("app", "Security lesson → 'Encrypt key token': a digital key token is encrypted with AES-GCM, stored as "
                    "Base64 and decrypted. The app reports whether the key is inside secure hardware.")

    s += chapter("5", "In-vehicle network security")
    s += stack_diagram(["Infotainment domain (AAOS, Wi-Fi, BT, cellular)", "Central secure gateway: filtering, rate limiting, IDS",
                        "Powertrain  |  Chassis  |  Body  |  ADAS domains"],
                       notes=["untrusted-ish", "allowlist of IDs per direction", "safety-critical"])
    s.append(P("SecOC: Secure Onboard Communication (AUTOSAR)", "h2"))
    s += bits_diagram([("payload", "data bytes", 2.2), ("freshness", "counter/time", 1.4), ("MAC", "truncated CMAC/HMAC", 1.8)],
                      caption="Secured PDU = payload + freshness value + truncated MAC over (ID | payload | freshness)")
    s += table([
        ["Attack", "What SecOC does"],
        ["Spoofing (send fake 'unlock')", "Receiver recomputes MAC with the shared key; mismatch → drop"],
        ["Tampering (change payload)", "MAC no longer matches → drop"],
        ["Replay (resend yesterday's valid frame)", "Freshness value must be newer than the last accepted one → drop"],
    ], [60, 110])
    s += box("app", "Security lesson → SecOC demo: 'Legit unlock' is ACCEPTED, 'Attacker spoof' is REJECTED_BAD_MAC, "
                    "'Replay old frame' is REJECTED_REPLAY. The logic is in SecOc.kt and verified by SecOcTest.")
    s += box("watch", ["Truncating the MAC (e.g. to 24-64 bits) is normal on CAN because space is tiny; security then "
                       "also depends on the receiver limiting the attempt rate.",
                       "Key management is the hard part: keys are provisioned in the factory into each ECU's HSM."])
    s += bullets([
        "<b>IDS</b>: detect unusual frame rates, unknown IDs, impossible signal values; report to a Vehicle SOC in the cloud.",
        "<b>Ethernet</b>: VLAN segmentation, MACsec (802.1AE), TLS/IPsec for SOME/IP and diagnostics.",
        "<b>Secure diagnostics</b>: UDS 0x27 SecurityAccess (seed/key, weak if the algorithm leaks) is being replaced by "
        "0x29 Authentication (certificate-based, PKI).",
    ])

    s += chapter("6", "Keys, HSMs and the digital key")
    s += table([
        ["Component", "Role"],
        ["HSM (in ECU microcontrollers, e.g. EVITA levels)", "Stores keys, does AES-CMAC for SecOC, secure boot of the MCU"],
        ["TEE / StrongBox (in the Android SoC)", "Android Keystore keys, DRM, biometric templates"],
        ["Backend PKI", "Issues certificates for vehicles, ECUs, V2X, diagnostic testers"],
        ["CCC Digital Key 3.0", "Phone as key over NFC + BLE + UWB; UWB time-of-flight defeats relay attacks"],
    ], [70, 100])
    s += box("brain", "A relay attacker forwards radio signals between your key (in your house) and your car. Why can BLE "
                      "signal strength be fooled, but UWB time-of-flight cannot be made <i>shorter</i>?")

    s += chapter("7", "OTA security")
    s += bullets([
        "Packages are <b>signed</b> by the OEM; the device verifies before installing (and AVB verifies again at boot).",
        "<b>Anti-rollback</b>: rollback index prevents installing older vulnerable images.",
        "<b>Uptane</b>: separates a Director repository (what this car should install) from an Image repository (what "
        "exists) with separate keys, so a single compromised server cannot push malware to the fleet.",
        "Transport over TLS with pinned certificates; resume + integrity checks for partial downloads.",
        "Install only when safe (parked), with user consent, logged for R156 audits.",
    ])
    s += box("app", "Lesson <b>12 OTA</b> shows verify → install inactive slot → reboot → rollback if the new slot fails to boot.")

    s += chapter("8", "Secure coding checklist for AAOS app developers", "print this page")
    s += table([
        ["Area", "Do", "Don't"],
        ["Exported components", "Set android:exported explicitly; protect services with permissions",
         "Leave services exported without checks"],
        ["MediaBrowserService", "Validate caller in onGetRoot (package + signature); return a limited root for unknown callers",
         "Expose the full library to any app"],
        ["Car App Library", "HostValidator allowlist in release", "Ship ALLOW_ALL_HOSTS_VALIDATOR"],
        ["Permissions", "Request only what you need; handle SecurityException", "Assume car permissions are granted"],
        ["Secrets", "Android Keystore (AES-GCM), no keys in APK", "Hard-code API keys or tokens"],
        ["Network", "TLS + network security config; certificate pinning for sensitive APIs", "Cleartext HTTP"],
        ["Multi-user", "Store data per Android user; wipe guest data", "Assume user 0 or share data across drivers"],
        ["Logging", "Strip PII/location/VIN from logs in release", "Log tokens or precise location"],
        ["Intents", "Use explicit intents; validate extras", "Trust data in incoming intents"],
        ["Dependencies", "SBOM, vulnerability scanning, pin versions", "Pull random SDKs into a car app"],
    ], [30, 80, 60])

    s += chapter("9", "Privacy")
    s.append(P("Cars know where you live, work and sleep, how you drive, who you call, and (with driver-monitoring "
               "cameras) where you look. GDPR (EU) and DPDP Act 2023 (India) apply. Principles: data minimisation, "
               "purpose limitation, consent, retention limits, and per-user separation (a guest must not see the "
               "owner's contacts or history)."))

    s += chapter("10", "Security questions interviewers ask")
    s += dumb([
        ("What is the difference between safety and security?", "Safety (ISO 26262) protects against <i>random failures "
         "and systematic errors</i>; security (ISO/SAE 21434) protects against <i>intentional attackers</i>. A security "
         "breach can cause a safety hazard, so the two analyses are linked."),
        ("Why can't we just encrypt CAN?", "Classic CAN has 8 bytes and hard real-time deadlines; encryption adds latency "
         "and doesn't solve authentication alone. SecOC adds authenticity + freshness; CAN FD gives room for MACs. "
         "Confidentiality is usually not the goal for control messages."),
        ("How does AAOS stop a random app from unlocking the doors?", "CONTROL_CAR_DOORS is signature|privileged; CarService "
         "checks it before calling VHAL; SELinux prevents the app from talking to VHAL directly; the gateway and SecOC "
         "protect the bus side."),
        ("What is a TARA?", "Threat Analysis and Risk Assessment: identify assets, damage scenarios, threats, attack paths, "
         "rate impact and feasibility, compute risk, choose treatment. Required by ISO/SAE 21434 and audited under R155."),
        ("What is a root of trust?", "An immutable starting point (Boot ROM + fused public key hash, or an HSM) that is "
         "trusted by definition and verifies everything after it."),
        ("How do you store a refresh token in a car app?", "Encrypt it with an Android Keystore AES-GCM key (hardware-backed "
         "when available), per Android user; never in plain SharedPreferences or in the APK."),
    ])

    s += chapter("", "Sharpen your pencil: answers")
    s += bullets([
        "Damage scenario: an attacker unlocks the car → theft or harm to occupants.",
        "STRIDE: <b>S</b>poofing (fake sender) and <b>T</b>ampering; replay is a spoofing variant.",
        "Attack path: compromise IVI via Bluetooth bug → send CAN frame through a weak gateway rule; or plug into the "
        "bus behind the headlight.",
        "Treatment: SecOC with freshness on the door message, gateway allowlist (IVI may not send 0x2A0), IDS alerts.",
    ])
    s += box("bullets", bullets([
        "Cars: long life, physical access, safety impact, many suppliers.",
        "Remember Jeep 2015 → isolate IVI with a gateway.",
        "R155 (CSMS), R156 (SUMS), ISO/SAE 21434 (TARA).",
        "Verified boot + SELinux + permissions + Keystore on AAOS.",
        "SecOC = MAC + freshness against spoofing and replay.",
        "UWB distance bounding against relay attacks.",
    ]))
    return s


if __name__ == "__main__":
    build("../03_Automotive_Security.pdf", "03 Automotive Security - Automotive Academy", "Security", story())
