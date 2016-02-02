# language: sv
@behorighet
# PRIVILEGE_NAVIGERING
Egenskap: Behörigheter för en "uthoppad" vårdadministratör

#@vardadmin_uthopp
Scenario: En vårdadministratör ska kunna byta vårdenhet 
   Givet att jag är inloggad som vårdadministratör
   #Givet att jag är inloggad som uthoppad vårdadministratör
   Och vårdenhet ska vara "WebCert-Vårdgivare2 - WebCert-Enhet2"
   När jag väljer att byta vårdenhet
   Och väljer "WebCert-Enhet2"
   Så vårdenhet ska vara "WebCert-Vårdgivare2 - WebCert-Enhet2"

# PRIVILEGE_VIDAREBEFORDRA_FRAGASVAR

# PRIVILEGE_VIDAREBEFORDRA_UTKAST
# PRIVILEGE_ATKOMST_ANDRA_ENHETER
#@vardadmin_uthopp
Scenario: Det ska gå att Vidarebefodra ett utkast
	Givet att jag är inloggad som vårdadministratör
	#Givet att jag är inloggad som uthoppad vårdadministratör
	Och går in på Ej signerade utkast 
	Och Vidarebeforda knappen synns
	Så avbryter jag vidarebefodran

# Skriva utkast, Läsa intyg/utkast
#@vardadmin_uthopp
Scenario: Administratör kan ej signera intyg.
	Givet att jag är inloggad som vårdadministratör
	#Givet att jag är inloggad som uthoppad vårdadministratör
	När jag väljer flik "Sök/skriv intyg"
	När jag väljer patienten "19971019-2387"
	Och jag går in på att skapa ett "Läkarintyg FK 7263" intyg
	Och jag fyller i alla nödvändiga fält för intyget
	Så synns inte signera knappen

# PRIVILEGE_HANTERA_PERSONUPPGIFTER
# - Visar "Hämta personuppgifter"-knapp på utkast
# - Information om personuppgifter som uppdateras på kopiera-dialog (ej djupintegration)
#@vardadmin_uthopp
Scenario: Admin kan visa "Hämta personuppgifter"-knapp på utkast
	Givet att jag är inloggad som vårdadministratör
		När jag väljer flik "Sök/skriv intyg"
		När jag väljer patienten "19971019-2387"
		Och jag går in på att skapa ett "Läkarintyg FK 7263" intyg
		Så synns Hämta personuppgifter knappen
		

# - Visar information om sekretessmarkerade personuppgifter
#@vardadmin_uthopp
Scenario: Admin kan visa information om sekretessmarkerade personuppgifter
	Givet att jag är inloggad som vårdadministratör
		När jag väljer flik "Sök/skriv intyg"
		När jag väljer patienten "19080814-9819"
		Och jag kan inte gå in på att skapa ett "Läkarintyg FK 7263" intyg
		Så meddelas jag om spärren

@vardadmin_uthopp
Scenario: Admin kan visa information om sekretessmarkerade personuppgifter
	Givet att jag är inloggad som vårdadministratör
		När jag väljer flik "Sök/skriv intyg"
		När jag väljer patienten "19121212-1212"

# PRIVILEGE_HANTERA_MAILSVAR
# Visar meddelande om att vårdenheter får mail om svar inkommer på en fråga.
@vardadmin_uthopp
Scenario: Det ska gå att Vidarebefodra ett utkast
	#Givet att jag är inloggad som uthoppad vårdadministratör

#@vardadmin_uthopp
#Scenario: Det ska gå att Vidarebefodra ett utkast
	#Givet att jag är inloggad som uthoppad vårdadministratör