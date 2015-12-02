/* globals pages */
/* globals browser, intyg, protractor */

'use strict';

module.exports = function () {

    this.Then(/^vill jag vara inloggad$/, function (callback) {
        expect(element(by.id('wcHeader')).getText()).to.eventually.contain('Logga ut').and.notify(callback);
    });

    this.Given(/^att jag är inloggad som läkare "([^"]*)"$/, function (anvandarnamn, callback) {
        console.log('Loggar in som ' + anvandarnamn + '..');
        global.pages.welcome.get();
        global.pages.welcome.loginByName(anvandarnamn);
        callback();
    });

    this.When(/^jag väljer patienten "([^"]*)"$/, function (personnummer, callback) {
        global.pages.app.views.sokSkrivIntyg.selectPersonnummer(personnummer);
        callback();
    });

    this.Given(/^jag går in på  att skapa ett "([^"]*)" intyg$/, function (intygsTyp, callback) {
        browser.ignoreSynchronization = true;

        global.pages.app.views.sokSkrivIntyg.selectIntygTypeByLabel(intygsTyp);
        global.pages.app.views.sokSkrivIntyg.continueToUtkast();
        browser.ignoreSynchronization = false;
        callback();
    });
    
    this.Given(/^signerar intyget$/, {
        timeout: 100 * 2000
    }, function (callback) {
        // expect(element(by.id('signera-utkast-button')).isPresent()).toBe(true);
        var EC = protractor.ExpectedConditions;
        // Waits for the element with id 'abc' to be clickable.
        browser.wait(EC.elementToBeClickable($('#signera-utkast-button')), 20000);
        element(by.id('signera-utkast-button')).click().then(callback);
    });

    this.Then(/^ska intygets status vara "([^"]*)"$/, function (statustext, callback) {
        expect(element(by.id('intyg-vy-laddad')).getText()).to.eventually.contain(statustext).and.notify(callback);
    });

    this.Then(/^jag ska se den data jag angett för intyget$/, function (callback) {
        // // Intyget avser
        var intygetAvser = element(by.id('intygAvser'));

        //Sortera typer till den ordning som Webcert använder
        var selectedTypes = intyg.korkortstyper.sort(function (a, b) {
            var allTypes = ['AM', 'A1', 'A2', 'A', 'B', 'BE', 'TRAKTOR', 'C1', 'C1E', 'C', 'CE', 'D1', 'D1E', 'D', 'DE', 'TAXI'];
            return allTypes.indexOf(a.toUpperCase()) - allTypes.indexOf(b.toUpperCase());
        });

        selectedTypes = selectedTypes.join(', ').toUpperCase();
        console.log('Kontrollerar att intyget avser körkortstyper:'+selectedTypes);

        expect(intygetAvser.getText()).to.eventually.contain(selectedTypes);

        // //Identiteten är styrkt genom
        var idStarktGenom = element(by.id('identitet'));
        console.log('Kontrollerar att intyg är styrkt genom: ' + intyg.identitetStyrktGenom);
        expect(idStarktGenom.getText()).to.eventually.contain(intyg.identitetStyrktGenom).and.notify(callback);
    });

};
