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

/*global intyg,logger,pages,Promise,wcTestTools,person,protractor*/
'use strict';
var testdataHelper = wcTestTools.helpers.testdata;
var loginHelpers = require('./inloggning/login.helpers.js');
// var restTestdataHelper = wcTestTools.helpers.restTestdata;
var sokSkrivIntygPage = pages.sokSkrivIntyg.pickPatient;
var sokSkrivIntygUtkastTypePage = pages.sokSkrivIntyg.valjUtkastType;
var fkUtkastPage = pages.intyg.fk['7263'].utkast;
var fkIntygPage = pages.intyg.fk['7263'].intyg;


function writeNewIntyg(typ, status) {
    var standardUser = global.user;

    var userObj = {
        fornamn: 'Erik',
        efternamn: 'Nilsson',
        hsaId: 'TSTNMT2321000156-105H',
        enhetId: standardUser.enhetId,
        lakare: true
    };

    // Logga in med en användrae som garanterat kan signera intyg
    return loginHelpers.logInAsUserRole(userObj, 'Läkare')
        // Väj samma person som tidigare
        .then(function() {
            return sokSkrivIntygPage.selectPersonnummer(person.id)
                .then(function() { // Välj rätt typ av utkast
                    return sokSkrivIntygUtkastTypePage.selectIntygTypeByLabel(typ);
                })
                .then(function() { // Klicka på skapa nytt utkast
                    return sokSkrivIntygUtkastTypePage.intygTypeButton.sendKeys(protractor.Key.SPACE);
                })
                .then(function() { // Ange intygsdata
                    global.intyg = require('./helpers').generateIntygByType(typ);
                    return require('./fillIn').fillIn(intyg);
                })
                .then(function() { //Klicka på signera
                    return fkUtkastPage.signeraButton.sendKeys(protractor.Key.SPACE);
                })
                .then(function() { // Skicka till mottagare om intyget ska vara Mottaget
                    if (status === 'Mottaget') {
                        return fkIntygPage.skicka.knapp.sendKeys(protractor.Key.SPACE)
                            .then(function() {
                                return fkIntygPage.skicka.dialogKnapp.sendKeys(protractor.Key.SPACE);
                            });
                    } else {
                        return Promise.resolve();
                    }
                })
                .then(function() { // Logga in med tidigare användare
                    return loginHelpers.logInAsUser({
                        fornamn: standardUser.fornamn,
                        efternamn: standardUser.efternamn,
                        hsaId: standardUser.hsaId,
                        enhetId: standardUser.enhetId,
                        lakare: standardUser.lakare,
                        origin: standardUser.origin
                    });
                });
        });
}

module.exports = {
    createIntygWithStatus: function(typ, status) {

        intyg.id = testdataHelper.generateTestGuid();
        logger.debug('intyg.id = ' + intyg.id);
        return writeNewIntyg(typ, status);
    }
};
