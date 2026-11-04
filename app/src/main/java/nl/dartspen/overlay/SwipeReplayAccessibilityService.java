package nl.dartspen.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.MagnificationConfig;
import android.content.pm.PackageManager;
import android.os.Build;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import java.util.List;

/**
 * Opt-in accessibility gesture player used ONLY after a swipe is explicitly
 * captured in Practice Capture mode. No screen contents or global touches
 * are monitored. The user must enable this service manually in Settings.
 *
 * Important: Android synthesizes the gesture after the real finger lifts.
 * Games may treat the replay differently from a direct touch.
 */
public final class SwipeReplayAccessibilityService extends AccessibilityService {
    interface Result { void onComplete(boolean dispatched); }
    private static volatile SwipeReplayAccessibilityService instance;
    private static MagnificationConfig zoomBeforeApp;
    private static boolean appOwnsZoom;

    /**
     * Window magnification is a platform feature (API 33+). Magnification is
     * managed by Android, so the app never captures or re-renders game pixels.
     * Unsupported devices are deliberately NOT switched to fullscreen zoom.
     */
    static boolean supportsBoardZoom() {
        SwipeReplayAccessibilityService service = instance;
        return service != null && Build.VERSION.SDK_INT >= 33 &&
                service.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_WINDOW_MAGNIFICATION);
    }

    static boolean setBoardZoom(float factor, float centerX, float centerY) {
        SwipeReplayAccessibilityService service = instance;
        if (!supportsBoardZoom() || factor < 1.1f || factor > 3f ||
                !Float.isFinite(centerX) || !Float.isFinite(centerY)) {
            return false;
        }
        try {
            MagnificationController controller = service.getMagnificationController();
            if (controller == null) return false;
            MagnificationConfig prior = controller.getMagnificationConfig();
            MagnificationConfig.Builder builder = new MagnificationConfig.Builder()
                    .setMode(MagnificationConfig.MAGNIFICATION_MODE_WINDOW)
                    .setScale(factor)
                    .setCenterX(centerX)
                    .setCenterY(centerY);
            if (Build.VERSION.SDK_INT >= 34) builder.setActivated(true);
            boolean applied = controller.setMagnificationConfig(builder.build(), true);
            if (applied) {
                // Remember the previous system setting only on first activation.
                if (!appOwnsZoom) {
                    zoomBeforeApp = prior;
                }
                appOwnsZoom = true;
            }
            return applied;
        } catch (SecurityException | IllegalArgumentException | IllegalStateException e) {
            return false;
        }
    }

    static void clearBoardZoom() {
        SwipeReplayAccessibilityService service = instance;
        if (!appOwnsZoom || service == null) return;
        try {
            MagnificationController controller = service.getMagnificationController();
            if (controller != null) {
                // Return control to the user's preexisting magnifier setting,
                // rather than disabling their own accessibility zoom.
                if (zoomBeforeApp != null) {
                    if (!controller.setMagnificationConfig(zoomBeforeApp, false)) {
                        controller.resetCurrentMagnification(false);
                    }
                } else {
                    controller.resetCurrentMagnification(false);
                }
            }
        } catch (RuntimeException ignored) { }
        finally {
            appOwnsZoom = false;
            zoomBeforeApp = null;
        }
    }

    static boolean isAvailable() { return instance != null; }

    static boolean replay(List<PointF> points, float w, float h,
                          float originX, float originY, long elapsedMs,
                          Result result) {
        SwipeReplayAccessibilityService service = instance;
        if (service == null || points == null || points.size() < 2 ||
                w <= 0f || h <= 0f) {
            if (result != null) result.onComplete(false);
            return false;
        }

        Path path = new Path();
        PointF first = points.get(0);
        path.moveTo(originX + first.x*w, originY + first.y*h);
        for (int i=1; i<points.size(); i++) {
            PointF p = points.get(i);
            path.lineTo(originX + p.x*w, originY + p.y*h);
        }

        long duration = Math.max(45L, Math.min(1000L, elapsedMs));
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0L, duration))
                .build();
        try {
            return service.dispatchGesture(gesture,
                    new GestureResultCallback() {
                        @Override public void onCompleted(GestureDescription description) {
                            if (result != null) result.onComplete(true);
                        }
                        @Override public void onCancelled(GestureDescription description) {
                            if (result != null) result.onComplete(false);
                        }
                    }, new Handler(Looper.getMainLooper()));
        } catch (RuntimeException e) {
            if (result != null) result.onComplete(false);
            return false;
        }
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // No inspection, recording, or collection of other apps' UI events.
    }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        if (instance == this) {
            clearBoardZoom();
            instance = null;
        }
        super.onDestroy();
    }
}
