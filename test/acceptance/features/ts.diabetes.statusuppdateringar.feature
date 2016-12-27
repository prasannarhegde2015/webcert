# language: sv
@statusuppdateringar @ts @diabetes
Egenskap: Statusuppdateringar för TS-diabetes intyg

Bakgrund: Jag har skickat en CreateDraft:2 till Webcert.
   Givet att jag är inloggad som djupintegrerad läkare på vårdenhet "TSTNMT2321000156-1004"
   Och att vårdsystemet skapat ett intygsutkast för "Transportstyrelsens läkarintyg, diabetes"
   Och jag går in på intygsutkastet via djupintegrationslänk

@skicka-till-ts
Scenario: Statusuppdateringar då intyg skickas till Transportstyrelsen
    Så ska statusuppdatering "SKAPAT" skickas till vårdsystemet. Totalt: "1"

    När jag fyller i alla nödvändiga fält för intyget
    Och jag signerar intyget
    Så ska statusuppdatering "SIGNAT" skickas till vårdsystemet. Totalt: "1"

    När jag skickar intyget till Transportstyrelsen
    Så ska statusuppdatering "SKICKA" skickas till vårdsystemet. Totalt: "1"


@makulera @waitingForFix
Scenario: Statusuppdateringar då intyg makuleras
    När jag fyller i alla nödvändiga fält för intyget
    Och jag signerar intyget
    Och jag skickar intyget till Försäkringskassan

    När jag makulerar intyget
    Så ska statusuppdatering "MAKULE" skickas till vårdsystemet. Totalt: "1"

@radera
Scenario: Statusuppdateringar då intyg raderas
    När jag fyller i alla nödvändiga fält för intyget
    Och jag raderar intyget
    Så ska statusuppdatering "RADERA" skickas till vårdsystemet. Totalt: "1"

@vardkontakt-skickas-med
Scenario: Vårdkontakt skickas med statusuppdateringar
    Så ska jag gå in på intyget med en extra "ref" parametrar med värdet "testref"

    När jag fyller i alla nödvändiga fält för intyget
    Och jag signerar intyget
    Och jag kopierar intyget
    Så ska statusuppdatering "SKAPAT" skickas till vårdsystemet. Totalt: "1"
    Och ska statusuppdateringen visa att parametern "ref" är mottagen med värdet "testref"
    Och ska statusuppdateringen visa mottagna frågor totalt 0,ej besvarade 0,besvarade 0, hanterade 0
    Och ska statusuppdateringen visa skickade frågor totalt 0,ej besvarade 0,besvarade 0, hanterade 0