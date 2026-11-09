# Darts Snap Remote 3.3 — experimental Darts Club overlay

This is the experimental branch, **not** the original Darts Pen Overlay on
`main`. It builds two Android APK variants for 3.3, both retaining the existing
41 replay slots and their per-target path editor while adding 20 singles.

## v3.3: Compact Single / Double / Treble remote

In **Spelen**, tap **🎛 Open 61-target remote**. The floating controller is
narrower (274dp instead of 290dp) and shorter (445dp instead of 520dp).
It has three clearly labeled buttons: **Single**, **Double** and **Treble**.
Only one numbered grid is shown at a time, ordered **20 through 1**.
The **Bull** button stays visible beneath every grid.

- Single buttons record/replay/edit **S20 ... S1**; doubles **D20 ... D1**;
  trebles **T20 ... T1**. Bull has its own original slot.
- Tap a green saved target to replay its swipe, exactly as before.
- Tap **💾 Save: OFF**, then tap a target to store your last captured throw in
  *only* that slot. Existing recordings in other slots are unchanged.
- Long-press a green saved target, or tap **✎ Edit: OFF** followed by the target,
  to edit that **individual** saved path using the existing v3.2 PRO-like tools.
- The selected Single/Double/Treble tab is remembered when closing/reopening the
  controller. Existing saved 41 T/D/Bull recordings use their unchanged keys.
- Throw speed remains the same **1–100%** slider, with 100% meaning original
  duration. Swipes still dispatch after your finger lifts, not live.

### Installing without losing previous saved throws

The **update** variant uses `nl.dartspen.overlay.gamesnap.percentremote`,
the same app ID as v3.1 and v3.2 update. If Android accepts an installation
over that package, its saved throws and edits are retained.

The **safecopy** variant uses `nl.dartspen.overlay.gamesnap.editorcopy`,
the same app ID as v3.2 safecopy. If that app is already installed and cannot
be updated due to signing mismatch, don't uninstall it if you need its saved
throws. Both APKs are GitHub Actions debug-signed; a mismatch can prevent an
in-place update. **Never uninstall the old app just to bypass the signature
check** without separately backing up what you need.

Keep only one overlay active while using Darts Club. Compatibility and
individual saved-throw playback still require testing on an Android phone.



## v3.2: PRO-style editor for each saved throw

**Edit one target without changing any other recording.** In **Spelen**,
open the **41-target remote**. Either long-press a **green saved** target
or tap **✎ Edit saved: OFF** and then tap a green target.

The full-screen, semi-transparent editor shows the **actual replay path** in
yellow, the original path as a faint white reference, the starting point in
cyan, and the ending point in orange. You can drag the yellow swipe to move
it, or drag its start/end handles to adjust just that end. In the remote:
- **Move: Whole / Start / End** chooses what the arrow controls nudge.
- **Step: 0.5 / 1 / 2 / 5 dp** sets each nudge.
- **↶ / ↷ 0.25°** rotate the entire swipe around its center.
- **− / + 0.5%** scale the swipe's length.
- **− / + 10 ms** adjust this target's recorded throw duration (25–1400 ms).
  The existing 1–100% remote speed multiplier is still applied on playback.
- **Undo** reverses the last path change; **Reset** restores the geometry and
  duration from when this editing session opened. **Previous saved version**
  loads the last persisted backup of this one target, if available.
- **✓ Save this throw** replaces only the current target's recording and keeps
  its previous version as a backup; **× Cancel** discards the session.

While editing, the full-screen layer **intercepts touches**, so touching
the dartboard does NOT send a throw. This is not an aim-assist or a live
correction to the game's raw touch stream. Replays are still deliberately
triggered by the remote. Edits keep the original target label, and display
profiles (portrait/landscape/fold) remain separate. To make the preview
accurate, previous snap correction is **baked into the editable path**;
saving that edited path stores snap at 0% so it isn't applied twice.

### Two install APKs

- **update APK**: same app ID as v3.1 (`nl.dartspen.overlay.gamesnap.percentremote`).
  Try installing over v3.1: if signing identities match, its saved throws
  are retained. If Android refuses the upgrade, **do not uninstall v3.1** just
  to make it work; that would erase those recordings.
- **safecopy APK**: installs as a separate app ID
  (`nl.dartspen.overlay.gamesnap.editorcopy`). Your v3.1 recordings stay
  safe inside the old app, but this new copy starts with empty slots. They
  must be recorded again; the app can't read other app-private data.

The GitHub Actions workflow now caches the experimental debug signing key to
help future v3.2+ updates reuse the same signing identity. A GitHub cache can
expire, so **do not depend on it as a permanent backup**.

## v3.1: Descending target order and precise speed

