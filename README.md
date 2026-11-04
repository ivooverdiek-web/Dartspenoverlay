# Darts Pen Overlay 2.5 — HONOR Magic V5

Een transparante richtlijn-overlay boven Darts Club, bedienbaar met een gewone capacitieve touchpen.

## Nieuw in 2.5: volledige schermzoom + minimaliseerbare menubalk

De zoom is gewijzigd van **venstervergroting** naar Androids **volledige schermvergroting**. Hiermee is er geen apart klein vergrootvenster meer: het hele scherm wordt vergroot rond je eerder gekalibreerde PRO-bullcentrum.

- **Spelen → 🔍 Schermzoom UIT:** tik om te schakelen tussen **1,25× → 1,5× → 2× → 2,5× → UIT**.
- **◎ Bull midden** centreert opnieuw op je bestaande PRO-kalibratie.
- Zodra je een zoomniveau kiest, verdwijnt het grote menu én de doellijst rechts. Er blijft een kleine zwevende **🎯-knop** over. Tik op de knop om de bediening weer te openen en indien gewenst zoom uit te zetten.
- Ook zonder zoom kun je altijd op **−** in de bovenste menubalk tikken om het menu en de rechter doellijst samen te minimaliseren. Tik **🎯** om terug te keren. De gekozen minimalisatiestand wordt lokaal onthouden.
- **Belangrijk:** volledige schermzoom betekent dat *alles wat op het scherm staat* groter wordt. De rand van het dartbord of score-informatie kan buiten het zichtbare scherm vallen en de gamefysica wordt niet gewijzigd. Gebruik 1,25× of 1,5× voor het beste overzicht.
- De zoom is **alleen beschikbaar in gewone Spelen-modus**, niet tijdens PRO-kalibratie, lijnen tekenen of oefenworpen opnemen. Bij overstappen naar die modi wordt de zoom uitgeschakeld.
- Androids eigen Accessibility-vergroting regelt de weergave en touchtransformatie. Deze app heeft optionele toegankelijkheidstoestemming nodig om de zoom te bedienen. Op sommige HONOR-versies is systeemvergroting niet toegankelijk voor externe apps.
- De app legt geen beelden van Darts Club vast. Bestaande PRO-lijnen, presets, glow, kalibratie, opgeslagen oefenworpen en versies blijven onaangetast wanneer je direct over 2.4 installeert.

 alleen het dartbord vergroten (bordzoom)

Deze functie maakt **een apart venster boven het bestaande dartbord** met Androids eigen **venstervergroting**, geen screenshots of vertraagde videobeelden. De rest van het scherm (zoals score en knoppen buiten het venster) blijft normaal. Android handelt de vergrote invoer voor dit systeemvenster zelf af.

### Gebruik

1. Installeer 2.4 over 2.3: verwijder de vorige app **niet**, zodat alle opgeslagen PRO-lijnen, ijkpunten, oefenworpen, back-ups, glow-instellingen en individuele doel-presets behouden blijven.
2. Start de overlay en open Darts Club. Zet de overlay op **Spelen**. Niet in **Vang worp**: bordzoom is juist bedoeld om in het gewone, direct bestuurbare spel te gebruiken.
3. Tik op **🔍 Bordzoom UIT** om de vergroting op **1,5×** te starten. Tik nogmaals voor **2×**, nogmaals voor **2,5×**, nogmaals voor **UIT**. Er staat ook **◎ Bull midden** om op jouw opgeslagen PRO-bullcentrum te hercentreren.
4. Als dit de eerste keer is dat de app Android-vergroting wil bedienen, vraagt de app je naar **Instellingen → Toegankelijkheid**. Schakel de optionele dienst **Darts Pen Overlay** in (of schakel hem na een update even uit en weer aan) en probeer de knop opnieuw.
5. Pas de grootte/positie van het **Android-vergrootvenster** desgewenst aan met zijn systeemgrepen. Bordzoom schakelt zichzelf uit als je naar **Tekenen** of **Vang worp** gaat, en wordt ook opgeruimd als de overlay stopt.

### Beperkingen

- Dit is een **bèta die nog niet op de HONOR Magic V5 is getest**. Android moet versie 13+ én de systeemfunctie `android.software.window_magnification` ondersteunen. HONOR kan deze functie wel voor de systeeminstellingen aanbieden maar niet aan externe toegankelijkheidsdiensten beschikbaar maken.
- Als de app meldt dat deze functie niet beschikbaar is, gebruik dan **HONOR → Instellingen → Toegankelijkheidsfuncties → Toegankelijkheid → Vergroting**, en kies de gedeeltelijke/venstervergroting. Daarmee kun je hetzelfde effect handmatig testen zonder nieuwe appmachtigingen.
- Het vergrootvenster toont slechts een deel van het bord. **Dit verandert de grootte of fysica van de Darts Club-game zelf niet**. Het kan helpen bij het zien van details, maar het zoomniveau en de venstergrootte worden door Android begrensd.
- Het venster wordt gecentreerd rond de **opgeslagen PRO-bullpositie** voor het huidige binnen-/buitenschermprofiel. Als dat niet klopt, kalibreer PRO opnieuw in **Tekenen** of verschuif het Android-venster.
- **Vang worp** blijft een afzonderlijke experimentele opname-en-naspeelfunctie; bordzoom wordt opzettelijk uitgeschakeld tijdens opnames om verkeerde touchcoördinaten te voorkomen.
- De app maakt geen schermopnamen voor bordzoom en leest geen inhoud van Darts Club.

 langere oefenlijnen en een voorbeeld vóór bewaren

