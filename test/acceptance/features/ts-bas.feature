# language: sv

@smoke @webcert @ts @bas
Egenskap: Kontrollera att det går att hantera bas-intyg för transportstyrelsen

Bakgrund: Jag befinner mig på webcerts förstasida
    Givet att jag är inloggad som läkare

Scenario: Skapa och signera ett intyg till transportstyrelsen
    När jag väljer patienten "19121212-1212"
    Och jag går in på att skapa ett "Transportstyrelsens läkarintyg" intyg
    Och jag fyller i alla nödvändiga fält för intyget
    Och signerar intyget
    Så ska intygets status vara "Intyget är signerat"
    Och jag ska se den data jag angett för intyget
    Och jag jämför att all data är rätt

Scenario: Skicka ett signerat bas-intyg till Transportstyrelsen
När jag väljer patienten "19121212-1212"@removeDraft
    Och jag går in på ett "Transportstyrelsens läkarintyg" med status "Signerat"
    Och jag skickar intyget till Transportstyrelsen
    Så ska intygets status vara "Intyget är signerat och har skickats till Transportstyrelsens system"

Scenario: Makulera ett skickat bas-intyg
	När jag väljer patienten "19121212-1212"
    Och jag går in på ett "Transportstyrelsens läkarintyg" med status "Mottaget"
    Så ska intygets status vara "Intyget är signerat och mottaget av Transportstyrelsens system"
	Och jag makulerar intyget
	Så ska intyget visa varningen "Begäran om makulering skickad till intygstjänsten"
