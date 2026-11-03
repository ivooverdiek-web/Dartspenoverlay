package nl.dartspen.overlay;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF0F172A);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(35), dp(22), dp(30));
        scroll.addView(root);

        TextView title = text("🎯 Darts Pen Overlay", 25, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Voor HONOR Magic V5 en een gewone touchpen — zonder Bluetooth of speciale software.", 16, 0xFFB8C6D8));
        status = text("", 16, 0xFF22D3EE);
        root.addView(status);
        Button start = button("Overlay starten", 0xFF0891B2);
        root.addView(start);
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(permission);
                return;
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
            startOverlay();
        });
        Button stop = button("Overlay stoppen", 0xFF475569);
        root.addView(stop);
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, OverlayService.class));
            status.setText("Overlay gestopt.");
        });
        root.addView(text("GEBRUIK", 18, 0xFF22D3EE));
        root.addView(text("1. Start de overlay en open Darts Club.\n\n" +
                "2. Teken met je eigen touchpen. Kies Rechte lijn of Vrij tekenen.\n\n" +
                "3. Tik op ▶ Spelen. De lijnen blijven zichtbaar; je pen bedient de game.\n\n" +
                "4. Via Kleuren kies je uit 24 kleuren. Met Gum en de maatknop wis je fijne stukjes van lijnen. ↶ maakt een wijziging ongedaan.\n\n" +
                "5. Onder Versies kun je lijnen bewaren, herstellen of een versie verwijderen. De app maakt voor herstel automatisch een back-up.\n\n" +
                "6. Met × sluit je de overlay direct. Wis (lang indrukken) verwijdert alle actieve lijnen.\n\n" +
                "7. Tik rechts op T20, T19, D20, D10, D17, D16, D8 of Bull: iedere knop toont uitsluitend zijn eigen opgeslagen richtlijnen.\n\n" +
                "8. Kies rechts een doel, teken daar eenmaal de richtlijn voor en tik op ▶ Spelen. Met een tik op een andere doelknop wissel je de zichtbare lijnen zonder opnieuw te tekenen.\n\n" +
                "9. Rechtsboven op ≡ kun je de doelbalk in- en uitklappen. De stand Vrij houdt je bestaande losse lijnen.\n\n" +
                "10. Wil je een lijn corrigeren? Tik in Tekenstand op ↔ Verplaats, tik op de bestaande lijn en sleep hem naar de juiste plaats. Gebruik daarna ← ↑ ↓ → voor nauwkeurig bijstellen.\n\n" +
                "11. Tik op de stapknop (0,5 / 1 / 2 / 5 dp) om de pijltjes preciezer of sneller te maken. Met ↶ kun je een correctie terugdraaien.\n\n" +
                "12. Tik op PRO (bovenaan rechts) om de hele-bord pijlen uit de aangeleverde foto te bekijken. Dit is een benadering van de schets; verplaats de pijlen om ze voor jouw scherm te kalibreren.\n\n" +
                "13. Binnen-/buitenscherm, staand/liggend en ieder doel hebben eigen opgeslagen lijnen en versies.",
                16, Color.WHITE));
        root.addView(text("Let op: bij sommige games blokkeert Android aanrakingen door een overlay. Een gewone capacitieve pen wordt als vinger herkend.", 14, 0xFF9CA3AF));
        setContentView(scroll);
    }

    @Override public void onResume() {
        super.onResume();
        if (status != null) {
            status.setText(Settings.canDrawOverlays(this)
                    ? "Overlaytoestemming aanwezig. Tik op Overlay starten."
                    : "Toestemming nodig: over andere apps weergeven.");
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 1) startOverlay();
    }

    private void startOverlay() {
        if (!Settings.canDrawOverlays(this)) return;
        try {
            startForegroundService(new Intent(this, OverlayService.class));
            status.setText("Overlay gestart. Open nu Darts Club.");
        } catch (RuntimeException e) {
            Toast.makeText(this, "Kon overlay niet starten. Controleer toestemmingen.", Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String value, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setPadding(0, 0, 0, dp(22));
        return t;
    }

    private Button button(String value, int color) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTextSize(17);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(58));
        p.bottomMargin = dp(12);
        b.setLayoutParams(p);
        b.setGravity(Gravity.CENTER);
        return b;
    }
    private int dp(float x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }
}
