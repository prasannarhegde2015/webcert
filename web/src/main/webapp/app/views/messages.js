/*
 * Copyright (C) 2016 Inera AB (http://www.inera.se)
 *
 * This file is part of sklintyg (https://github.com/sklintyg).
 *
 * sklintyg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * sklintyg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* jshint maxlen: false, unused: false */
var wcMessages = {
    'sv': {
        'webcert.header': 'Webcert',
        'webcert.description': 'Välkommen till Webcert.',
        'dashboard.title': 'Mina andra enheter',
        'dashboard.unanswered.title': 'Frågor och svar',
        'dashboard.unsigned.title': 'Ej signerade utkast',
        'dashboard.about.title': 'Om Webcert',

        //labels
        'label.default-intyg-type': 'Välj typ av intyg',

        'label.unsignedintyg': 'Ej signerade intyg',
        'label.unansweredintyg': 'Intyg med ej hanterad fråga',
        'label.readytosignintyg': 'Intyg färdiga att signera (massignering)',
        'label.showallintyg': 'Visa alla intyg',
        'label.showfewerintyg': 'Visa färre intyg',
        'label.patient': 'Patient:',
        'label.signselectedintyg': 'Signera valda intyg',

        'label.createutkast.sekretessmarkering': '<p>Patienten har en sekretessmarkering. Det innebär att patientens folkbokföringsuppgifter är skyddade. Var vänlig hantera dem varsamt.</p>På grund av sekretessmarkeringen går det inte att skriva nya elektroniska intyg.',
        'label.copyutkast.sekretessmarkering': 'På grund av sekretessmarkeringen går det inte att kopiera intyg.',

        'label.confirmaddress': 'Återanvänd uppgifter',
        'label.confirmsign': 'Signera intyget',
        'label.copyintyg': 'Kopiera intyg',
        'label.fornyaintyg': 'Förnya intyg',

        'print.label.signed': 'Observera! Detta är en webbutskrift av intyget och är därför inte giltigt som intyg.',
        'print.label.draft': 'Observera! Intyget är ett utkast och är därför inte giltigt.',
        'print.label.revoked': 'Observera! Intyget är makulerat och därför inte giltigt.',

        'label.makulera': 'Makulera intyg',
        'label.makulera.body' : 'Intyg som innehåller ogiltig eller felaktig information kan makuleras. Om intyget har skickats elektroniskt till en mottagare kommer mottagaren att informeras om makuleringen.<br><br>Det makulerade intyget kan ersättas med ett nytt. Då kopieras intygsinformationen in i ett nytt utkast som kan korrigeras.',
        'label.makulera.confirmation': 'Kvittens - Återtaget intyg',

        'label.qaonlywarning' : 'Du har valt att lämna frågor och svar',
        'label.qaonlywarning.body' : '<p>Observera att intyg ska utfärdas via journalsystemet och inte via Webcert.</p><p>Information i Webcert som inte är frågor och svar kan inte visas i journalsystemet.</p>',

        'label.qacheckhanterad.title' : 'Markera besvarade frågor som hanterade',
        'label.qacheckhanterad.body' : '<p>Det finns besvarade frågor som inte är markerade som hanterade.<br>En besvarad fråga som markeras som hanterad anses avslutad och flyttas till \"Hanterade frågor och svar\"</p>',
        'label.qacheckhanterad.igen' : 'Visa inte det här meddelandet igen',
        'label.qacheckhanterad.checkbox' : 'Visa inte det här meddelandet igen',
        'label.qacheckhanterad.help.checkbox' : 'Visa inte det här meddelandet igen',
        'label.qacheckhanterad.hanterad' : 'Hanterade',
        'label.qacheckhanterad.ejhanterad' : 'Ej Hanterade',
        'label.qacheckhanterad.tillbaka' : 'Tillbaka',

        //intyg types
        'intygtypes.fk7263.typename': 'Läkarintyg FK 7263',
        'intygtypes.ts-bas.typename': 'Transportstyrelsens läkarintyg',
        'intygtypes.ts-diabetes.typename': 'Transportstyrelsens läkarintyg, diabetes',
        'intygtypes.lisu.typename': 'Läkarintyg för sjukpenning utökat',
        'intygtypes.luse.typename': 'Läkarutlåtande för sjukersättning',
        'intygtypes.luae_na.typename': 'Läkarutlåtande för aktivitetsersättning vid nedsatt arbetsförmåga',
        'intygtypes.luae_fs.typename': 'Läkarutlåtande för aktivitetsersättning vid förlängd skolgång',

        //intyg help new texts as of 2016 (Can be removed?)
        'intygtypes.2016.ts-bas.helptext': '<p>Transportstyrelsens läkarintyg ska användas vid förlängd giltighet av högre behörighet från 45 år, ansökan om körkortstillstånd för grupp II och III och vid ansökan om taxiförarlegitimation. Transportstyrelsens läkarintyg kan även användas när Transportstyrelsen i annat fall begärt ett allmänt läkarintyg avseende lämplighet att inneha körkort.</p>Specialistintyg finns bl.a. för alkohol, läkemedel, synfunktion, Alkolås m.m. Se <a href="http://www.transportstyrelsen.se" target="_blank">www.transportstyrelsen.se</a>.',
        'intygtypes.2016.ts-diabetes.helptext': '<p>Transportstyrelsens läkarintyg, diabetes ska användas vid diabetessjukdom. Föreskrivna krav på läkarens specialistkompetens vid diabetessjukdom framgår av 17 kap. i Transportstyrelsens föreskrifter (TSFS 2010:125) och allmänna råd om medicinska krav för innehav av körkort m.m.</p>Information om Transportstyrelsens föreskrifter finns på <a href="http://www.transportstyrelsen.se" target="_blank">www.transportstyrelsen.se</a>.',
        'intygtypes.2016.fk7263.helptext': 'Läkarintyget används av Försäkringskassan för att bedöma om patienten har rätt till sjukpenning. Av intyget ska det framgå hur sjukdomen påverkar patientens arbetsförmåga och hur länge patienten behöver vara sjukskriven.',
        'intygtypes.2016.sjukpenning.helptext': 'NEW: Läkarintyg för sjukpenning utökat används av läkaren för att dokumentera en nedsättning av arbetsförmågan, medicinska behandlingar och prognos samt rekommendera åtgärder. Det finns även möjlighet att ange uppgifter som kan hjälpa Försäkringskassan att utreda behov av samordning. Patienten använder därefter det för att ansöka om sjukpenning och rehabilitering eller vid frånvaro pga. sjukdom vid deltagande i arbetsmarknadspolitiskt program. Försäkringskassan fattar beslut om sjukpenning. Informationen i Läkarintyg för sjukpenning utökat ligger till grund för detta beslut. En läkares bedömningar i det innebär ingen ovillkorlig rätt till sjukpenning utan är ett underlag för beslut.',
        'intygtypes.2016.luse.helptext': '<h3>Vad är sjukersättning?</h3><p>Sjukersättning är en ersättning för personer mellan 30 och 64 år som har nedsatt arbetsförmåga på grund av sjukdom, skada eller funktionsnedsättning. Beroende på hur mycket arbetsförmågan är nedsatt kan man få en fjärdedels, halv, tre fjärdedels eller hel sjukersättning. Man kan få sjukersättning om Försäkringskassan bedömer att arbetsförmågan är nedsatt med minst 25 procent för all överskådlig framtid och att alla rehabiliteringsmöjligheter är uttömda.</p>',
        'intygtypes.2016.lisu.helptext': 'Läkarintyg för sjukpenning utökat används av läkaren för att dokumentera en nedsättning av arbetsförmågan, medicinska behandlingar och prognos samt rekommendera åtgärder. Det finns även möjlighet att ange uppgifter som kan hjälpa Försäkringskassan att utreda behov av samordning. Patienten använder därefter det för att ansöka om sjukpenning och rehabilitering eller vid frånvaro pga. sjukdom vid deltagande i arbetsmarknadspolitiskt program. Försäkringskassan fattar beslut om sjukpenning. Informationen i Läkarintyg för sjukpenning utökat ligger till grund för detta beslut. En läkares bedömningar i det innebär ingen ovillkorlig rätt till sjukpenning utan är ett underlag för beslut.',
        'intygtypes.2016.luae_na.helptext': 'Läkarintyget används av Försäkringskassan för att bedöma om patienten har rätt till aktivitetsersättning för nedsatt arbetsförmåga.',
        'intygtypes.2016.luae_fs.helptext': 'Läkarutlåtandet används av Försäkringskassan för att bedöma om patienten har rätt till aktivitetsersättning vid förlängd skolgång. Det medicinska underlaget skabeskriva om funktionsnedsättning finns eller har funnits som påverkar behovet av förlängd skolgång. Försäkringskassan behöver uppgifterna för att bedöma om det finns ett direkt samband mellan funktionsnedsättningen och behovet av förlängd skolgångför attslutföra grundskola eller gymnasium.',

        //about texts
        'about.cookies': '<h3>Om kakor (cookies)</h3><p>Så kallade kakor (cookies) används för att underlätta för besökaren på webbplatsen. En kaka är en textfil som lagras på din dator och som innehåller information. Denna webbplats använder så kallade sessionskakor. Sessionskakor lagras temporärt i din dators minne under tiden du är inne på en webbsida. Sessionskakor försvinner när du stänger din webbläsare. Ingen personlig information om dig sparas vid användning av sessionskakor.</p><p>Om du inte accepterar användandet av kakor kan du stänga av det via din webbläsares säkerhetsinställningar. Du kan även ställa in webbläsaren så att du får en varning varje gång webbplatsen försöker sätta en kaka på din dator.</p><p><strong>Observera!</strong> Om du stänger av kakor i din webbläsare kan du inte logga in i Webcert.</p><p>Allmän information om kakor (cookies) och lagen om elektronisk kommunikation finns på Post- och telestyrelsens webbplats.</p><p><a href="https://www.pts.se/sv/Privat/Internet/Integritet1/Fragor-och-svar-om-kakor-for-anvandare2/" target="_blank">Mer om kakor (cookies) på Post- och telestyrelsens webbplats</a></p>',

        // validation messages
        'validation.invalidfromdate': 'Från-datum är felaktigt. Använd formatet ÅÅÅÅ-MM-DD',
        'validation.invalidtodate':   'Till-datum är felaktigt. Använd formatet ÅÅÅÅ-MM-DD',
        'validation.invalidtobeforefromdate': 'Till-datum är före från-datum.',

        //info messages
        'info.nounsignedintygfound': '<strong>Inga ej signerade intyg hittades.</strong>',
        'info.nounsigned.intyg.for.unit': '<strong>Inga ej signerade intyg hittades på enheten.</strong>',
        'info.nounansweredintygfound': '<strong>Inga intyg med ohanterade frågor hittades.</strong>',
        'info.noreadytosignintygfound': '<strong>Inga klarmarkerade intyg hittades.</strong>',
        'info.loadingdata': '<strong>Uppdaterar lista...</strong>',
        'info.nounanswered.qa.for.unit': '<strong>Samtliga frågor och svar är hanterade. Det finns inget att åtgärda.</strong>',
        'info.nointygfound': '<strong>Inga intyg hittades.</strong>',
        'info.query.noresults': '<strong>Sökningen gav inga resultat.</strong>',
        'info.query.error': '<strong>Sökningen kunde inte utföras.</strong>',
        'info.intygload.error': '<strong>Kunde inte hämta intyg.</strong>',
        'info.intygload.offline': '<strong>Intygstjänsten ej tillgänglig, endast Intyg utfärdade av Webcert visas.</strong>',
        'info.running.query': '<strong>Söker...</strong>',

        //error messages
        'error.unsignedintyg.couldnotbeloaded': '<strong>Kunde inte hämta ej signerade intyg.</strong>',
        'error.unansweredintyg.couldnotbeloaded': '<strong>Kunde inte hämta listan med ej hanterade frågor och svar.</strong>',
        'error.readytosignintyg.couldnotbeloaded': '<strong>Kunde inte hämta intyg klara för signering.</strong>',
        'error.failedtocreateintyg': 'Kunde inte skapa intyget. Försök igen senare.',
        'error.failedtomakuleraintyg': 'Kunde inte makulera intyget. Försök igen senare.',
        'error.failedtocopyintyg': 'Kunde inte kopiera intyget. Försök igen senare.',
        'error.failedtocopyintyg.personidnotfound': 'Kunde inte kopiera intyget. Det nya person-id:t kunde inte hittas.',
        'error.failedtofornyaintyg': 'Kunde inte förnya intyget. Försök igen senare.',
        'error.failedtofornyaintyg.personidnotfound': 'Kunde inte förnya intyget. Det nya person-id:t kunde inte hittas.',
        'error.failedtosendintyg': 'Kunde inte skicka intyget. Försök igen senare.',
        'error.pu.namenotfound': 'Personnumret du har angivit finns inte i folkbokföringsregistret. Kontrollera om du har skrivit rätt.<br>Observera att det inte går att ange reservnummer. Webcert hanterar enbart person- och samordningsnummer.',
        'error.pu.samordningsnummernotfound': 'Samordningsnumret du har angivit finns inte i folkbokföringsregistret. Kontrollera om du har skrivit rätt.<br>Observera att det inte går att ange reservnummer. Webcert hanterar enbart person- och samordningsnummer.',
        'error.pu.nopersonnummer': 'Det personnummer du har angett kunde tyvärr inte hämtas från folkbokföringsregistret. Försök igen senare.',
        'error.pu.noname': 'Namn för det nummer du har angett kunde tyvärr inte hämtas från folkbokföringsregistret. Försök igen senare.',
        'error.pu.unknownerror': 'Kunde inte kontakta PU-tjänsten.',

        //Table headings
        //Tidigare intyg tabell
        'th.label.intyg-type': 'Typ av intyg',
        'th.help.intyg-type': 'Typ av intyg',
        'th.label.status': 'Status',
        'th.help.status': 'Visar utkastets/intygets status.<ul> <li>Utkast, uppgifter saknas = Utkastet är sparat, men obligatoriska uppgifter saknas.</li><li>Utkast, kan signeras = Utkastet är sparat och kan signeras.</li><li>Signerat = Intyget är signerat.</li><li>Skickat = Intyget är signerat och skickat till mottagaren.</li> <li>Mottaget = Intyget är signerat och mottaget av mottagarens system.</li><li>Makulerat = Intyget är makulerat.</li></ul>',
        'th.label.saved-date': 'Sparat datum',
        'th.help.saved-date': 'Datum då utkastet/intyget senast sparades.',
        'th.label.saved-signed-by': 'Sparat/signerat av',
        'th.help.saved-signed-by': 'Person som senast sparade utkastet/intyget alternativt person som signerade intyget.',

        //Ej signerade utkast
        'th.label.draft-forwarded': 'Vidarebefordrad',
        'th.help.draft-forwarded': 'Här markerar du om utkastet är vidarebefordrat till den som ska signera det.',
        'th.label.draft-type': 'Typ av intyg',
        'th.help.draft-type': 'Typ av intyg',
        'th.label.draft-status': 'Status',
        'th.help.draft-status': 'Visar utkastets status.<ul><li>Utkast, uppgifter saknas = Utkastet är sparat, men obligatoriska uppgifter saknas.</li><li>Utkast, kan signeras = Utkastet är sparat och kan signeras.</li><li>Signerat = Intyget är signerat.</li><li>Skickat = Intyget är signerat och skickat till mottagaren.</li> <li>Mottaget = Intyget är signerat och mottaget av mottagarens system.</li><li>Makulerat = Intyget är makulerat.</li></ul>',
        'th.label.draft-saved-date': 'Sparat datum',
        'th.help.draft-saved-date': 'Datum då utkastet senast sparades.',
        'th.label.draft-patient': 'Patient',
        'th.help.draft-patient': 'Personnummer för patient som utkastet gäller.',
        'th.label.draft-saved-by': 'Sparat av',
        'th.help.draft-saved-by': 'Person som senast sparade utkastet.',

        //Fraga/Svar
        'th.label.qa-forwarded': 'Vidarebefordrad',
        'th.help.qa-forwarded': 'Markera om fråga-svar är vidarebefordrat till den som ska hantera det.',
        'th.label.qa-action': 'Åtgärd',
        'th.help.qa-action': 'Åtgärd som krävs för att fråga-svar ska anses som hanterad och avslutad.',
        'th.label.qa-sender': 'Avsändare',
        'th.help.qa-sender': 'Vem som initierade frågan.',
        'th.label.qa-patient': 'Patient',
        'th.help.qa-patient': 'Berörd patients personnummer.',
        'th.label.qa-signed-by': 'Signerat av',
        'th.help.qa-signed-by': 'Läkare som har signerat intyget.',
        'th.label.qa-sent-recv-date': 'Skickat/mottaget',
        'th.help.qa-sent-recv-date': 'Datum för senaste händelse. Exempelvis när fråga skickades eller när ett svar inkom.'

    },
    'en': {
        'webcert.header': 'Webcert Application (en)'
    }
};
