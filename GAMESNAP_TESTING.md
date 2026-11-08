# Experimental angle snapping for Darts Club

This branch builds a separate Android app named **Darts Pen Snap (Test)** using the
application ID `nl.dartspen.overlay.gamesnap`. It can be installed *alongside*
your existing **Darts Pen Overlay** without overwriting the original app's
saved PRO lines or calibration. However, it starts with its own separate settings.



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
