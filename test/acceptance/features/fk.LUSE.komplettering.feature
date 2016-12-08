# language: sv
@komplettering @luse @INTYG-2642
 # kompletteringsfråga saknas på utkast-sida
Egenskap: Komplettering av LUSE-intyg

Bakgrund: Jag befinner mig på webcerts förstasida
   Givet att jag är inloggad som läkare
   När jag går in på en patient
   
@nyttIntyg
Scenario: Ska kunna besvara komplettering med nytt intyg
   När jag går in på att skapa ett "Läkarutlåtande för sjukersättning" intyg
   Och jag fyller i alla nödvändiga fält för intyget
   Och jag signerar intyget
   Och jag ska se den data jag angett för intyget
   Och jag skickar intyget till Försäkringskassan
   Så ska intygets status vara "Intyget är signerat"

   När Försäkringskassan skickar ett "KOMPLT" meddelande på intyget
   Och jag går in på intyget
   Och jag väljer att svara med ett nytt intyg
   Så ska jag se kompletteringsfrågan på utkast-sidan

   När jag signerar intyget
   Så jag ska se den data jag angett för intyget

   @fortsattUtkast @INTYG-2885 @notReady
Scenario: Ska kunna fortsätta besvara kompletterande intyg 
   När jag går in på ett "Läkarutlåtande för sjukersättning" med status "Signerat" 
   Och jag skickar intyget till Försäkringskassan
   När Försäkringskassan skickar ett "KOMPLT" meddelande på intyget
   Och jag går in på intyget
   Och jag väljer att svara med ett nytt intyg
   Och jag går tillbaka till intyget som behöver kompletteras
   Så ska det finnas en knapp med texten "Fortsätt på intygsutkast"

# Scenario: Ska kunna besvara komplettering med textmeddelande
#    När jag går in på ett "Läkarintyg för sjukpenning" med status "Mottaget"
#    När Försäkringskassan ställer en "Komplettering_av_lakarintyg" fråga om intyget
#    Och jag går in på intyget
#    Så ska jag se kompletteringsfrågan på intygs-sidan
#    Och jag ska kunna svara med textmeddelande

   @fortsattUtkast @SMIkompletteringsutkast
Scenariomall: Ska kunna fortsätta besvara kompletterande intyg 
   När jag går in på ett <intygsTyp> med status "Signerat" 
   Och jag skickar intyget till Försäkringskassan
   När Försäkringskassan skickar ett "KOMPLT" meddelande på intyget
   Och jag går in på intyget
   Och jag väljer att svara med ett nytt intyg
   Och sparar länken till aktuell sida
   Och jag går tillbaka till intyget som behöver kompletteras
   Så ska det finnas en knapp med texten "Fortsätt på intygsutkast"
   Och jag trycker på knappen med texten "Fortsätt på intygsutkast"
   Så jag verifierar att URL:en är samma som den sparade länken

Exempel:
   |                             intygsTyp                              |
   |"Läkarutlåtande för sjukersättning"                                 |
   |"Läkarintyg för sjukpenning"                                        |
   |"Läkarutlåtande för aktivitetsersättning vid förlängd skolgång"     |
   |"Läkarutlåtande för aktivitetsersättning vid nedsatt arbetsförmåga" |