De opgenomen swipes in versie 2.2 waren vaak maar enkele centimeters lang. In deze versie wordt hun spoor bij het **bewaren als richtlijn** verlengd zonder de echte worp of de game-aanraking te beïnvloeden.

- Tijdens een veeg verschijnt meteen een **dun groen sporenlijntje** in de overlay als visuele feedback. De dart van Darts Club zelf kan nog steeds **pas na het loslaten** verschijnen: dat is inherent aan de gebruikte opneem-en-naspeelmethode. Het is dus geen realtime opname van de echte spel-touch.
- In **Spelen** staan nu onder **Vang worp / ★ Bewaar / Overslaan** twee knoppen: **↗ Lengte ×4** en **Lijndikte 1,5**.
- **Lengte** wisselt tussen **1×, 2×, 3×, 4×, 6× en 8×**. Standaard is **4×**. De hoek en kromming blijven behouden en het beginpunt blijft vast. Bij schermranden kan de app de verlenging beperken, zodat de vorm intact blijft.
- **Lijndikte** wisselt tussen **1 / 1,5 / 2 / 3 dp**, standaard 1,5 dp, apart van de ultradunne PRO-referentielijnen.
- Na een swipe verschijnt alvast een **preview van de lange lijn**. Pas de lengte en dikte zo vaak als je wilt aan. Tik **★ Bewaar** om de op die manier ingestelde versie lokaal als aparte lijn op te slaan, of **× Overslaan** om de kandidaat te verwijderen.
- Nieuwe opgenomen oefenlijnen krijgen een **groene** kleur, zodat je ze kunt onderscheiden van de bestaande gele PRO-pijlen en blauwe hulplijnen.
- Oude PRO-lijnen, eerdere oefenlijnen en alle kalibratie/versies blijven onaangetast. De aanpassing is alleen voor nieuwe opgeslagen oefenworpen.

 oefenworp vangen, naspelen en bewaren

**Doel:** maak een oefenworp, kijk of het resultaat in Darts Club goed was, en tik dan **★ Bewaar**. De opgenomen swipe wordt als een nieuwe richtlijn in de geselecteerde laag opgeslagen, zonder de oude PRO-lijnen te vervangen.

**Android-beperking:** een gewone transparante, niet-aanraakbare overlay kan **niet** de exacte swipe registreren die gelijktijdig rechtstreeks naar Darts Club gaat. Android's publieke accessibility-API laat het origineel ook niet passief zien zonder dat dit de normale spelinvoer beïnvloedt. Daarom is dit een **experimenteel opneem-en-naspeelmodus**, geen garantie dat de game iedere worp accepteert.

### Gebruik

1. Installeer versie 2.2 **over** de bestaande app; verwijder je oude versie niet. Je opgeslagen PRO-lijnen/kalibratie blijven staan.
2. Open Darts Club, kies rechts de laag waarin je een goede worp wilt bewaren (bijvoorbeeld **PRO**, of **Vrij** voor een afzonderlijke laag).
3. Zet de overlay op **▶ Spelen**. De nieuwe knop **◎ Vang worp** verschijnt.
4. De **eerste keer** opent die knop Androids **Toegankelijkheid**-instellingen. Schakel daar alleen als je de werking begrijpt en toestemt de dienst **Darts Pen Overlay** in. Android kan bij sideloading eerst expliciet **Beperkte instellingen toestaan** verlangen. Deze gevoelige toegang is **uitsluitend nodig voor de oefenfunctie**, niet voor normale weergave.
5. Keer terug naar Darts Club en tik opnieuw **◎ Vang worp**.
6. Swipe één keer over het speelveld. De overlay registreert het gebaar en maakt zichzelf daarna niet-aanraakbaar. De toestemming-dienst probeert **diezelfde route na het loslaten** als Android-gebaar naar de game te verzenden.
7. Kijk naar het resultaat. **Goed?** Tik **★ Bewaar** om de laatste route meteen als lijn aan de actieve laag toe te voegen. **Misser?** Tik **× Overslaan**. Bij iedere volgende swipe vervangt de nieuw opgenomen route de vorige **niet-bewaarde** route.
8. Tik **● Vang AAN** om de oefenfunctie te stoppen. Gewoon spelen blijft direct met je pen werken.

### Bekende beperkingen / privacy

