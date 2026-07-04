# NeonGrid — Release Guide

## 1. Create your upload keystore (one time, do this yourself)

The signing key must be owned and backed up by you — anyone with it can publish
updates to your app. Run in PowerShell (pick your own strong password):

```powershell
& "C:\Program Files\Java\jdk-21.0.11\bin\keytool.exe" -genkeypair `
  -alias neongrid-upload -keyalg RSA -keysize 2048 -validity 10000 `
  -keystore "C:\Users\amirf\neongrid-release-keys\upload-keystore.jks"
```

**Back the .jks file up somewhere safe (not only this PC).** Then add to
`C:\Users\amirf\.gradle\gradle.properties` (never commit these):

```
NEONGRID_KEYSTORE=C:/Users/amirf/neongrid-release-keys/upload-keystore.jks
NEONGRID_KEYSTORE_PASSWORD=<your password>
NEONGRID_KEY_ALIAS=neongrid-upload
NEONGRID_KEY_PASSWORD=<your password>
```

## 2. Enable the signing config

In [app/build.gradle.kts](app/build.gradle.kts), add inside `android { }`
(the release buildType already exists):

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(providers.gradleProperty("NEONGRID_KEYSTORE").get())
        storePassword = providers.gradleProperty("NEONGRID_KEYSTORE_PASSWORD").get()
        keyAlias = providers.gradleProperty("NEONGRID_KEY_ALIAS").get()
        keyPassword = providers.gradleProperty("NEONGRID_KEY_PASSWORD").get()
    }
}
```
and in `buildTypes.release`: `signingConfig = signingConfigs.getByName("release")`

## 3. Build the App Bundle

```powershell
.\gradlew.bat :app:bundleRelease
# → C:\Users\amirf\.neongrid-build\app\outputs\bundle\release\app-release.aab
# (build output is redirected outside the repo because OneDrive locks build dirs;
#  see the allprojects block in build.gradle.kts)
```

## 4. Play Console checklist

- Create app (name: pick final — check the store for collisions first).
- Enroll in **Play App Signing** (upload key = the one above).
- **Data safety form:** "No data collected, no data shared" — truthful: the app
  has zero SDKs, zero network access (no INTERNET permission), everything local.
- **Privacy policy:** host [store-assets/privacy-policy.md](store-assets/privacy-policy.md)
  (GitHub Pages or any static host) and link it.
- **Content rating questionnaire:** puzzle game, no violence/UGC → Everyone.
- Listing assets: 512×512 icon, 1024×500 feature graphic, 4–8 phone screenshots
  (capture combo/clear moments — the neon effects are the marketing).
- **New personal dev accounts:** Play requires a closed test with 12 testers for
  14 days before production — start that clock early.
- Target API is already 36; R8 + resource shrinking enabled; APK ~1.1MB.

## 5. Pre-submit QA (each release)

- `.\gradlew.bat :engine:test` — all green (includes 5,000-game fairness sim).
- Full playthrough of the **release** build (R8 can behave differently).
- Kill app mid-run → relaunch → run resumes.
- Advance device clock a day → streak increments; missions rotate.
- Merged manifest has only `VIBRATE` permission.

## Deferred polish (nice-to-have before launch)

- Real synthwave music loop + designed SFX (current SFX are synthesized
  placeholders in [SoundEngine.kt](app/src/main/kotlin/com/neongrid/app/juice/SoundEngine.kt);
  swap in SoundPool + ogg assets, keep the same public API). For the music
  loop, re-add `media3-exoplayer` — it was removed because its manifest drags
  in ACCESS_NETWORK_STATE (the app manifest now strips it defensively anyway).
- AGSL scanline/RGB-split shader on clears (API 33+) with band-sweep fallback.
- Baseline profile for cold-start.
- Orbitron/Rajdhani display font (OFL license) for HUD.
