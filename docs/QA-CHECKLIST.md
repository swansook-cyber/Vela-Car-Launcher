# Vela Car Launcher V1.0 — Internal QA

Target: Isuzu D-Max 2010 2.5L, Android 12 aftermarket head unit.

## Build gate
- [x] Local unit tests and assembleDebug succeed
- [x] APK installs on Android 12 emulator without replacing the original launcher
- [x] Launcher icon appears and app opens in landscape (1280 × 720)
- [x] Driving / Navigation buttons switch; navigation app opens without a fabricated destination
- [x] Fine location permission is checked and can be granted
- [x] GPS speed shows unavailable when location is invalid, and clears when GPS is disabled
- [x] Open-Meteo weather loads at the accepted GPS location and shows update time
- [x] mph setting persists after app restart
- [ ] GitHub Actions build succeeds on the V1 feature branch
- [ ] Verify Quick Apps ordering and VelaRedio MediaSession against the actual installed apps
- [ ] Speed updates outdoors while moving (passenger observes; driver does not interact)
- [ ] Existing navigation, radio, reverse camera and hardware Home/Back still work
- [ ] Can exit and reopen the original launcher without reset

## Not accepted yet
- Physical head-unit and in-vehicle validation remains, including reverse camera, MCU, and hardware controls.
- VelaRedio is not installed on the emulator, so station metadata and transport actions cannot be verified there.
- Signed release APK requires the original permanent keystore and signing credentials; no replacement key is allowed.
- No embedded map or ECU/OBD telemetry is claimed.

## Safety
Never test interactions while driving. Do not set Vela as default HOME before rollback testing. Do not show fake fuel, gear, TPMS or speed values. Debug builds are internal-only and are not signed with the permanent production key.
