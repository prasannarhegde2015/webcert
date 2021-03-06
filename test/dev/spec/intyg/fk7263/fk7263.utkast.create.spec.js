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

/*globals describe,it,browser */
'use strict';
var wcTestTools = require('webcert-testtools');
var specHelper = wcTestTools.helpers.spec;
var testdataHelper = wcTestTools.helpers.restTestdata;
var UtkastPage = wcTestTools.pages.intyg.fk['7263'].utkast;
var IntygPage = wcTestTools.pages.intyg.fk['7263'].intyg;
var ValjIntygPage = wcTestTools.pages.sokSkrivIntyg.pickPatient;

describe('Create and Sign FK utkast', function() {

    var utkastId = null,
        utkastIdSmittSkydd = null,
        data = null;

    browser.ignoreSynchronization = true;

    beforeAll(function() {
        browser.ignoreSynchronization = false;
        specHelper.login();
    });

    describe('Smittskydd', function() {
        beforeAll(function() {
            specHelper.createUtkastForPatient('191212121212', 'Läkarintyg FK 7263');
        });

        describe('Fyll i intyget', function() {

            it('Spara undan intygsId från URL', function() {
                UtkastPage.disableAutosave();

                browser.getCurrentUrl().then(function(url) {
                    utkastIdSmittSkydd = url.split('/').reverse()[1];
                });
                data = wcTestTools.testdata.fk['7263'].getRandom(utkastIdSmittSkydd, true);
            });

            it('angeSmittskydd', function() {
                UtkastPage.angeSmittskydd(data.smittskydd);
            });
            it('angeArbetsformaga', function() {
                UtkastPage.angeArbetsformaga(data.arbetsformaga);
            });
            it('angeArbetsformagaFMB', function() {
                UtkastPage.angeArbetsformagaFMB(data.arbetsformagaFMB);
            });
            it('angePrognos', function() {
                UtkastPage.angePrognos(data.prognos);
            });
            it('angeKontaktOnskasMedFK', function() {
                UtkastPage.enableAutosave();
                UtkastPage.angeKontaktOnskasMedFK(data.kontaktOnskasMedFK);
            });
            it('angeOvrigaUpplysningar', function() {
                UtkastPage.angeOvrigaUpplysningar(data.ovrigaUpplysningar);
            });
        });

        it('Signera intyget', function() {
            UtkastPage.whenSigneraButtonIsEnabled();

            browser.sleep(1000);

            UtkastPage.signeraButtonClick();

            browser.sleep(1000);

            expect(IntygPage.isAt()).toBeTruthy();
        });

        it('Verifiera intyg', function() {
            IntygPage.verify(data);
        });
    });

    describe('Vanligt', function() {
        beforeAll(function() {
            browser.ignoreSynchronization = false;
            ValjIntygPage.get();
            specHelper.createUtkastForPatient('191212121212', 'Läkarintyg FK 7263');
        });

        describe('Fyll i intyget', function() {

            it('Spara undan intygsId från URL', function() {
                UtkastPage.disableAutosave();

                browser.getCurrentUrl().then(function(url) {
                    utkastId = url.split('/').pop();
                });
                data = wcTestTools.testdata.fk['7263'].getRandom(utkastId, false);
            });

            it('angeIntygetBaserasPa', function() {
                UtkastPage.angeIntygetBaserasPa(data.baserasPa);
            });
            it('angeDiagnos', function() {
                UtkastPage.angeDiagnoser(data.diagnos);
            });
            it('angeAktuelltSjukdomsForlopp', function() {
                UtkastPage.angeAktuelltSjukdomsForlopp(data.aktuelltSjukdomsforlopp);
            });
            it('angeFunktionsnedsattning', function() {
                UtkastPage.angeFunktionsnedsattning(data.funktionsnedsattning);
            });
            it('angeAktivitetsBegransning', function() {
                UtkastPage.angeAktivitetsBegransning(data.aktivitetsBegransning);
            });
            it('angeArbete', function() {
                UtkastPage.angeArbete(data.arbete);
            });
            it('angeArbetsformaga', function() {
                UtkastPage.angeArbetsformaga(data.arbetsformaga);
            });
            it('angeArbetsformagaFMB', function() {
                UtkastPage.angeArbetsformagaFMB(data.arbetsformagaFMB);
            });
            it('angePrognos', function() {
                UtkastPage.angePrognos(data.prognos);
            });
            it('angeAtgarder', function() {
                UtkastPage.angeAtgarder(data.atgarder);
            });
            it('angeRekommendationer', function() {
                UtkastPage.angeRekommendationer(data.rekommendationer);
            });
            it('angeKontaktOnskasMedFK', function() {
                UtkastPage.enableAutosave();
                UtkastPage.angeKontaktOnskasMedFK(data.kontaktOnskasMedFK);
            });
            it('angeOvrigaUpplysningar', function() {
                UtkastPage.angeOvrigaUpplysningar(data.ovrigaUpplysningar);
            });
        });

        it('Signera intyget', function() {
            UtkastPage.whenSigneraButtonIsEnabled();

            browser.sleep(1000);

            UtkastPage.signeraButtonClick();

            browser.sleep(1000);

            expect(IntygPage.isAt()).toBeTruthy();
        });

        it('Verifiera intyg', function() {
            IntygPage.whenCertificateLoaded();

            IntygPage.verify(data);
        });
    });

    afterAll(function() {
        browser.sleep(1000);
        testdataHelper.deleteIntyg(utkastIdSmittSkydd);
        testdataHelper.deleteUtkast(utkastIdSmittSkydd);
        testdataHelper.deleteIntyg(utkastId);
        testdataHelper.deleteUtkast(utkastId);
    });
});
