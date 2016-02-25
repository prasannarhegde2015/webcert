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

/* globals pages, protractor, person, browser, intyg, logger*/

'use strict';

var fk7263Utkast = pages.intyg.fk['7263'].utkast;
var fk7263Intyg = pages.intyg.fk['7263'].intyg;
var sokSkrivIntygUtkastTypePage = pages.sokSkrivIntyg.valjUtkastType;
var sokSkrivIntygPage = pages.sokSkrivIntyg.pickPatient;
var webcertBase = pages.webcertBase;
// webcertBase.flikarsokSkrivIntyg

module.exports = function() {

    this.Then(/^vill jag vara inlogger.infoad$/, function(callback) {
        expect(webcertBase.header.getText()).to.eventually.contain('logger.infoa ut').and.notify(callback);
        // expect(element(by.id('wcHeader')).getText()).to.eventually.contain('logger.infoa ut').and.notify(callback);
    });

    this.When(/^jag väljer patienten "([^"]*)"$/, function(personnummer, callback) {
        person.id = personnummer;

        if (global.user.origin !== 'DJUPINTEGRATION') {
            element(by.id('menu-skrivintyg')).sendKeys(protractor.Key.SPACE);
            browser.sleep(1000);
        }
        sokSkrivIntygPage.selectPersonnummer(personnummer);
        //Patientuppgifter visas
        var patientUppgifter = element(by.cssContainingText('.form-group', 'Patientuppgifter'));
        expect(patientUppgifter.getText()).to.eventually.contain(personnummer).and.notify(callback);
    });

    this.Given(/^jag går in på att skapa ett "([^"]*)" intyg$/, function(intygsTyp, callback) {
        logger.info('intygstyp: ' + intygsTyp);
        intyg.typ = intygsTyp;
        sokSkrivIntygUtkastTypePage.selectIntygTypeByLabel(intygsTyp);
        sokSkrivIntygUtkastTypePage.intygTypeButton.sendKeys(protractor.Key.SPACE);

        // Save INTYGS_ID:
        browser.getCurrentUrl().then(function(text) {
            intyg.id = text.split('/').slice(-1)[0];
            logger.debug(intyg.id);
        });
        callback();
    });

    this.Then(/^ska intygets status vara "([^"]*)"$/, function(statustext, callback) {
        expect(fk7263Intyg.intygStatus.getText()).to.eventually.contain(statustext).and.notify(callback);
        // expect(element(by.id('intyg-vy-laddad')).getText()).to.eventually.contain(statustext).and.notify(callback);
    });

    this.Then(/^(?:ska jag|jag ska) se den data jag angett för intyget$/, function(callback) {
        if (intyg.typ === 'Transportstyrelsens läkarintyg, diabetes' || intyg.typ === 'Transportstyrelsens läkarintyg') {
            logger.info('-- Kontrollerar Transportstyrelsens läkarintyg, diabetes & Transportstyrelsens läkarintyg (gemensama fält) --');
            require('./checkValues/ts.common.js').checkTsCommonValues(intyg, callback);
        }

        if (intyg.typ === 'Transportstyrelsens läkarintyg, diabetes') {
            logger.info('-- Kontrollerar Transportstyrelsens läkarintyg, diabetes --');
            require('./checkValues/ts.diabetes.js').checkTsDiabetesValues(intyg, callback);
        } else if (intyg.typ === 'Transportstyrelsens läkarintyg') {
            logger.info('-- Kontrollerar Transportstyrelsens läkarintyg --');
            require('./checkValues/ts.bas.js').checkTsBasValues(intyg, callback);
        } else if (intyg.typ === 'Läkarintyg FK 7263') {
            logger.info('-- Kontrollerar Läkarintyg FK 7263 --');
            require('./checkValues/fk.js').checkFKValues(intyg, callback);
        } else if (intyg.typ === 'Läkarutlåtande för sjukersättning') {
            logger.info('-- Kontrollerar Läkarutlåtande för sjukersättning --');
            require('./checkValues/luse.js').checkLuseValues(intyg, callback);
        }
    });

    this.Given(/^ska signera\-knappen inte vara synlig$/, function(callback) {
        expect(fk7263Utkast.signeraButton.isPresent()).to.eventually.become(false).and.notify(callback);
    });

};