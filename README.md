# Darts Pen Overlay 1.4 — HONOR Magic V5

Android-app voor een **gewone capacitieve touchpen**, zonder speciale drivers of koppeling.

## Nieuw in v1.4
- **24 kleuren:** tik op **Kleuren** om het overzicht te openen en een kleur aan te tikken.
- **Versies bewaren:** tik op **Versies** → **+ Huidige lijnen bewaren**. Maximaal 12 snapshots per schermprofiel, met datum/tijd.
- **Versies terugzetten:** selecteer **Herstel**. De app maakt eerst automatisch een back-up van je huidige lijnen. Verwijderen via **×** naast de opgeslagen versie.
- **Gum voor finetuning:** tik op **Gum**, wis gericht stukjes van een lijn. Tik op de maatknop om te kiezen uit 4, 8, 16 of 32 dp.
- **Snelle sluitknop:** **×** rechtsboven de zwevende werkbalk stopt de overlay meteen. Ook via **Stop** in de Android-melding.
- **Ongedaan maken:** **↶** herstelt een vorige actie (tot 12 acties per schermprofiel, tijdens dezelfde sessie).
- **Meer lijndiktes:** 1, 2, 3, 4, 6, 8 of 12 dp.

## Overige functies
- Transparante lijnen boven Darts Club.
- Schakelaar **Tekenen / Spelen**. In speelmodus probeert Android aanrakingen door te geven aan de game.
- Rechte of vrije lijnen, automatisch opgeslagen bij het tekenen.
- Afzonderlijke profielen voor binnen-/buitenscherm en staand/liggend.
- Zwevende werkbalk verplaatsen via **↕**.

## APK downloaden
Open **Actions** > **Build Android APK** > de laatste geslaagde run. Download onder **Artifacts** `DartsPenOverlay-APK`, pak de ZIP uit en installeer `app-debug.apk` op je telefoon.

De APK is een **debug-build** voor handmatige installatie. GitHub bouwt deze met een tijdelijke debug-ondertekening per build. Als een eerdere versie al geïnstalleerd is, kan Android **'App niet geïnstalleerd'** melden omdat de handtekening verschilt. Om deze versie te installeren moet je dan mogelijk de vorige app verwijderen; **daarmee kunnen lokaal opgeslagen lijnen en versies verloren gaan**. Maak niet zomaar een uninstall als de lijnen belangrijk zijn.

Na installatie: Open Darts Pen Overlay, verleen toestemming voor **Over andere apps weergeven**, start de overlay en open Darts Club.

**Let op:** Android en sommige games kunnen aanraakdoorgifte door overlays blokkeren. Deze app kan dat niet omzeilen. Een gewone capacitieve pen wordt als vinger gezien. De app is nog niet op een Magic V5 getest. Versies zijn lokaal opgeslagen in de appgegevens en zijn geen export naar cloud of bestand.
