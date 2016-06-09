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
module.exports.CreateDraftCertificateV2 = function(pnr, user, intygstyp) {
    var shortcodes = {
        'Läkarintyg för sjukpenning utökat': 'LISU',
        'Läkarutlåtande för sjukersättning': 'LUSE'
    };
    var typCode = shortcodes[intygstyp];



    return '<urn1:CreateDraftCertificate' +
        ' xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"' +
        ' xmlns:urn="urn:riv:itintegration:registry:1"' +
        ' xmlns:urn1="urn:riv:clinicalprocess:healthcond:certificate:CreateDraftCertificateResponder:2"' +
        ' xmlns:urn2="urn:riv:clinicalprocess:healthcond:certificate:types:2"' +
        ' xmlns:urn3="urn:riv:clinicalprocess:healthcond:certificate:2">' +
        '<urn1:intyg>' +
        '<urn1:typAvIntyg>' +
        '<urn2:code>' + typCode + '</urn2:code>' +
        '<urn2:codeSystem>f6fb361a-e31d-48b8-8657-99b63912dd9b</urn2:codeSystem>' +
        '<urn2:displayName>' + intygstyp + '</urn2:displayName>' +
        '</urn1:typAvIntyg>' +
        '<urn1:patient>' +
        '<urn3:person-id>' +
        '<urn2:root>1.2.752.129.2.1.3.1</urn2:root>' +
        '<urn2:extension>' + pnr.replace('-', '') + '</urn2:extension>' +
        '</urn3:person-id>' +
        '<urn3:fornamn>test</urn3:fornamn>' +
        '<urn3:efternamn>testsson</urn3:efternamn>' +
        '<urn3:postadress>Langgatan 12</urn3:postadress>' +
        '<urn3:postnummer>990 90</urn3:postnummer>' +
        '<urn3:postort>Simrishamn</urn3:postort>' +
        '</urn1:patient>' +
        '<urn1:skapadAv>' +
        '<urn1:personal-id>' +
        '<urn2:root>1.2.752.129.2.1.4.1</urn2:root>' +
        '<urn2:extension>' + user.hsaId + '</urn2:extension>' +
        '</urn1:personal-id>' +
        '<urn1:fullstandigtNamn>' + user.fornamn + '' + user.efternamn + '</urn1:fullstandigtNamn>' +
        '<urn1:enhet>' +
        '<urn1:enhets-id>' +
        '<urn2:root>1.2.752.129.2.1.4.1</urn2:root>' +
        '<urn2:extension>' + user.enhetId + '</urn2:extension>' +
        '</urn1:enhets-id>' +
        '<urn1:enhetsnamn>Enhetsnamn</urn1:enhetsnamn>' +
        '</urn1:enhet>' +
        '</urn1:skapadAv>' +
        '</urn1:intyg>' +
        '</urn1:CreateDraftCertificate>';
};