/*
 * Copyright (C) 2015 Inera AB (http://www.inera.se)
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

/* globals browser, intyg, logg */

'use strict';

module.exports = function() {

    this.Given(/^ska intyget finnas i Mina intyg$/, function (callback) {
      var intygElement = element(by.id('certificate-' + intyg.id));
      expect(intygElement.isPresent()).to.eventually.equal(true).and.notify(callback);
    });

    this.Given(/^jag går till Mina intyg för patienten "([^"]*)"$/, function(pnr, callback) {
        browser.ignoreSynchronization = true;
        browser.get(process.env.MINAINTYG_URL + '/welcome.jsp');
        element(by.id('guid')).sendKeys(pnr);
        element(by.css('input.btn')).click().then(function() {

            // Detta behövs pga att Mina intyg är en extern sida
            browser.sleep(2000);

            // Om samtyckesruta visas
            element(by.id('consentTerms')).isPresent().then(function(result) {
                if (result) {
                    logg('Lämnar samtycke..');
                    element(by.id('giveConsentButton')).click().then(callback);
                } else {
                    callback();
                }
            });
        });
    });

    this.Given(/^ska intygets status i Mina intyg visa "([^"]*)"$/, function(status, callback) {
        var intygElement = element(by.id('certificate-' + intyg.id));
        expect(intygElement.getText()).to.eventually.contain(status).and.notify(callback);
    });
};