The floating remote now shows **T20, T19, T18 ... T1** followed by
**D20, D19, D18 ... D1**, with **Bull** below. Only the button layout changes;
each saved slot remains named T20, T19 etc.

The **Speed** control is now a slider adjustable in **1% increments from 1%
through 100%**, plus **− / +** buttons (1 percentage point per tap). Long
press either button for a 10-point step. Tap the percentage label to reset to
100%. **100% is the original recorded swipe duration.** Below 100% means
slower, not faster: 50% replays at twice the duration, 25% at four times, and
1% requests 100 times the duration. Android limits accessibility gestures to
60 seconds, so very low percentages can be capped. Slower simulated gestures
may be rejected or interpreted differently by Darts Club.

### Safe install alongside your previous version

This build is named **Darts Snap Remote 3.1** with its own package ID,
`nl.dartspen.overlay.gamesnap.percentremote`, and installs **alongside**
your earlier Darts Pen Snap (Test) app. Your previously saved T20 recording
remains in that old app, not in the new one. You must capture it again in
v3.1 (or migrate it separately with advanced Android development tools).
Do **not** uninstall your old app if its recording is valuable.
Disable the old overlay while testing the new one.

## NEW: Floating replay remote and throw speed

In **Spelen**, tap **🎛 Open 41-target remote**. You can drag its **↕** handle,
scroll through **TREBLES** (T1–T20) and **DOUBLES** (D1–D20), or press **Bull**.
The remote draws saved throws as **green buttons** and empty targets as gray.
Tapping a green target manually sends its saved swipe to Darts Club.

### Saving a recording straight to any target

1. Enable **◎ Vang worp** and capture a real swipe. The experimental
   accessibility capture automatically plays it once after you lift your finger.
2. In the remote tap **💾 Save last: OFF** to enable save mode.
3. Tap the target you want to assign, such as **T20**. Save mode turns itself
   back off when saved. If you had already saved a throw to this slot, saving
   again replaces it. It does **not** change the other 40 slots.
4. Tap **▶ T20** to replay that saved path without touching the game board.
   Your 41 recordings persist after app restarts, within the same display profile.
5. To close the remote tap **×**. It automatically becomes non-touchable and
   visually hidden during a replay, then reappears afterward.

### Playback speed

Use **− / +** and the slider next to **Speed** for 1%–100%. 100% keeps
the original recorded duration and values below 100% replay more slowly.
The setting is remembered. Durations are limited by the Android accessibility
gesture-duration maximum (currently 60 seconds). The saved path and original
duration are unchanged; speed applies to both the remote and the small
**▶ T20** button.

**Limitations:** This is accessibility gesture injection after capture, not
real-time touch modification. Darts Club may respond differently to replay
than to a direct swipe, and speed adjustment may affect accuracy. This build
has not been device-tested. Keep the original Darts Pen Overlay disabled while
testing the experimental overlay.

**Updates and signing:** Each GitHub Actions debug build may have a different
debug signing key. You might need to uninstall only **Darts Pen Snap (Test)**
before installing this update. Uninstalling clears that test app's saved
throws and calibration. Your separate original Darts Pen Overlay is unaffected.

## Install and try it

1. Download the ZIP artifact **DartsPenOverlay-APK** from the latest successful
   [GameSnap branch build](https://github.com/ivooverdiek-web/Dartspenoverlay/actions?query=branch%3Agamesnap-angle-snap-demo).
   Extract `app-debug.apk` and install it on your Android phone.
2. Open **Darts Pen Snap (Test)**. Follow its instructions to allow display
   over other apps and start the overlay; open Darts Club.
3. Use the overlay's **Spelen** (Play) controls. Enable **◎ Vang worp**.
   If prompted, enable the experimental app's **Practice Replay** accessibility
   service in Android Settings.
4. In the practice settings row, tap **Snap** to cycle through **0% → 55% →
   85% → 100%** correction. 85% is the starting value.
5. Swipe over the capture area. The game sees a synthetic, straightened swipe
   *after your finger lifts*. Test it in practice mode first.

**Limits:** This is not real-time interception. The game dart will not move in
response to your swipe while the capture overlay is intercepting touches.
Android gesture dispatch is not guaranteed to be accepted by every game.
The snap algorithm preserves the first and last swipe positions, blending all
intermediate positions toward a straight path using traveled distance.
Turn Snap to 0% for the original unmodified replay.

**Important:** Switch off your original Darts Pen Overlay while running the
experimental app so the two overlays do not conflict. The new app uses separate
settings and does **not** import saved calibration from the original app.
These GitHub Actions debug APKs are signed with ephemeral debug keys; later
experimental builds may not install as updates over this test build without
uninstalling the experimental copy. **Do not uninstall your original app.**
