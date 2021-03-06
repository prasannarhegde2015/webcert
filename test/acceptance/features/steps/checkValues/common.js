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

/* globals pages*/
/* globals logger, Promise */

'use strict';

var intygPage;

function checkEnhetAdress(adressObj) {
    return Promise.all([
        expect(intygPage.enhetsAdress.postAdress.getText()).to.eventually.contain(adressObj.postadress),
        expect(intygPage.enhetsAdress.postNummer.getText()).to.eventually.contain(adressObj.postnummer),
        expect(intygPage.enhetsAdress.postOrt.getText()).to.eventually.contain(adressObj.postort),
        expect(intygPage.enhetsAdress.enhetsTelefon.getText()).to.eventually.contain(adressObj.telefon)
    ]);
}


module.exports = {
    checkValues: function(intyg) {
        intygPage = pages.getIntygPageByType(intyg.typ);

        return Promise.all([
            checkEnhetAdress(global.user.enhetsAdress).then(function(value) {
                logger.info('OK - checkEnhetAdress = ' + value);
            }, function(reason) {
                throw ('FEL - checkEnhetAdress: ' + reason);
            })
        ]);

    },

    regExp: function(regexp) {
        return new RegExp(regexp, 'g');
    }
};
