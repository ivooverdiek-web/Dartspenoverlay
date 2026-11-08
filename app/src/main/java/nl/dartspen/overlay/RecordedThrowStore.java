package nl.dartspen.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores user-confirmed practice swipe paths for opt-in replay.
 *
 * Profiles keep folded/unfolded screen coordinates separate. These are raw
 * normalized finger points and their duration, not the extended guide-lines.
 * The snap strength used for the original captured throw is saved with it.
 */
final class RecordedThrowStore {
    static final class SavedThrow {
        final ArrayList<PointF> points;
        final long durationMs;
        final float snapStrength;

        SavedThrow(ArrayList<PointF> points, long durationMs, float snapStrength) {
            this.points = points;
            this.durationMs = durationMs;
            this.snapStrength = snapStrength;
        }
    }

    private final SharedPreferences prefs;

    RecordedThrowStore(Context context) {
        prefs = context.getSharedPreferences("saved_replay_throws_v1", Context.MODE_PRIVATE);
    }

    private String key(String profile, String target) {
        return "throw_" + profile + "_" + target;
    }

    boolean save(String profile, String target, List<PointF> points,
                 long durationMs, float snapStrength) {
        if (profile == null || profile.isEmpty() || target == null || target.isEmpty()
                || points == null || points.size() < 2 || points.size() > 200
                || durationMs < 25L || durationMs > 1400L) return false;
        try {
            JSONArray array = new JSONArray();
            for (PointF p : points) {
                if (p == null || !Float.isFinite(p.x) || !Float.isFinite(p.y)
                        || p.x < 0f || p.x > 1f || p.y < 0f || p.y > 1f) return false;
                JSONArray pair = new JSONArray();
                pair.put((double)p.x);
                pair.put((double)p.y);
                array.put(pair);
            }
            JSONObject json = new JSONObject();
            json.put("points", array);
            json.put("durationMs", durationMs);
            json.put("snapStrength", (double)Math.max(0f, Math.min(1f, snapStrength)));
            // commit() means saved is durable before the app tells the user it succeeded.
            return prefs.edit().putString(key(profile, target), json.toString()).commit();
        } catch (JSONException e) {
            return false;
        }
    }

    SavedThrow load(String profile, String target) {
        String raw = prefs.getString(key(profile, target), null);
        if (raw == null) return null;
        try {
            JSONObject json = new JSONObject(raw);
            JSONArray points = json.getJSONArray("points");
            if (points.length() < 2 || points.length() > 200) return null;
            long duration = json.getLong("durationMs");
            if (duration < 25L || duration > 1400L) return null;
            float snap = (float)json.getDouble("snapStrength");
            if (!Float.isFinite(snap) || snap < 0f || snap > 1f) return null;
            ArrayList<PointF> result = new ArrayList<>();
            for (int i = 0; i < points.length(); i++) {
                JSONArray pair = points.getJSONArray(i);
                float x = (float)pair.getDouble(0);
                float y = (float)pair.getDouble(1);
                if (!Float.isFinite(x) || !Float.isFinite(y)
                        || x < 0f || x > 1f || y < 0f || y > 1f) return null;
                result.add(new PointF(x,y));
            }
            return new SavedThrow(result, duration, snap);
        } catch (JSONException | RuntimeException e) {
            return null;
        }
    }
}
