# Darts Pen Overlay 1.5 — HONOR Magic V5

Een transparante richtlijn-overlay boven Darts Club, bedienbaar met een gewone capacitieve touchpen.

## Nieuw: doelbalk rechts
Er staat nu een smalle, verticaal scrollbare balk aan de **rechterkant** van je scherm. Je kunt er rechtstreeks op tikken, ook terwijl de app in **Spelen** staat.

Doelen, in volgorde:
- **Vrij** (bestaande algemene lijnen uit versie 1.4)
- **Triples:** T20, T19, T18, T17, T16, T15, T14, T13
- **Dubbels:** D20, D10, D17, D16, D8
- **Bull**

**Zo gebruik je de doelen:** tik T20 > ga indien nodig naar **Tekenen** > teken één keer je gewenste lijn(en) > kies **Spelen**. Tik vervolgens op T19 of D20 om uitsluitend de bijbehorende opgeslagen lijnen te zien. Als er nog geen lijnen voor een doel bestaan, is de overlay daar leeg: de app berekent dus *geen* automatische richtingslijn naar de spelkaart.

Elk doel heeft een eigen geheugen binnen elk schermprofiel (binnenscherm/buitenscherm, staand/liggend). Bij het wisselen van doel verdwijnen de lijnen van het vorige doel uit beeld, maar ze blijven opgeslagen. Gebruik **≡** boven de balk om hem in/uit te klappen; de balk is scrollbaar.

De knoppen **Versies**, **Gum**, **↶** en **Kleuren** werken op het geselecteerde doel. De oorspronkelijke vrije lijnen zijn terug te vinden onder **Vrij**.

## Andere functies
- 24 kleuren, verstelbare lijndiktes en precieze gum.
- Maximaal 12 opgeslagen versies per doel en schermprofiel, met herstel en reservekopie.
- Zwevende, verplaatsbare werkbalk links; rechts de doelen.
- **▶ Spelen** laat aanrakingen waar mogelijk door naar Darts Club.
- **×** sluit onmiddellijk af.
- Werkt met een gewone touchpen zonder Bluetooth.

## APK bouwen en downloaden
Open **Actions** > **Build Android APK** > de meest recente geslaagde run. Download onder **Artifacts** het bestand `DartsPenOverlay-APK`, pak uit en installeer `app-debug.apk`.

De GitHub Actions-workflow maakt een **debug-APK**. Een debug-ondertekening kan per run verschillen. Android kan daarom bij een update naar een andere build weigeren de APK over de oude versie te installeren. **Verwijder de vorige app niet voordat je zeker weet dat je oude opgeslagen lijnen niet nodig hebt:** bij verwijderen gaan de lokaal opgeslagen appgegevens doorgaans verloren. De versie-opslag is lokaal, geen cloudbackup.

**Let op:** Android of de game kunnen in speelmodus aanraakdoorgifte door overlays beperken. Dit kan de app niet omzeilen. De app is wel gecompileerd via GitHub Actions, maar niet daadwerkelijk getest op de HONOR Magic V5.
