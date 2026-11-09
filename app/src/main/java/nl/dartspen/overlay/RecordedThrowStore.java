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
        // null for legacy recordings, which remain valid and replay uniformly.
        final ArrayList<Long> pointTimesMs;

        SavedThrow(ArrayList<PointF> points, long durationMs, float snapStrength,
                   ArrayList<Long> pointTimesMs) {
            this.points = points;
            this.durationMs = durationMs;
            this.snapStrength = snapStrength;
            this.pointTimesMs = pointTimesMs;
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
        return save(profile, target, points, durationMs, snapStrength, null);
    }

    boolean save(String profile, String target, List<PointF> points,
                 long durationMs, float snapStrength, List<Long> times) {
        return write(profile, target, points, durationMs, snapStrength, times, false);
    }

    boolean saveEdited(String profile, String target, List<PointF> points,
                       long durationMs, float snapStrength) {
        return saveEdited(profile, target, points, durationMs, snapStrength, null);
    }

    boolean saveEdited(String profile, String target, List<PointF> points,
                       long durationMs, float snapStrength, List<Long> times) {
        return write(profile, target, points, durationMs, snapStrength, times, true);
    }

    private boolean write(String profile, String target, List<PointF> points,
                          long durationMs, float snapStrength, List<Long> times,
                          boolean preservePrevious) {
        if (profile == null || profile.isEmpty() || target == null || target.isEmpty()
                || points == null || points.size() < 2 || points.size() > 200
                || durationMs < 25L || durationMs > 30000L) return false;
        if (times != null) {
            if (times.size() != points.size() || times.get(0) != 0L) return false;
            long previous = -1L;
            for (Long value : times) {
                if (value == null || value < previous || value > durationMs) return false;
                previous = value;
            }
        }
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
            if (times != null) {
                JSONArray timesJson = new JSONArray();
                for (Long time : times) timesJson.put(time.longValue());
                json.put("pointTimesMs", timesJson);
            }
            // Save the previous version of only this target atomically with the edit.
            SharedPreferences.Editor editor = prefs.edit();
            String storageKey = key(profile, target);
            if (preservePrevious) {
                String previous = prefs.getString(storageKey, null);
                if (previous != null) editor.putString("backup_" + storageKey, previous);
            }
            return editor.putString(storageKey, json.toString()).commit();
        } catch (JSONException e) {
            return false;
        }
    }

    SavedThrow load(String profile, String target) {
        return parse(prefs.getString(key(profile, target), null));
    }

    SavedThrow loadPrevious(String profile, String target) {
        return parse(prefs.getString("backup_" + key(profile, target), null));
    }

    /** Swap the backup with the current saved throw, so the action is reversible. */
    boolean restorePrevious(String profile, String target) {
        String currentKey = key(profile, target);
        String previous = prefs.getString("backup_" + currentKey, null);
        String current = prefs.getString(currentKey, null);
        if (parse(previous) == null || parse(current) == null) return false;
        return prefs.edit().putString(currentKey, previous)
                .putString("backup_" + currentKey, current).commit();
    }

    private SavedThrow parse(String raw) {
        if (raw == null) return null;
        try {
            JSONObject json = new JSONObject(raw);
            JSONArray points = json.getJSONArray("points");
            if (points.length() < 2 || points.length() > 200) return null;
            long duration = json.getLong("durationMs");
            if (duration < 25L || duration > 30000L) return null;
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
            ArrayList<Long> times = null;
            JSONArray savedTimes = json.optJSONArray("pointTimesMs");
            if (savedTimes != null) {
                if (savedTimes.length() != result.size()) return null;
                times = new ArrayList<>();
                long prior = -1L;
                for (int i = 0; i < savedTimes.length(); i++) {
                    long at = savedTimes.getLong(i);
                    if (at < prior || at > duration) return null;
                    times.add(at);
                    prior = at;
                }
                if (times.get(0) != 0L) return null;
            }
            return new SavedThrow(result, duration, snap, times);
        } catch (JSONException | RuntimeException e) {
            return null;
        }
    }
}
