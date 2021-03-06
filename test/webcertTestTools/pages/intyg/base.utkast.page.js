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
        this.fmbDialogs = {
            symptomPrognosBehandling: element(by.id('fmb_text_SYMPTOM_PROGNOS_BEHANDLING')),
            generellInfo: element(by.id('fmb_text_GENERELL_INFO')),
            funktionsnedsattning: element(by.id('fmb_text_FUNKTIONSNEDSATTNING')),
            aktivitetsbegransning: element(by.id('fmb_text_AKTIVITETSBEGRANSNING')),
            beslutsunderlag: element(by.id('fmb_text_BESLUTSUNDERLAG_TEXTUELLT'))
        };
        this.fmbButtons = {
            falt2: element(by.id('FALT2-fmb-button')),
            falt4: element(by.id('FALT4-fmb-button')),
            falt5: element(by.id('FALT5-fmb-button')),
            falt8: element(by.id('FALT8B-fmb-button'))
        };
        this.fmbAlertText = element(by.id('fmb_diagnos_not_in_fmb_alert'));
        this.at = null;
        this.signeraButton = element(by.id('signera-utkast-button'));
        this.radera = {
            knapp: element(by.id('ta-bort-utkast')),
            bekrafta: element(by.id('confirm-draft-delete-button'))
        };
        this.skrivUtBtn = element(by.id('skriv-ut-utkast'));
        this.signingDoctorName = element(by.id('signingDoctor'));

        this.newTextVersionAlert = element(by.id('newTextVersion'));
        this.backBtn = element(by.id('tillbakaButton'));
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
        browser.get('/web/dashboard#/' + intygType + '/edit/' + intygId + '/');
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
        this.signeraButton.sendKeys(protractor.Key.SPACE);
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

        var postAddrEL = this.enhetensAdress.postAdress;
        var postOrtEL = this.enhetensAdress.postOrt;
        var enhetTelefonEL = this.enhetensAdress.enhetsTelefon;
        var postNummerEL = this.enhetensAdress.postNummer;

        return Promise.all([
            postAddrEL.clear().then(function() {
                return postAddrEL.sendKeys(adressObj.postadress);
            }),
            postNummerEL.clear().then(function() {
                return postNummerEL.sendKeys(adressObj.postnummer);
            }),
            postOrtEL.clear().then(function() {
                return postOrtEL.sendKeys(adressObj.postort);
            }),
            enhetTelefonEL.clear().then(function() {
                return enhetTelefonEL.sendKeys(adressObj.telefon);
            })
        ]);
    },
    angePatientAdress: function(adressObj) {

        var postAddrEL = this.patientAdress.postAdress;
        var postNummerEL = this.patientAdress.postNummer;
        var postOrtEL = this.patientAdress.postOrt;

        return Promise.all([
            postAddrEL.clear().then(function() {
                return postAddrEL.sendKeys(adressObj.postadress);
            }),
            postNummerEL.clear().then(function() {
                return postNummerEL.sendKeys(adressObj.postnummer);
            }),
            postOrtEL.clear().then(function() {
                return postOrtEL.sendKeys(adressObj.postort);
            }),
        ]);


    }
});

module.exports = BaseUtkast;
