# language: sv
@smitta @fk7263
Egenskap: Hantera FK7263 Smittskyddsintyg

Bakgrund: Jag är inne på en patient
	Givet att jag är inloggad som läkare
	När jag går in på en patient

Scenario: Kontrollera att endast fält för Smitta-intyg följer med när intyg signeras
	När jag går in på att skapa ett "Läkarintyg FK 7263" intyg
	Och jag fyller i ett intyg som inte är smitta
	Och jag fyller i ett intyg som är smitta
	Och jag signerar intyget
	Så jag ska se den data jag angett för intyget