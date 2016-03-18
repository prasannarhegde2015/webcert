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

package se.inera.intyg.webcert.web.converter;

import se.inera.intyg.webcert.persistence.arende.model.Arende;
import se.inera.intyg.webcert.persistence.fragasvar.model.FragaSvar;
import se.inera.intyg.webcert.web.web.controller.api.dto.ArendeMetaData;

public final class ArendeMetaDataConverter {

    private ArendeMetaDataConverter() {
    }

    public static ArendeMetaData convert(FragaSvar fragaSvar) {
        if (fragaSvar.getIntygsReferens() == null || fragaSvar.getVardperson() == null) {
            return null;
        }
        ArendeMetaData res = new ArendeMetaData();
        res.setFragestallare(fragaSvar.getFrageStallare());
        res.setIntygId(fragaSvar.getIntygsReferens().getIntygsId());
        res.setIntygTyp(fragaSvar.getIntygsReferens().getIntygsTyp());
        res.setMeddelandeId(Long.toString(fragaSvar.getInternReferens()));
        res.setReceivedDate(fragaSvar.getSenasteHandelseDatum());
        res.setPatientId(fragaSvar.getIntygsReferens().getPatientId().getPersonnummer());
        res.setSigneratAv(fragaSvar.getVardperson().getNamn());
        res.setStatus(fragaSvar.getStatus());
        res.setVidarebefordrad(fragaSvar.getVidarebefordrad());
        res.setAmne(fragaSvar.getAmne().name());
        res.setEnhetsnamn(fragaSvar.getVardperson().getEnhetsnamn());
        res.setVardgivarnamn(fragaSvar.getVardperson().getVardgivarnamn());
        return res;
    }

    public static ArendeMetaData convert(Arende arende) {
        ArendeMetaData res = new ArendeMetaData();
        res.setAmne(arende.getAmne().name());
        res.setFragestallare(arende.getSkickatAv());
        res.setIntygId(arende.getIntygsId());
        res.setIntygTyp(arende.getIntygTyp());
        res.setMeddelandeId(arende.getMeddelandeId());
        res.setPatientId(arende.getPatientPersonId());
        res.setReceivedDate(arende.getSkickatTidpunkt());
        res.setSigneratAv(arende.getSigneratAvName());
        res.setStatus(arende.getStatus());
        res.setVidarebefordrad(getSafeBooleanValue(arende.getVidarebefordrad()));
        return res;
    }

    private static boolean getSafeBooleanValue(Boolean booleanObj) {
        return (booleanObj != null) && booleanObj;
    }
}
