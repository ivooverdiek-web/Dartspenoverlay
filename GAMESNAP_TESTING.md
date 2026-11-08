# Experimental angle snapping for Darts Club

This branch builds a separate Android app named **Darts Pen Snap (Test)** using the
application ID `nl.dartspen.overlay.gamesnap`. It can be installed *alongside*
your existing **Darts Pen Overlay** without overwriting the original app's
saved PRO lines or calibration. However, it starts with its own separate settings.

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
