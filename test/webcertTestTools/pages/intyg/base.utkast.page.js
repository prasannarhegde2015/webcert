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

/**
 * Created by bennysce on 02-12-15.
 */
/*globals browser,protractor, Promise*/
'use strict';

var JClass = require('jclass');
var EC = protractor.ExpectedConditions;
var BaseUtkast = JClass._extend({
    init: function() {
        this.at = null;
        this.signeraButton = element(by.id('signera-utkast-button'));
        this.radera = {
            knapp: element(by.id('ta-bort-utkast')),
            bekrafta: element(by.id('confirm-draft-delete-button'))
        };
        this.newTextVersionAlert = element(by.id('newTextVersion'));
        this.backBtn = element(by.id('tillbakaButton'));
        this.showMissingInfoButton = element(by.id('showCompleteButton'));
        this.showMissingInfoList = element(by.id('visa-vad-som-saknas-lista'));
        this.patientNamnPersonnummer = element(by.id('patientNamnPersonnummer'));
        this.sparatOchKomplettMeddelande = element(by.id('intyget-sparat-och-komplett-meddelande'));
        this.enhetensAdress = {
            postAdress: element(by.id('clinicInfoPostalAddress')),
            postNummer: element(by.id('clinicInfoPostalCode')),
            postOrt: element(by.id('clinicInfoPostalCity')),
            enhetsTelefon: element(by.id('clinicInfoPhone'))
        };
        this.patientAdress = {
            postAdress: element(by.id('grundData.patient.postadress')),
            postNummer: element(by.id('grundData.patient.postnummer')),
            postOrt: element(by.id('grundData.patient.postort'))
        };
    },
    get: function(intygType, intygId) {
        browser.get('/web/dashboard#/' + intygType + '/edit/' + intygId);
    },
    isAt: function() {
        return this.at.isDisplayed();
    },
    isSigneraButtonEnabled: function() {
        return this.signeraButton.isEnabled();
    },
    whenSigneraButtonIsEnabled: function() {
        return browser.wait(EC.elementToBeClickable(this.signeraButton), 5000);
    },
    signeraButtonClick: function() {
        this.signeraButton.click();
    },
    showMissingInfoButtonClick: function(optional) {
        if (optional) {
            var button = this.showMissingInfoButton;
            button.isPresent().then(function(result) {
                if (result) {
                    button.click();
                }
            });
        } else {
            this.showMissingInfoButton.click();
        }
    },
    getMissingInfoMessagesCount: function() {
        return this.showMissingInfoList.all(by.tagName('a')).then(function(items) {
            return items.length;
        });
    },
    enableAutosave: function() {
        browser.executeScript(function() {
            window.autoSave = true;
        });
    },
    disableAutosave: function() {
        browser.executeScript(function() {
            window.autoSave = false;
        });
    },
    angeEnhetAdress: function(adressObj) {
        return Promise.all([
            this.enhetensAdress.postAdress.clear().sendKeys(adressObj.postadress),
            this.enhetensAdress.postNummer.clear().sendKeys(adressObj.postnummer),
            this.enhetensAdress.postOrt.clear().sendKeys(adressObj.postort),
            this.enhetensAdress.enhetsTelefon.clear().sendKeys(adressObj.telefon)
        ]);
    },
    angePatientAdress: function(adressObj) {
        return Promise.all([
            this.patientAdress.postAdress.clear().sendKeys(adressObj.postadress),
            this.patientAdress.postNummer.clear().sendKeys(adressObj.postnummer),
            this.patientAdress.postOrt.clear().sendKeys(adressObj.postort)
        ]);


    }
});

module.exports = BaseUtkast;
