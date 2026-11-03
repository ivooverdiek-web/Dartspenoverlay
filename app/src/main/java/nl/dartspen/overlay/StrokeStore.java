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
        boolean arrow = false;
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

    // Quick-dart targets are independent drawing layers for each screen profile.
    // The "Vrij" layer uses the original storage key for backwards compatibility.
    static final String[] TARGETS = {
            "Vrij", "T20", "T19", "T18", "T17", "T16", "T15",
            "T14", "T13", "D20", "D10", "D17", "D16", "D8", "Bull", "PRO"
    };

    static String layerProfile(String screenProfile, String target) {
        if (screenProfile == null || screenProfile.isEmpty()) return "";
        return "Vrij".equals(target) ? screenProfile : screenProfile + "__" + target;
    }

    String selectedTarget(String screenProfile) {
        String saved = prefs.getString("dart_target_" + screenProfile, "Vrij");
        for (String choice : TARGETS) {
            if (choice.equals(saved)) return saved;
        }
        return "Vrij";
    }

    void selectedTarget(String screenProfile, String target) {
        if (screenProfile == null || screenProfile.isEmpty()) return;
        for (String choice : TARGETS) {
            if (choice.equals(target)) {
                prefs.edit().putString("dart_target_" + screenProfile, target).apply();
                break;
            }
        }
    }


    static final class Version {
        final String id;
        final String label;
        Version(String id, String label) { this.id = id; this.label = label; }
    }
    private static final int MAX_VERSIONS = 12;

    private JSONArray versionArray(String profile) {
        try { return new JSONArray(prefs.getString("versions_" + profile, "[]")); }
        catch (org.json.JSONException ignored) { return new JSONArray(); }
    }

    List<Version> listVersions(String profile) {
        ArrayList<Version> versions = new ArrayList<>();
        JSONArray entries = versionArray(profile);
        for (int i = 0; i < entries.length(); i++) {
            JSONObject item = entries.optJSONObject(i);
            if (item != null) versions.add(new Version(item.optString("id", ""),
                    item.optString("label", "Versie")));
        }
        return versions;
    }

    boolean saveVersion(String profile, String title) {
        if (profile == null || profile.isEmpty()) return false;
        JSONArray previous = versionArray(profile);
        JSONArray next = new JSONArray();
        long id = System.currentTimeMillis();
        int seq = prefs.getInt("version_seq_" + profile, 0) + 1;
        String time = new java.text.SimpleDateFormat("dd-MM HH:mm",
                java.util.Locale.forLanguageTag("nl-NL")).format(new java.util.Date(id));
        try {
            JSONObject item = new JSONObject();
            item.put("id", Long.toString(id) + "-" + seq);
            item.put("label", title == null ? "Versie " + seq + " · " + time : title + " · " + time);
            item.put("data", prefs.getString("strokes_" + profile, "[]"));
            next.put(item);
            for (int i = 0; i < previous.length() && next.length() < MAX_VERSIONS; i++) {
                JSONObject old = previous.optJSONObject(i);
                if (old != null) next.put(old);
            }
            return prefs.edit().putInt("version_seq_" + profile, seq)
                    .putString("versions_" + profile, next.toString()).commit();
        } catch (org.json.JSONException e) { return false; }
    }

    boolean restoreVersion(String profile, String id) {
        JSONArray versions = versionArray(profile);
        for (int i = 0; i < versions.length(); i++) {
            JSONObject version = versions.optJSONObject(i);
            if (version != null && id.equals(version.optString("id"))) {
                String data = version.optString("data", "[]");
                // Auto-backup so restoring the wrong version is reversible.
                if (!saveVersion(profile, "Back-up voor herstel")) return false;
                return prefs.edit().putString("strokes_" + profile, data).commit();
            }
        }
        return false;
    }

    boolean deleteVersion(String profile, String id) {
        JSONArray previous = versionArray(profile);
        JSONArray next = new JSONArray();
        boolean found = false;
        for (int i = 0; i < previous.length(); i++) {
            JSONObject item = previous.optJSONObject(i);
            if (item == null) continue;
            if (id.equals(item.optString("id"))) found = true;
            else next.put(item);
        }
        return found && prefs.edit().putString("versions_" + profile, next.toString()).commit();
    }


    // Manual approximation of the supplied pro-player whole-board sketch.
    // Photo coordinates refer to the screen in the user's reference image.
    // Normalized values allow the sketch to work across different screen sizes.
    private static PointF sketchPoint(float photoX, float photoY) {
        return new PointF(
                Math.max(0f, Math.min(1f, (photoX - 160f) / 625f)),
                Math.max(0f, Math.min(1f, (photoY - 205f) / 1160f)));
    }
    private static void path(List<Stroke> strokes, int color,
                             float width, boolean arrow, float... coordinates) {
        if (coordinates.length < 4 || coordinates.length % 2 != 0) return;
        Stroke stroke = new Stroke(color, width, false);
        stroke.arrow = arrow;
        for (int i = 0; i < coordinates.length; i += 2) {
            stroke.points.add(sketchPoint(coordinates[i], coordinates[i+1]));
        }
        strokes.add(stroke);
    }
    private static List<Stroke> createProSketch() {
        ArrayList<Stroke> lines = new ArrayList<>();
        // Central cross + upper and lower alignment rules.
        int guide = 0xFF8EE7FF;  // light cyan, better visible than black in the game
        int arrow = 0xFFFFCF57;  // gold curved arrows
        path(lines, guide, 1.5f, false, 458,496, 462,610, 464,739, 467,906, 468,1040);
        path(lines, guide, 1.5f, false, 178,736, 332,734, 468,733, 625,731, 742,732);
        path(lines, guide, 1.2f, false, 196,518, 343,518, 464,518, 611,517, 775,516);
        path(lines, guide, 1.2f, false, 310,990, 462,990, 606,987, 779,984);
        // Upper sector: arrows curve toward the top line or the central column.
        path(lines, arrow, 2.7f, true, 391,560, 400,542, 427,527, 450,524, 466,519);
        path(lines, arrow, 2.7f, true, 542,593, 511,584, 489,566, 477,538, 463,520);
        path(lines, arrow, 2.7f, true, 384,688, 387,655, 403,630, 423,617, 449,609);
        path(lines, arrow, 2.7f, true, 548,677, 522,670, 493,654, 475,629, 470,607);
        path(lines, arrow, 2.7f, true, 627,707, 609,686, 584,666, 555,635);
        path(lines, arrow, 2.7f, true, 733,732, 711,696, 688,659, 661,618);
        path(lines, arrow, 2.7f, true, 302,761, 308,728, 320,687, 345,648);
        // Midboard towards central intersection.
        path(lines, arrow, 2.8f, true, 396,846, 413,815, 434,780, 460,737);
        path(lines, arrow, 2.8f, true, 533,844, 523,805, 506,767, 473,736);
        // Lower sector: starts are the round pen-down dots in the photo.
        path(lines, arrow, 2.7f, true, 318,1020, 305,977, 287,926, 275,895);
        path(lines, arrow, 2.7f, true, 452,981, 416,957, 386,913, 357,866, 357,834);
        path(lines, arrow, 2.7f, true, 497,971, 478,972, 443,952, 427,911, 422,878);
        path(lines, arrow, 2.7f, true, 513,958, 533,943, 553,896, 567,832);
        path(lines, arrow, 2.7f, true, 563,906, 576,887, 590,849, 600,797);
        path(lines, arrow, 2.7f, true, 626,961, 645,915, 664,870, 675,839);
        path(lines, arrow, 2.7f, true, 473,1074, 494,1043, 511,1014, 530,978);
        path(lines, arrow, 2.7f, true, 398,844, 422,812, 439,776, 463,739);
        return lines;
    }

    private void initializeProPreset(String profile) {
        if (profile == null || !profile.endsWith("__PRO")) return;
        String key = "strokes_" + profile;
        String marker = "pro_preset_initialized_" + profile;
        if (prefs.getBoolean(marker, false)) return;
        // A profile already containing user data is never overwritten.
        if (prefs.contains(key)) {
            prefs.edit().putBoolean(marker, true).commit();
            return;
        }
        save(profile, createProSketch());
        // Keep a restorable original even if the sketch is subsequently adjusted.
        saveVersion(profile, "Origineel PRO Bord");
        prefs.edit().putBoolean(marker, true).commit();
    }

    List<Stroke> load(String profile) {
        initializeProPreset(profile);
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
                stroke.arrow = obj.optBoolean("arrow", false);
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
                obj.put("arrow", stroke.arrow);
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
