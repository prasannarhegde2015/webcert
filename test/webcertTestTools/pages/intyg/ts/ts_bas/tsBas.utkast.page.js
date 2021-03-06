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
 * Created by bennysce on 09/06/15.
 */
/*globals element,by,protractor, Promise,browser*/
'use strict';

// NOTE: This file is loaded before helpers in protractor.conf.js onPrepare. Therefore helpers are not available in file scope.
var BaseTsUtkast = require('../ts.base.utkast.page.js');
//var pageHelpers = require('../../../pageHelper.util.js');

var TsBasUtkast = BaseTsUtkast._extend({
    init: function init() {

        init._super.call(this);
        this.intygType = 'ts-bas';
        this.at = element(by.id('edit-ts-bas'));
        this.syn = {
            aYes: element(by.id('synay')),
            aNo: element(by.id('synan')),
            bYes: element(by.id('synby')),
            bNo: element(by.id('synbn')),
            cYes: element(by.id('syncy')),
            cNo: element(by.id('syncn')),
            dYes: element(by.id('syndy')),
            dNo: element(by.id('syndn')),
            eYes: element(by.id('syney')),
            eNo: element(by.id('synen')),
            hoger: {
                utan: element(by.id('synHogerOgaUtanKorrektion')),
                med: element(by.id('synHogerOgaMedKorrektion'))
            },
            vanster: {
                utan: element(by.id('synVansterOgaUtanKorrektion')),
                med: element(by.id('synVansterOgaMedKorrektion'))
            },
            binokulart: {
                utan: element(by.id('synBinokulartUtanKorrektion')),
                med: element(by.id('synBinokulartMedKorrektion'))
            },
            // utanKorrektion: {
            //     hoger: element(by.id('synHogerOgaUtanKorrektion')),
            //     vanster: element(by.id('synVansterOgaUtanKorrektion')),
            //     binokulart: element(by.id('synBinokulartUtanKorrektion'))
            // },
            // medKorrektion: {
            //     hoger: element(by.id('synHogerOgaMedKorrektion')),
            //     vanster: element(by.id('synVansterOgaMedKorrektion')),
            //     binokulart: element(by.id('synBinokulartMedKorrektion'))
            // },
            kontaktlins: {
                hoger: element(by.id('synHogerOgaKontaktlins')),
                vanster: element(by.id('synVasterOgaKontaktlins'))
            }
        };

        this.horselBalans = {
            aYes: element(by.id('horselbalansay')),
            aNo: element(by.id('horselbalansan')),
            bYes: element(by.id('horselbalansby')),
            bNo: element(by.id('horselbalansbn'))
        };
        this.funktionsnedsattning = {
            aYes: element(by.id('funktionsnedsattningay')),
            aNo: element(by.id('funktionsnedsattningan')),
            aText: element(by.id('funktionsnedsattning')),
            bYes: element(by.id('funktionsnedsattningby')),
            bNo: element(by.id('funktionsnedsattningbn'))
        };
        this.hjartKarl = {
            aYes: element(by.id('hjartkarlay')),
            aNo: element(by.id('hjartkarlan')),
            bYes: element(by.id('hjartkarlby')),
            bNo: element(by.id('hjartkarlbn')),
            cYes: element(by.id('hjartkarlcy')),
            cNo: element(by.id('hjartkarlcn')),
            cText: element(by.id('beskrivningRiskfaktorer'))
        };
        this.diabetes = {
            aYes: element(by.id('diabetesay')),
            aNo: element(by.id('diabetesan')),
            typ1: element(by.id('diabetestyp1')),
            typ2: element(by.id('diabetestyp2')),
            endastkost: element(by.id('diabetestreat1')),
            tabletter: element(by.id('diabetestreat2')),
            insulin: element(by.id('diabetestreat3'))
        };
        this.neurologiska = {
            aYes: element(by.id('neurologiay')),
            aNo: element(by.id('neurologian'))
        };
        this.epilepsi = {
            aYes: element(by.id('medvetandestorningay')),
            aText: element(by.id('beskrivningMedvetandestorning')),
            aNo: element(by.id('medvetandestorningan'))
        };
        this.njursjukdom = {
            aYes: element(by.id('njuraray')),
            aNo: element(by.id('njuraran'))
        };
        this.kognitivt = {
            aYes: element(by.id('kognitivtay')),
            aNo: element(by.id('kognitivtan'))
        };
        this.somnOchVakenhetsStorningar = {
            JA: element(by.css('input[name="somnvakenheta"][value="true"]')),
            NEJ: element(by.css('input[name="somnvakenheta"][value="false"]'))
        };
        this.patientAdress = {
            postAdress: element(by.id('patientPostadress')),
            postNummer: element(by.id('patientPostnummer')),
            postOrt: element(by.id('patientPostort'))
        };
    },
    fillInSynfunktioner: function(utkast) {
        var promiseArr = [];
        if (utkast.synDonder === 'Ja') {
            promiseArr.push(this.syn.aYes.sendKeys(protractor.Key.SPACE));
        } else {
            promiseArr.push(this.syn.aNo.sendKeys(protractor.Key.SPACE));
        }
        if (utkast.synNedsattBelysning === 'Ja') {
            promiseArr.push(this.syn.bYes.sendKeys(protractor.Key.SPACE));
        } else {
            promiseArr.push(this.syn.bNo.sendKeys(protractor.Key.SPACE));
        }
        if (utkast.synOgonsjukdom === 'Ja') {
            promiseArr.push(this.syn.cYes.sendKeys(protractor.Key.SPACE));
        } else {
            promiseArr.push(this.syn.cNo.sendKeys(protractor.Key.SPACE));
        }
        if (utkast.synDubbel === 'Ja') {
            promiseArr.push(this.syn.dYes.sendKeys(protractor.Key.SPACE));
        } else {
            promiseArr.push(this.syn.dNo.sendKeys(protractor.Key.SPACE));
        }
        if (utkast.synNystagmus === 'Ja') {
            promiseArr.push(this.syn.eYes.sendKeys(protractor.Key.SPACE));
        } else {
            promiseArr.push(this.syn.eNo.sendKeys(protractor.Key.SPACE));
        }
        if (utkast.linser.hoger === 'Ja') {
            promiseArr.push(this.syn.kontaktlins.hoger.sendKeys(protractor.Key.SPACE));

        }
        if (utkast.linser.vanster === 'Ja') {
            promiseArr.push(this.syn.kontaktlins.vanster.sendKeys(protractor.Key.SPACE));
        }

        promiseArr.push(this.syn.hoger.utan.sendKeys(utkast.styrkor.houk));
        promiseArr.push(this.syn.vanster.utan.sendKeys(utkast.styrkor.vouk));
        promiseArr.push(this.syn.binokulart.utan.sendKeys(utkast.styrkor.buk));

        promiseArr.push(this.syn.hoger.med.sendKeys(utkast.styrkor.homk));
        promiseArr.push(this.syn.vanster.med.sendKeys(utkast.styrkor.vomk));
        promiseArr.push(this.syn.binokulart.med.sendKeys(utkast.styrkor.bmk));

        return Promise.all(promiseArr);
    },
    fillInYrsel: function(yrsel) {
        if (yrsel === 'Ja') {
            return this.horselBalans.aYes.sendKeys(protractor.Key.SPACE);
        } else {
            return this.horselBalans.aNo.sendKeys(protractor.Key.SPACE);
        }
    },
    fillInHorselOchBalanssinne: function(horselObj) {
        var horselBalansEl = this.horselBalans;
        return this.fillInYrsel(horselObj.yrsel)
            .then(function() {
                if (horselObj.samtal === 'Ja') {
                    return horselBalansEl.bYes.sendKeys(protractor.Key.SPACE);
                } else if (horselObj.samtal === 'Nej') {
                    return horselBalansEl.bNo.sendKeys(protractor.Key.SPACE);
                }
            });
    },

    fillInRorelseNedsattning: function(nedsattning, beskrivning) {
        var nedsattningEl = this.funktionsnedsattning;

        if (nedsattning === 'Ja') {
            return nedsattningEl.aYes.sendKeys(protractor.Key.SPACE)
                .then(function() {
                    return nedsattningEl.aText.sendKeys(beskrivning ? beskrivning : 'Nedsattning text');
                });

        } else if (nedsattning === 'Nej') {
            return nedsattningEl.aNo.sendKeys(protractor.Key.SPACE);
        }
    },
    fillInRorelseorganensFunktioner: function(rorelseorganensFunktionerObj) {
        var nedsattningEl = this.funktionsnedsattning;
        var inUtUrFordon = rorelseorganensFunktionerObj.inUtUrFordon;

        return this.fillInRorelseNedsattning(rorelseorganensFunktionerObj.nedsattning, rorelseorganensFunktionerObj.nedsattningBeskrivning)
            .then(function() {
                if (inUtUrFordon === 'Ja') {
                    return nedsattningEl.bYes.sendKeys(protractor.Key.SPACE);
                } else if (inUtUrFordon === 'Nej') {
                    return nedsattningEl.bNo.sendKeys(protractor.Key.SPACE);
                }
            });
    },

    fillInHjartOchKarlsjukdomar: function(utkast) {
        var promiseArr = [];
        var hjartKarlCEl = this.hjartKarl;

        if (utkast.hjartHjarna === 'Ja') {
            promiseArr.push(hjartKarlCEl.aYes.sendKeys(protractor.Key.SPACE));
        } else {
            promiseArr.push(hjartKarlCEl.aNo.sendKeys(protractor.Key.SPACE));
        }
        if (utkast.hjartSkada === 'Ja') {
            promiseArr.push(hjartKarlCEl.bYes.sendKeys(protractor.Key.SPACE));
        } else {
            promiseArr.push(hjartKarlCEl.bNo.sendKeys(protractor.Key.SPACE));
        }
        if (utkast.hjartRisk === 'Ja') {
            promiseArr.push(hjartKarlCEl.cYes.sendKeys(protractor.Key.SPACE)
                .then(function() {
                    return browser.sleep(1000); // Testar att vänta på animering eller nästa tick
                })
                .then(function() {
                    return hjartKarlCEl.cText.sendKeys(utkast.hjartRiskBeskrivning);
                }));
        } else {
            promiseArr.push(hjartKarlCEl.cNo.sendKeys(protractor.Key.SPACE));
        }
        return Promise.all(promiseArr);
    },
    fillInDiabetes: function(diabetesObj) {
        var diabetes = this.diabetes;
        if (diabetesObj.hasDiabetes === 'Ja') {
            return diabetes.aYes.sendKeys(protractor.Key.SPACE).then(function() {

                if (diabetesObj.typ === 'Typ 1') {
                    return diabetes.typ1.sendKeys(protractor.Key.SPACE);
                } else {
                    return diabetes.typ2.sendKeys(protractor.Key.SPACE)
                        .then(function() {
                            // Ange behandlingstyp 
                            var promiseArr = [];
                            var typ = diabetesObj.behandlingsTyper;
                            if (typ.indexOf('Endast kost') > -1) {
                                promiseArr.push(diabetes.endastkost.sendKeys(protractor.Key.SPACE));
                            }
                            if (typ.indexOf('Tabletter') > -1) {
                                promiseArr.push(diabetes.tabletter.sendKeys(protractor.Key.SPACE));
                            }
                            if (typ.indexOf('Insulin') > -1) {
                                promiseArr.push(diabetes.insulin.sendKeys(protractor.Key.SPACE));
                            }
                            return Promise.all(promiseArr);
                        });
                }
            });
        } else {
            return diabetes.aNo.sendKeys(protractor.Key.SPACE);
        }

    },
    fillInNeurologiskaSjukdomar: function(utkast) {
        if (utkast.neurologiska === 'Ja') {
            return this.neurologiska.aYes.sendKeys(protractor.Key.SPACE);
        } else {
            return this.neurologiska.aNo.sendKeys(protractor.Key.SPACE);
        }
    },
    fillInEpilepsi: function(utkast) {
        var promiseArr = [];
        if (utkast.epilepsi === 'Ja') {
            promiseArr.push(this.epilepsi.aYes.sendKeys(protractor.Key.SPACE));
            promiseArr.push(this.epilepsi.aText.sendKeys(utkast.epilepsiBeskrivning));
        } else {
            promiseArr.push(this.epilepsi.aNo.sendKeys(protractor.Key.SPACE));
        }
        return Promise.all(promiseArr);
    },
    fillInNjursjukdomar: function(utkast) {
        if (utkast.njursjukdom === 'Ja') {
            return this.njursjukdom.aYes.sendKeys(protractor.Key.SPACE);
        } else {
            return this.njursjukdom.aNo.sendKeys(protractor.Key.SPACE);
        }
    },
    fillInDemens: function(utkast) {
        if (utkast.demens === 'Ja') {
            return this.kognitivt.aYes.sendKeys(protractor.Key.SPACE);
        } else {
            return this.kognitivt.aNo.sendKeys(protractor.Key.SPACE);
        }
    },
    fillInSomnOchVakenhet: function(utkast) {
        if (utkast.somnVakenhet === 'Ja') {
            return this.somnOchVakenhetsStorningar.JA.sendKeys(protractor.Key.SPACE);
        } else {
            return this.somnOchVakenhetsStorningar.NEJ.sendKeys(protractor.Key.SPACE);
        }
    },
    fillInAlkoholNarkotikaLakemedel: function(utkast) {
        var promiseArr = [];

        promiseArr.push(element.all(by.css('[name="narkotikalakemedela"]')).then(function(elm) {
            if (utkast.alkoholMissbruk === 'Ja') {
                return elm[0].sendKeys(protractor.Key.SPACE);
            } else {
                return elm[1].sendKeys(protractor.Key.SPACE);
            }
        }));
        promiseArr.push(element.all(by.css('[name="narkotikalakemedelb"]')).then(function(elm) {
            if (utkast.alkoholVard === 'Ja') {
                return elm[0].sendKeys(protractor.Key.SPACE);
            } else {
                return elm[1].sendKeys(protractor.Key.SPACE);
            }
        }));

        promiseArr.push(element.all(by.css('[name="narkotikalakemedelc"]')).then(function(elm) {
            if (utkast.alkoholLakemedel === 'Ja') {
                return Promise.all([
                    elm[0].sendKeys(protractor.Key.SPACE),
                    element(by.id('beskrivningNarkotikalakemedel')).sendKeys(utkast.alkoholLakemedelBeskrivning)
                ]);
            } else {
                return elm[1].sendKeys(protractor.Key.SPACE);
            }
        }));

        promiseArr.push(element.all(by.css('[name="narkotikalakemedelb2"]')).then(function(elm) {
            if (utkast.alkoholMissbruk === 'Ja' || utkast.alkoholVard === 'Ja') {
                if (utkast.alkoholProvtagning === 'Ja') {
                    return elm[0].sendKeys(protractor.Key.SPACE);
                } else {
                    return elm[1].sendKeys(protractor.Key.SPACE);
                }
            }
        }));

        return Promise.all(promiseArr);

    },
    fillInPsykiska: function(utkast) {
        return element.all(by.css('[name="psykiskta"]')).then(function(elm) {
            if (utkast.psykiskSjukdom === 'Ja') {
                return elm[0].sendKeys(protractor.Key.SPACE);
            } else {
                return elm[1].sendKeys(protractor.Key.SPACE);
            }
        });
    },
    fillInAdhd: function(utkast) {
        var promiseArr = [];

        var a = element.all(by.css('[name="utvecklingsstorninga"]')).then(function(elm) {
            if (utkast.adhdPsykisk === 'Ja') {
                return elm[0].sendKeys(protractor.Key.SPACE);
            } else {
                return elm[1].sendKeys(protractor.Key.SPACE);
            }
        });

        promiseArr.push(a);

        var b = element.all(by.css('[name="utvecklingsstorningb"]')).then(function(elm) {
            if (utkast.adhdSyndrom === 'Ja') {
                return elm[0].sendKeys(protractor.Key.SPACE);
            } else {
                return elm[1].sendKeys(protractor.Key.SPACE);
            }
        });

        promiseArr.push(b);

        return Promise.all(promiseArr);
    },
    fillInSjukhusvard: function(utkast) {
        return element.all(by.css('[name="sjukhusvarda"]')).then(function(elm) {
            if (utkast.sjukhusvard === 'Ja') {
                var promiseArr = [];
                promiseArr.push(elm[0].sendKeys(protractor.Key.SPACE));
                promiseArr.push(element(by.id('tidpunkt')).sendKeys(utkast.sjukhusvardTidPunkt));
                promiseArr.push(element(by.id('vardinrattning')).sendKeys(utkast.sjukhusvardInrattning));
                promiseArr.push(element(by.id('anledning')).sendKeys(utkast.sjukhusvardAnledning));
                return Promise.all(promiseArr);
            } else {
                return elm[1].sendKeys(protractor.Key.SPACE);
            }
        });
    },
    fillInOvrigMedicinering: function(utkast) {
        return element.all(by.css('[name="medicineringa"]')).then(function(elm) {
            if (utkast.ovrigMedicin === 'Ja') {
                return Promise.all([
                    elm[0].sendKeys(protractor.Key.SPACE),
                    element(by.id('beskrivningMedicinering')).sendKeys(utkast.ovrigMedicinBeskrivning)
                ]);
            } else {
                return elm[1].sendKeys(protractor.Key.SPACE);
            }
        });
    }
});

module.exports = new TsBasUtkast();
