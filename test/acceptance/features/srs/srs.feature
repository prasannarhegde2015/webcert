#language: sv

@notReady
@SRS
Egenskap: Webcert visa information från SRS i form av prediktioner på sjukskrivningslängd

Bakgrund: 
    Givet att jag är inloggad som läkare på vårdenhet med SRS
    

@SRS-US-W01
@SRS-US-W02
@SRS-US-W03
@SRS-US-W04
Scenario: När samtycke är givet och ytterligare frågor besvarade ska information från SRS visas.
    Givet att jag valt en patient som inte har givit samtycke till SRS
    Och att jag befinner mig på ett nyskapat Läkarintyg FK 7263
    När jag fyller i diagnoskod som inte finns i SRS
    När jag klickar på knappen för SRS
    Så ska en fråga om samtycke visas
    När jag anger att patienten samtycker till SRS
    Och jag fyller i ytterligare frågor för SRS
    Så ska åtgärdsförslag från SRS-tjänsten visas
    När jag trycker på fliken "Statistik"
    Så ska en statistikbild från SRS-tjänsten visas


@SRS-US-W01
Scenario: SRS-knappen ska bara visas när diagnos som har stöd för SRS är ifylld
    Givet att jag valt en patient som har givit samtycke till SRS
    Och att jag befinner mig på ett nyskapat Läkarintyg FK 7263
    När jag fyller i diagnoskod som finns i SRS
    Så ska knappen för SRS visas i läge "stängd"
    När jag fyller i diagnoskod som inte finns i SRS
    Så ska knappen för SRS inte visas

@SRS-US-W01
Scenario: Samtycken som patienter har givit ska lagras
    Givet att patienten har givit samtycke för SRS
    Och jag har fyllt i diagnoskod
    När jag klickar på knappen för SRS
    Så ska en fråga om samtycke visas
    Och frågan om samtycke ska redan vara ifylld som "Ja"

@SRS-US-W01
Scenario: Användaren ska kunna visa och dölja UI-komponenter som hör till SRS
    Givet att jag valt en patient som har givit samtycke till SRS
    Och att jag befinner mig på ett nyskapat Läkarintyg FK 7263
    När jag fyller i diagnoskod som finns i SRS
    Så ska knappen för SRS visas i läge "stängd"
    När jag klickar på knappen för SRS
    Så ska knappen för SRS visas i läge "öppen"
    Och ska visas en frågepanel för SRS
    När jag klickar på texten "visa mindre"
    Så ska frågepanelen för SRS minimeras
    När jag klickar på texten "visa mer"
    Så ska frågepanelen för SRS maximeras

@SRS-US-W02
Scenario: När åtgärdsförslag inte kan ges ska korrekt felmeddelande zvisas
    Givet att jag valt en patient som har givit samtycke till SRS
    Och att jag befinner mig på ett nyskapat Läkarintyg FK 7263
    När jag fyller i diagnoskod som finns i SRS men med tekniskt fel
    Och jag fyller i ytterligare frågor för SRS
    Så ska felmeddelandet "Tekniskt fel" visas

@SRS-US-W02
Scenario: När åtgärdsförslag saknas ska korrekt felmeddelande visas
    Givet att jag valt en patient som har givit samtycke till SRS
    Och att jag befinner mig på ett nyskapat Läkarintyg FK 7263
    När jag fyller i diagnoskod som finns i SRS men där åtgärdsförslag saknas
    Och jag fyller i ytterligare frågor för SRS
    Så ska felmeddelandet "Åtgärdsförslag saknas" visas

@SRS-US-W04
Scenario: När jag byter svar i diagnosspecifika frågor ska ny information från SRS visas


@SRS-US-W04
Scenario: När prediktion för en viss diagnoskod saknas ska användaren informeras.