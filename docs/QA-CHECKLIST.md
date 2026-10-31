# Vela Car Launcher V0.1 — Internal QA

Target: Isuzu D-Max 2010 2.5L, Android 12 aftermarket head unit.

## Build gate
- [ ] GitHub Actions assembleDebug succeeds
- [ ] APK installs without replacing the original launcher
- [ ] Launcher icon appears and app opens in landscape
- [ ] Driving / Navigation buttons switch and persist after restart
- [ ] Fine location permission prompts correctly
- [ ] GPS speed shows unavailable when location is not valid
- [ ] Speed updates outdoors while moving (passenger observes; driver does not interact)
- [ ] Existing navigation, radio, reverse camera and hardware Home/Back still work
- [ ] Can exit and reopen the original launcher without reset

## Not accepted yet
- Weather and VelaRedio panels are placeholders
- Quick app tiles are placeholders
- No embedded map or vehicle ECU/OBD telemetry
- Branding image assets not integrated yet
- No release APK or Vela App Store publishing

## Safety
Never test interactions while driving. Do not set this prototype as default HOME. Do not show fake fuel, gear, TPMS or speed values. Debug builds are internal-only and not signed with the permanent production key.
