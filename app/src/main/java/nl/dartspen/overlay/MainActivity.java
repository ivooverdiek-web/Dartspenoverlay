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
                "13. PRO is nu uitgelijnd op de bull en het bord van jouw screenshot. Om alle PRO-lijnen nog nauwkeuriger te plaatsen: kies PRO > Tekenen > ◎ PRO ijken. Tik eerst midden op de bull en dan midden bovenaan op de buitenste gouden bordrand. Alle lijnen worden tegelijk uitgelijnd.\n\n" +
                "14. Je oude PRO-lijnen staan als back-up onder Versies en kunnen worden teruggezet.\n\n" +
                "15. Voor héél kleine foutjes: kies PRO, zet Tekenen aan en tik op ⊞ PRO fijn. Met ← ↑ ↓ → verschuif je ALLE PRO-pijlen tegelijk met 0,5, 1, 2 of 5 dp.\n\n" +
                "16. Gebruik −0,5% / +0,5% om het hele schema iets kleiner of groter te maken, en ↶/↷ om alles 0,25 graad te draaien.\n\n" +
                "17. Elke correctie wordt bewaard. Met ↶ in de hoofdbalk herstel je je vorige stap. Onder Versies blijft een back-up voor het fijnstellen beschikbaar.\n\n" +
                "18. Nieuw in 2.0: de PRO-lijnen zijn nu neonblauw en neongeel met een donkere contrastrand en zacht ogende gloed. Kies PRO > Tekenen > ✦ Glow om te wisselen tussen Uit, Laag, Normaal en Sterk. De instelling wordt onthouden.\\n\\n" +
                "19. Nieuw in 2.1: PRO-lijnen zijn veel dunner. Kies PRO > Tekenen > Lijn: ultradun om te wisselen tussen Ultradun, Dun en Normaal. Je opgeslagen pijlen en kalibratie blijven staan.\\n\\n" +
                "20. NIEUW 2.2 (BÈTA): Vang oefenworp. Ga naar Spelen > ◎ Vang worp. De eerste keer vraagt Android dat je handmatig Darts Pen Overlay als Toegankelijkheidsdienst inschakelt. De app vangt dan jouw swipe op de overlay, speelt hem daarna als Android-gebaar na in de game, en laat jou met ★ Bewaar beslissen of die geslaagd was. Tik × Overslaan bij een misser.\\n\\n" +
                "21. BELANGRIJK: Dit is een experimentele naspeelfunctie, geen automatische registratie van echte aanrakingen in Darts Club. Het spel kan nagemaakte vegen anders behandelen. Gewoon Spelen blijft onveranderd en vereist géén toegankelijkheidstoestemming.\\n\\n" +
                "22. NIEUW 2.3: Opgenomen swipe-lijnen worden standaard VIER keer langer bewaard. In Spelen tik je op ↗ Lengte ×4 om te kiezen voor 1× / 2× / 3× / 4× / 6× / 8×. Pas met Lijndikte de dikte van de opgenomen lijn aan. Je ziet eerst een voorbeeld en bewaart alleen met ★ Bewaar.\\n\\n" +
                "23. Tijdens de swipe toont de overlay alvast een dun groen spoor. De echte dart verschijnt pas zodra Android de veeg na je loslaten heeft nagespeeld; dat is een beperking van de oefenmodus.\\n\\n" +
                "24. NIEUW 2.6: Schermzoom is helemaal verwijderd. De bestaande PRO-richtlijnen zijn standaard veel dikker en duidelijker: 3,25 dp (Dik), 4,75 dp (Extra) of 6,5 dp (Max). Kies PRO > Tekenen > Lijn om de weergave te veranderen.\\n\\n" +
                "25. NIEUW 2.7: In PRO worden de blauwe referentielijnen verborgen. Je ziet alleen de gele richtlijnen (plus eventueel zelf opgeslagen groene oefenworpen). Alle gele richtlijnen eindigen als gewone lijnen, zonder pijlpunt of begindot. Bestaande PRO-ijkpunten en opgeslagen lijnen blijven in de opslag aanwezig.\\n\\n" +
                "26. Druk op − om het hele menu inclusief de doellijst te minimaliseren. Tik op de kleine 🎯 knop om alles weer te openen. De menustand wordt onthouden.\\n\\n" +
                "27. NIEUW 2.8: Test PRO-lijnen op VINGER-dikte. De eerste keer na deze update verschijnen de gele PRO-lijnen met maar liefst 48 dp dikte (ongeveer een vingerbreedte). Open PRO > Tekenen > Lijn: VINGER 48 om per tik te wisselen: 3,25 / 4,75 / 6,5 / 12 / 24 / 48 dp. Het zijn puur zichtbare diktes; de lijnen en hun kalibratie verplaatsen niet.\\n\\n" +
                "28. Blauwe PRO-lijnen blijven verborgen, de gele lijnen hebben geen pijlpunten. Druk op − om het menu te verkleinen en tik 🎯 om het terug te halen.\\n\\n" +
                "29. Binnen-/buitenscherm, staand/liggend en ieder doel hebben eigen opgeslagen lijnen en versies.",
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