- In oefenmodus raakt je vinger/pen eerst **de overlay**, niet het spel. Het spel krijgt na loslaten een **door Android nagebootste** swipe. **Snelheid, timing en gamefysica kunnen afwijken**; op de HONOR Magic V5 en in Darts Club is dit nog niet getest. Als de game zulke vegen niet accepteert: stop oefenmodus en gebruik gewoon Spelen.
- Het systeem vereist een accessibility-service die normaal gesproken krachtige rechten heeft. De implementatie **leest geen scherminhoud, ontvangt geen globale touchstreams, en stuurt uitsluitend een swipe uit na een bewuste actie**. Android toont de algemene beveiligingsmelding voor de machtiging.
- De opgenomen veeg wordt uitsluitend tijdelijk in geheugen bewaard. **★ Bewaar** slaat hem lokaal op als richtlijn en maakt eerst een terugzetbare versie; **× Overslaan** verwijdert de kandidaat.
- Eén vinger of capacitieve pen tegelijk; geen multi-touch. PRO-kalibratie, PRO fijn, glow en alle doel-lagen blijven aanwezig.
- Een succesvolle Android-gesture-dispatch garandeert **niet** dat Darts Club hem als goede worp verwerkt.

 extra dunne PRO-lijnen

De vorige neonuitvoering maakte de PRO-richtlijnen te dik en bedekte daardoor delen van het dartbord. Deze versie tekent de **lijnen zelf weer ultradun**, met een subtiele gloed in plaats van brede strepen. De standaardkern is ongeveer **0,85 dp** (pijlen 0,95 dp). De pijlpuntjes en beginpunten zijn eveneens kleiner.

- Selecteer **PRO → Tekenen** en tik op **Lijn: ultradun** om te wisselen tussen **Ultradun (0,85 dp)**, **Dun (1,25 dp)** en **Normaal (1,8 dp)**. De precieze pixels verschillen met de schermdichtheid.
- Kies **✦ Glow** voor Uit/Laag/Normaal/Sterk. Zelfs bij Sterk blijft de kern dun; de gloed is een zwakke, doorschijnende halo.
- Het uiterlijk wordt **zonder gegevensmigratie** aangepast. Bestaande PRO-kalibratie, aangepaste coördinaten, versies en individuele andere targets blijven opgeslagen zoals in 2.0.
- De gekozen lijndikte blijft na opnieuw opstarten van de overlay bewaard.

 neonlijnen met instelbare glow

De PRO-preset is nu veel beter zichtbaar op licht én donker dartbord: de lijnen hebben een donkere contrastrand, een felgekleurde kern en meerdere transparante buitenlagen die een zachte neon-gloed simuleren.

- **PRO referentielijnen:** helder elektrisch cyaan / ijsblauw.
- **PRO pijlen en pijlpuntjes:** helder neongeel / goud, inclusief oplichtende startpunten.
- **Glow-stand:** kies rechts **PRO**, ga naar **Tekenen** en tik op **✦ Glow** in de linker werkbalk. Elke tik wisselt tussen **Uit → Laag → Normaal → Sterk → Uit**.
- **Normaal** is standaard ingeschakeld. De gekozen sterkte blijft na herstart bewaard.
- De glow-effecten werken ook in **Spelen**-modus, zonder dat de gehele achtergrond minder transparant wordt.
- **Belangrijk:** de wijziging betreft alleen de weergave van PRO. Bestaande richtingen, pijlvormen, kalibratiepunten, correcties en opgeslagen versies blijven ongewijzigd. T20, D20, enzovoort behouden hun eigen kleuren en lagen.
- De gloed wordt met meerdere hardwarevriendelijke transparante stroken getekend, niet met een dure blurfilter.

 PRO-fijnafstelling

Is de PRO-set bijna goed, maar iets te hoog, laag, links, rechts, te groot of net scheef? Je hoeft niet opnieuw het bord te ijken of elke pijl apart te verplaatsen.

1. Kies rechts **PRO** en ga naar **Tekenen**.
2. Tik op **⊞ PRO fijn** in de linker werkbalk. De app bewaart eerst automatisch **Back-up vóór PRO fijn** onder **Versies**.
3. Gebruik **← ↑ ↓ →** om **alle PRO-lijnen tegelijk** te verplaatsen. Tik op de groene stapknop om te wisselen tussen **0,5 / 1 / 2 / 5 dp**.
4. Tik op **−0,5% zoom** of **+0,5% zoom** als de complete groep net te groot of klein is.
5. Tik op **↶ 0,25°** of **↷ 0,25°** om de complete groep lichtjes te draaien.
6. Tik opnieuw op **✓ PRO fijn** om het paneel te sluiten, en op **▶ Spelen** om de game verder te gebruiken.

Elke stap wordt direct lokaal opgeslagen en kan via de **↶**-knop van de hoofdwerkbalk worden teruggedraaid. De PRO-kalibratie en de lijnen blijven bij het heropenen opgeslagen. Oude PRO-, T20-, D20-, enzovoort, lagen worden niet vervangen door deze update.

**Opmerking:** dit corrigeert de hele overlay, niet afzonderlijke gebogen lijnen. Als één bepaalde pijl daarna nog afwijkend staat, gebruik **↔ Verplaats** om die losse pijl te selecteren en te verschuiven.

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
