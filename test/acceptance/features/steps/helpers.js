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

/*global testdata,intyg,logger,pages,Promise,browser,commonTools, person*/
'use strict';
// var fkIntygPage = pages.intyg.fk['7263'].intyg;
var fkLusePage = pages.intyg.luse.intyg;
var mysql = require('mysql');

function sh(value) {
    return (value.search(/\s-\s/g) !== -1) ? value.split(/\s-\s/g)[0].replace('Ämne: ', '') : value.split(/\n/g)[0].replace('Ämne: ', '');
}

module.exports = {
    insertDashInPnr: function(pnrString) {
        if (pnrString.indexOf('-') >= 0) {
            return pnrString;
        }
        return pnrString.slice(0, 8) + '-' + pnrString.slice(8);
    },
    intygShortcode: commonTools.helpers.intygShortcode,


    //TODO Kan vi hantera detta bättre, Om HSA ändras så behöver vi uppdatera denna data vilket inte är optimalt
    // TSTNMT2321000156-ULLA saknar enhetadress i hsa, dvs behåll tidigare angivet enhetAdress objekt
    updateEnhetAdressForNewIntyg: function() {

        if (global.user.enhetId !== 'TSTNMT2321000156-ULLA') {
            global.user.enhetsAdress = {
                postnummer: '65340',
                postort: 'Karlstad',
                postadress: 'Bryggaregatan 11',
                telefon: '054100000'
            };

            if (global.user.enhetId === 'TSTNMT2321000156-107P') {
                global.user.enhetsAdress.telefon = '054121314';
            }

        }
    },
    generateIntygByType: function(typ, id) {
        if (typ === 'Transportstyrelsens läkarintyg') {
            return testdata.ts.bas.getRandom(id, person);
        } else if (typ === 'Transportstyrelsens läkarintyg, diabetes') {
            return testdata.ts.diabetes.getRandom(id, person);
        } else if (typ === 'Läkarintyg FK 7263') {
            return testdata.fk['7263'].getRandom(id);
        } else if (typ === 'Läkarutlåtande för sjukersättning') {
            return testdata.fk.LUSE.getRandom(id);
        } else if (typ === 'Läkarintyg för sjukpenning') {
            return testdata.fk.LISJP.getRandom(id);
        } else if (typ === 'Läkarutlåtande för aktivitetsersättning vid nedsatt arbetsförmåga') {
            return testdata.fk.LUAE_NA.getRandom(id);
        } else if (typ === 'Läkarutlåtande för aktivitetsersättning vid förlängd skolgång') {
            return testdata.fk.LUAE_FS.getRandom(id);
        }
    },
    fetchMessageIds: function(intygtyp) {
        console.log('Hämtar meddelande-id:n');

        // var isSMIIntyg = this.isSMIIntyg(intygtyp);

        if (!intyg.messages) {
            intyg.messages = [];
        }
        var panels = fkLusePage.qaPanels;

        if (!panels) {
            return Promise.resolve('Inga frågor hittades');
        } else {
            var messageIdAttributes = panels.map(function(elm) {
                return Promise.all([
                    elm.getAttribute('id'),
                    elm.element(by.css('.fraga-status-header')).getText()
                ]);
            });

            return messageIdAttributes.then(function(result) {
                for (var i = 0; i < result.length; i++) {
                    var messageId, messageAmne;
                    var idAttr = result[i][0];
                    var headerText = result[i][1];
                    var isHandled = false;

                    messageId = idAttr.replace('arende-unhandled-', '');

                    //Är ärende hanterat?
                    isHandled = (messageId.indexOf('arende-handled') === 0);
                    messageId = messageId.replace('arende-handled-', '');

                    //Fånga ämne
                    messageAmne = sh(headerText);

                    logger.info('Meddelanden som finns på intyget: ' + messageId + ', ' + messageAmne + ' Hanterad:' + isHandled);
                    intyg.messages.push({
                        id: messageId,
                        amne: messageAmne,
                        isHandled: isHandled
                    });
                }
            });
        }

    },
    stripTrailingSlash: function(str) {
        if (str.substr(-1) === '/') {
            return str.substr(0, str.length - 1);
        }
        return str;
    },
    getIntygElementRow: function(intygstyp, status, cb) {
        var qaTable = element(by.css('table.table-qa'));

        var connection = mysql.createConnection({
            host: 'mysql.ip30.nordicmedtest.se',
            user: process.env.DATABASE_USER,
            password: process.env.DATABASE_PASSWORD,
            // database: process.env.DATABASE_NAME,
            multipleStatements: true
        });

        qaTable.all(by.cssContainingText('tr', status)).filter(function(elem, index) {
                return elem.all(by.css('td')).get(2).getText().then(function(text) {
                    return (text === intygstyp);
                });
            })
            // Kontrollera att intyget ej är ersatt
            // Förhoppningsvis temporärt tills vi har någon label att gå på istället
            .filter(function(el, i) {
                return el.element(by.cssContainingText('button', 'Visa')).getAttribute('id')
                    .then(function(id) {
                        id = id.replace('showBtn-', '');
                        var query = 'SELECT RELATION_KOD FROM webcert_ip30.INTYG where RELATION_INTYG_ID = "' + id + '"  AND RELATION_KOD = "ERSATT"';

                        return new Promise(function(resolve, reject) {
                            connection.query(query,
                                function(err, rows, fields) {
                                    if (err) {
                                        throw (err);
                                    }
                                    resolve(rows.length <= 0);
                                });

                        });
                    });
            })
            .then(function(filteredElements) {
                connection.end();
                cb(filteredElements[0]);
            });
    },
    getAbbrev: function(value) {
        for (var key in this.intygShortcode) {
            if (this.intygShortcode[key] === value) {
                return key.toString();
            }
        }
        return null;
    },

    isSMIIntyg: function(intygsType) {
        var regex = /(Läkarintyg för|Läkarutlåtande för)/g;
        return (intygsType) ? (intygsType.match(regex) ? true : false) : false;
    },
    isTSIntyg: function(intygsType) {
        return intygsType.indexOf('Transportstyrelsen') > -1;
    },
    subjectCodes: {
        'Komplettering': 'KOMPLT',
        'Paminnelse': 'PAMINN',
        'Arbetstidsförläggning': 'ARBTID',
        'Avstämningsmöte': 'AVSTMN',
        'Kontakt': 'KONTKT',
        'Övrigt': 'OVRIGT'
    },
    subjectCodesFK7263: {
        'Avstämningsmöte': 'Avstamningsmote',
        'Kontakt': 'Kontakt',
        'Arbetstidsförläggning': 'Arbetstidsforlaggning',
        'Påminnelse': 'Paminnelse',
        'Komplettering': 'Komplettering_av_lakarintyg'
    },
    getSubjectFromCode: function(value, isFK7263) {
        var subjectCodes = this.subjectCodes;

        if (isFK7263) {
            subjectCodes = this.subjectCodesFK7263;
        }

        for (var key in subjectCodes) {
            if (subjectCodes[key] === value) {
                return key.toString();
            }
        }
        return null;
    },
    splitHeader: function(value) {
        return sh(value);
    },
    randomTextString: function() {
        var text = '';
        var possible = 'ABCDEFGHIJKLMNOPQRSTUVWXYZÅÄÖabcdefghijklmnopqrstuvwxyzåäö0123456789';

        for (var i = 0; i < 16; i++) {
            text += possible.charAt(Math.floor(Math.random() * possible.length));
        }
        return text;
    },
    randomPageField: function(isSMIIntyg, intygAbbrev) {
        var index = Math.floor(Math.random() * 3);
        if (isSMIIntyg) {
            if (intygAbbrev === 'LISJP') {
                return this.pageField[intygAbbrev][index];
            } else if (intygAbbrev === 'LUSE') {
                return this.pageField[intygAbbrev][index];
            } else if (intygAbbrev === 'LUAE_NA') {
                return this.pageField[intygAbbrev][index];
            } else if (intygAbbrev === 'LUAE_FS') {
                return this.pageField[intygAbbrev][index];
            }
        } else if (intygAbbrev === 'TSTRK1007') {
            return this.pageField.TSTRK1007[index];
        } else {
            return this.pageField.FK7263[index];
        }

    },
    pageField: {
        'LISJP': ['aktivitetsbegransning', 'sysselsattning', 'funktionsnedsattning'],
        'LUSE': ['aktivitetsbegransning', 'sjukdomsforlopp', 'funktionsnedsattning'],
        'LUAE_NA': ['aktivitetsbegransning', 'sjukdomsforlopp', 'ovrigt'],
        'LUAE_FS': ['funktionsnedsattningDebut', 'funktionsnedsattningPaverkan', 'ovrigt'],
        'FK7263': ['aktivitetsbegransning', 'funktionsnedsattning', 'diagnoskod'],
        'TSTRK1007': ['funktionsnedsattning', 'hjartKarlsjukdom', 'utanKorrektion']

    },
    getUserObj: function(userKey) {
        return this.userObj[userKey];
    },

    diffDays: function(dateFrom, dateTo) {
        var fromEl = dateFrom.split('-');
        var toEl = dateTo.split('-');

        var oneDay = 24 * 60 * 60 * 1000; // hours*minutes*seconds*milliseconds

        var firstDate = new Date(fromEl[0], fromEl[1], fromEl[2]);
        var secondDate = new Date(toEl[0], toEl[1], toEl[2]);

        return Math.round(Math.abs((firstDate.getTime() - secondDate.getTime()) / (oneDay)));
    },
    injectConsoleTracing: function() {
        return browser.executeScript('window.errs=typeof(errs)=="undefined" ? [] : window.errs; window.console.error = function(msg){window.errs.push(msg); }; ');
    },

    intygURL: function(typAvIntyg, intygsId) {
        if (typAvIntyg === 'Läkarutlåtande för sjukersättning') {
            return process.env.WEBCERT_URL + 'web/dashboard#/intyg/luse/' + intygsId;
        } else if (typAvIntyg === 'Läkarintyg för sjukpenning') {
            return process.env.WEBCERT_URL + 'web/dashboard#/intyg/lisjp/' + intygsId;
        } else if (typAvIntyg === 'Läkarutlåtande för aktivitetsersättning vid förlängd skolgång') {
            return process.env.WEBCERT_URL + 'web/dashboard#/intyg/luae_fs/' + intygsId;
        } else if (typAvIntyg === 'Läkarutlåtande för aktivitetsersättning vid nedsatt arbetsförmåga') {
            return process.env.WEBCERT_URL + 'web/dashboard#/intyg/luae_na/' + intygsId;
        } else if (typAvIntyg === 'Läkarintyg FK 7263') {
            return process.env.WEBCERT_URL + 'web/dashboard#/intyg/fk7263/' + intygsId;
        } else if (typAvIntyg === 'Transportstyrelsens läkarintyg, diabetes') {
            return process.env.WEBCERT_URL + 'web/dashboard#/intyg/ts-diabetes/' + intygsId;
        } else if (typAvIntyg === 'Transportstyrelsens läkarintyg') {
            return process.env.WEBCERT_URL + 'web/dashboard#/intyg/ts-bas/' + intygsId;
        }
    }
};
