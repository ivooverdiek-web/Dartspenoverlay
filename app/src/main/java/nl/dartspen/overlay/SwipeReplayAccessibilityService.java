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
                          float snapStrength, Result result) {
        SwipeReplayAccessibilityService service = instance;
        if (service == null || points == null || points.size() < 2 ||
                w <= 0f || h <= 0f) {
            if (result != null) result.onComplete(false);
            return false;
        }

        // Straighten relative to the first and last touch positions, preserving
        // both endpoints. Blend the original travel with a distance-progressed
        // straight path so the throw keeps approximately its original pacing.
        // This affects the synthetic replay only, not live game input.
        float strength = Math.max(0f, Math.min(1f, snapStrength));
        PointF first = points.get(0);
        PointF last = points.get(points.size() - 1);
        float total = 0f;
        for (int i = 1; i < points.size(); i++) {
            PointF a = points.get(i - 1), b = points.get(i);
            total += (float)Math.hypot((b.x - a.x)*w, (b.y - a.y)*h);
        }

        Path path = new Path();
        path.moveTo(originX + first.x*w, originY + first.y*h);
        float traveled = 0f;
        for (int i = 1; i < points.size(); i++) {
            PointF p = points.get(i), prev = points.get(i - 1);
            traveled += (float)Math.hypot((p.x - prev.x)*w, (p.y - prev.y)*h);
            float fraction = total > 0f ? traveled/total : (float)i/(points.size() - 1);
            float lineX = first.x + (last.x - first.x)*fraction;
            float lineY = first.y + (last.y - first.y)*fraction;
            float x = p.x + strength*(lineX - p.x);
            float y = p.y + strength*(lineY - p.y);
            path.lineTo(originX + x*w, originY + y*h);
        }

        // 100% of a saved throw uses its original captured duration.
        // Slower replay can last longer; cap only at Android's actual gesture
        // duration limit (60 seconds on current Android), not at the old 1s limit.
        long duration = Math.max(25L,
                Math.min(GestureDescription.getMaxGestureDuration(), elapsedMs));
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
