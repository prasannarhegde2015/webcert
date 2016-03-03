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
'use strict';

var fk7263CheckValues = require('./fk.7263.js').checkValues;
var fkLUSECheckValues = require('./fk.LUSE.js').checkValues;
var fkLISUCheckValues = require('./fk.LISU.js').checkValues;

var tsCommonCheckValues = require('./ts.common.js').checkValues;
var tsBasCheckValues = require('./ts.bas.js').checkValues;
var tsDiabetesCheckValues = require('./ts.diabetes.js').checkValues;

module.exports = {
    fk: {
        '7263': fk7263CheckValues,
        'LUSE': fkLUSECheckValues,
        'LISU': fkLISUCheckValues
    },
    ts: {
        common: tsCommonCheckValues,
        bas: tsBasCheckValues,
        diabetes: tsDiabetesCheckValues
    },
    forIntyg: function(intyg, cb) {
        if (intyg.typ === 'Transportstyrelsens läkarintyg, diabetes') {
            tsCommonCheckValues(intyg, function(err) {
                tsDiabetesCheckValues(intyg, cb);
            });
        } else if (intyg.typ === 'Transportstyrelsens läkarintyg') {
            tsCommonCheckValues(intyg, function(err) {
                tsBasCheckValues(intyg, cb);
            });
        } else if (intyg.typ === 'Läkarintyg FK 7263') {
            fk7263CheckValues(intyg, cb);
        } else if (intyg.typ === 'Läkarutlåtande för sjukersättning') {
            fkLUSECheckValues(intyg, cb);
        } else if (intyg.typ === 'Läkarintyg för sjukpenning utökat') {
            fkLISUCheckValues(intyg, cb);
        } else {
            cb('Saknar värdecheckar för intygstyp: ' + intyg.typ);
        }
    }
};