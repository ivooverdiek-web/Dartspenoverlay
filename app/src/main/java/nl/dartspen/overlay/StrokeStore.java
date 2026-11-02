package nl.dartspen.overlay;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PointF;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class StrokeStore {
    static final class Stroke {
        int color;
        float widthDp;
        boolean straight;
        final ArrayList<PointF> points = new ArrayList<>();
        Stroke(int color, float widthDp, boolean straight) {
            this.color = color;
            this.widthDp = widthDp;
            this.straight = straight;
        }
    }
    private final SharedPreferences prefs;
    StrokeStore(Context context) {
        prefs = context.getSharedPreferences("saved_overlay_lines_v1", Context.MODE_PRIVATE);
    }
    int colorIndex() { return prefs.getInt("color", 0); }
    void colorIndex(int n) { prefs.edit().putInt("color", n).apply(); }
    int widthIndex() { return prefs.getInt("width", 1); }
    void widthIndex(int n) { prefs.edit().putInt("width", n).apply(); }
    boolean straight() { return prefs.getBoolean("straight", true); }
    void straight(boolean value) { prefs.edit().putBoolean("straight", value).apply(); }

    List<Stroke> load(String profile) {
        String key = "strokes_" + profile;
        if (!prefs.contains(key) && profile.startsWith("cover_")) {
            String oldKey = "strokes_" + (profile.endsWith("portrait") ? "portrait" : "landscape");
            if (prefs.contains(oldKey)) {
                prefs.edit().putString(key, prefs.getString(oldKey, "[]")).commit();
            }
        }
        ArrayList<Stroke> result = new ArrayList<>();
        try {
            JSONArray lines = new JSONArray(prefs.getString(key, "[]"));
            for (int i = 0; i < Math.min(lines.length(), 500); i++) {
                JSONObject obj = lines.optJSONObject(i);
                if (obj == null) continue;
                Stroke stroke = new Stroke(obj.optInt("color", 0xFF22D3EE),
                        (float)Math.max(1, Math.min(20, obj.optDouble("widthDp", 4))),
                        obj.optBoolean("straight", true));
                JSONArray pts = obj.optJSONArray("points");
                if (pts == null) continue;
                for (int j = 0; j < Math.min(pts.length(), 4000); j++) {
                    JSONArray p = pts.optJSONArray(j);
                    if (p == null || p.length() < 2) continue;
                    double x = p.optDouble(0, -1), y = p.optDouble(1, -1);
                    if (Double.isFinite(x) && Double.isFinite(y) &&
                            x >= 0 && x <= 1 && y >= 0 && y <= 1) {
                        stroke.points.add(new PointF((float)x, (float)y));
                    }
                }
                if (!stroke.points.isEmpty()) result.add(stroke);
            }
        } catch (org.json.JSONException | RuntimeException ignored) { }
        return result;
    }
    void save(String profile, List<Stroke> strokes) {
        JSONArray lines = new JSONArray();
        for (int i = Math.max(0, strokes.size() - 500); i < strokes.size(); i++) {
            Stroke stroke = strokes.get(i);
            try {
                JSONObject obj = new JSONObject();
                obj.put("color", stroke.color);
                obj.put("widthDp", stroke.widthDp);
                obj.put("straight", stroke.straight);
                JSONArray points = new JSONArray();
                for (PointF p : stroke.points) {
                    points.put(new JSONArray().put(p.x).put(p.y));
                }
                obj.put("points", points);
                lines.put(obj);
            } catch (org.json.JSONException ignored) { }
        }
        prefs.edit().putString("strokes_" + profile, lines.toString()).commit();
    }
}
