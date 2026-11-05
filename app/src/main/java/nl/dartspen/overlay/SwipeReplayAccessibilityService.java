package nl.dartspen.overlay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
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
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
