# Darts Pen Overlay 1.8 — HONOR Magic V5

Een transparante richtlijn-overlay boven Darts Club, bedienbaar met een gewone capacitieve touchpen.

## Nieuw in 1.8: PRO-lijnen op dartbord uitgelijnd

De referentielijnen en gebogen pijlen uit de oorspronkelijke handgetekende topspeler-foto blijven behouden. Hun positie is opnieuw berekend met de **echte bull en buitenrand van het bord** uit de aangeleverde schone Darts Club-screenshot (685 x 1536).

- Kies rechts **PRO**. De complete PRO-preset verschijnt bij eerste gebruik automatisch.
- Als er al een PRO-preset uit versie 1.7 was, wordt deze **vóór de nieuwe uitlijning als versie bewaard**: onder **Versies** kun je **Back-up PRO vóór uitlijning** herstellen. Je T20-, D20- en andere lagen blijven ongewijzigd.
- Voor precieze afstelling: ga naar **Tekenen** en tik op **◎ PRO ijken** in de linkse werkbalk.
- Tik **(1)** precies op het midden van de bull en vervolgens **(2)** midden bovenaan op de **buitenste gouden bordrand**. De hele PRO-set verschuift en schaalt tegelijk naar die twee punten.
- Elke ijking bewaart eerst een terugzetbare versie van de vorige PRO-lijnen. Je hoeft niet alle pijlen afzonderlijk te verplaatsen.
- Daarna kun je nog steeds één pijl finetunen met **↔ Verplaats** en de kleine pijltjes.

De lijnen zijn gebaseerd op een handgetekende foto en de getekende bochten blijven interpretaties; de kalibratie maakt de **uitlijning met het bord** nauwkeuriger, maar garandeert geen optimale dartworp of exacte reproductie van de bewegingen. De preset heeft aparte opgeslagen gegevens voor ieder schermformaat.

## Nieuw in 1.7: PRO Bord-preset uit de gebruikersfoto

- Tik rechts op **PRO**, direct onder **Vrij**. Je krijgt één verzameling referentielijnen en gebogen pijlen voor het **hele bord**, geïnspireerd op de handgetekende foto.
- De foto is met de hand geïnterpreteerd; posities en curves zijn **benaderingen**, geen bewezen optimale worpen. De blauwe referentielijnen en gele pijlen zijn bewust contrastvol.
- De PRO-preset wordt bij het **eerste openen per schermstand** automatisch aangemaakt. Daarna blijven je aanpassingen lokaal bewaard. De bestaande T20-, D20- en andere lagen veranderen niet.
- Het programma bewaart automatisch een herstelbare versie **Origineel PRO Bord**. Gebruik **Versies** → **Herstel** om de oorspronkelijke schets terug te zetten.
- Gebruik **↔ Verplaats** om een pijl of referentielijn te verschuiven, en de pijltjes voor fijne correcties. Met **Gum** kun je losse stukken wissen en met **↶** ongedaan maken.
- **PRO** is een hele-bord laag; als je wisselt naar T20 of D20 verdwijnen alleen de PRO-lijnen uit beeld, ze worden niet gewist.
- De positie is geschaald naar de huidige schermafmetingen; de lay-out van Darts Club of de fotografische perspectiefvervorming kan om handmatige bijstelling vragen.

## Nieuw in versie 1.6: bestaande lijnen verplaatsen

Wanneer je bijvoorbeeld net te laag gooit, hoef je je T20-richtlijn **niet opnieuw te tekenen**:

1. Kies rechts bijvoorbeeld **T20**.
2. Ga naar **✎ Tekenen** en tik op **↔ Verplaats**.
3. De laatste lijn is alvast geselecteerd. Heb je meerdere lijnen, tik dan op de lijn die je wilt aanpassen; een witte omlijning toont welke geselecteerd is.
4. Sleep die lijn met je touchpen naar de juiste plaats, zonder de vorm/hoek te wijzigen.
5. Met **← ↑ ↓ →** verplaats je de geselecteerde lijn heel precies. Tik op de stapgrootte om te wisselen tussen **0,5 / 1 / 2 / 5 dp**.
6. Tik opnieuw op **✓ Verplaats** om verder te tekenen, of op **▶ Spelen** om te spelen.

**Opslag:** elke afgeronde sleepbeweging en iedere pijltjescorrectie worden onmiddellijk in het actieve doel opgeslagen. **↶** maakt de laatste correctie ongedaan. De verplaatste lijn blijft gekoppeld aan het huidige doel (T20, D20, enz.) en de huidige schermstand.

De lijn wordt bij het verplaatsen als geheel verschoven, inclusief beide uiteinden; het is **nog geen** aparte eindpunt- of hoekbewerking. De positie blijft binnen de schermranden.

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
