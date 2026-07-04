# Cube Crash

A neon-cyberpunk block puzzle game for Android (Block Blast-style), built with
Kotlin + Jetpack Compose. Completely free: no ads, no IAP, no accounts, no
network — the manifest requests only the VIBRATE permission.

## Gameplay

Drag the active piece onto an 8×8 energy grid — a NEXT slot previews the
upcoming piece; **tap the active piece to rotate it 90°**. Full rows/columns
discharge in a neon surge. Chain clears to build a combo heat meter (dies
after 3 placements without a clear). Game over when nothing fits in any
rotation — "GRID FAILURE" — or when a timed mode's clock runs out.

- **Three categories:** BEGINNER (untimed classic), INTERMEDIATE (30s to
  place each piece), ADVANCED (30s that shrinks 1s every 2,000 points, down
  to a 5s floor).
- **Fair-but-tense generator:** every dealt piece is mathematically guaranteed
  placeable after the piece ahead of it (sequence solvability with line-clear
  simulation); difficulty ramps by score band; a relief piece appears under
  sustained congestion.
- **Retention:** daily streaks, 3 rotating daily missions, earned-only "shards"
  currency, 8 unlockable themes.
- **Juice:** pooled particle system, trauma screen shake, staggered clear
  animations, escalating haptics, slow-mo on big clears, danger pulse at 75%
  fill. Reduce-motion setting for accessibility.
- **Audio:** fully procedural — a synthesized synthwave loop (Am–F–C–G,
  110 BPM) and an in-key clear arpeggio that climbs the scale with your combo.
  Zero audio assets, zero licensing. In-game music mute + Settings toggles.
- **Web demo:** `web-demo/index.html` is a self-contained HTML5 port of the
  core loop (serve with `python -m http.server 8123 -d web-demo`).

## Project layout

- `engine/` — pure Kotlin JVM game logic (bitboard, pieces, reducer, adaptive
  generator). Zero Android deps; tests run in ~1s.
- `app/` — Compose UI, DataStore persistence (CBOR), juice systems, meta.

## Build

Requires JDK 21 and the Android SDK (`local.properties` → `sdk.dir`).

```powershell
.\gradlew.bat :engine:test          # engine tests incl. 5,000-game fairness sim
.\gradlew.bat :app:assembleDebug    # debug APK
.\gradlew.bat :app:assembleRelease  # R8-minified release (~1.1MB)
```

Build output is redirected to `C:\Users\amirf\.neongrid-build\` (outside the
OneDrive-synced repo — OneDrive locks build intermediates otherwise).

See [RELEASE.md](RELEASE.md) for signing and Play Store submission.
