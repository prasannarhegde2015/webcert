/*
 * Copyright (C) 2017 Inera AB (http://www.inera.se)
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
package se.inera.intyg.webcert.web.service.notification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import se.inera.intyg.common.support.common.enumerations.HandelsekodEnum;
import se.inera.intyg.common.support.modules.support.api.notification.ArendeCount;
import se.inera.intyg.common.support.modules.support.api.notification.FragorOchSvar;
import se.inera.intyg.common.support.modules.support.api.notification.NotificationMessage;
import se.inera.intyg.common.support.modules.support.api.notification.SchemaVersion;
import se.inera.intyg.webcert.persistence.utkast.model.Utkast;

@Component
public class NotificationMessageFactoryImpl implements NotificationMessageFactory {

    private static final List<HandelsekodEnum> USES_FRAGOR_OCH_SVAR = Arrays.asList(HandelsekodEnum.NYFRFM,
            HandelsekodEnum.NYSVFM, HandelsekodEnum.NYFRFV, HandelsekodEnum.HANFRFM,
            HandelsekodEnum.HANFRFV, HandelsekodEnum.MAKULE);

    @Autowired
    private FragorOchSvarCreator fragorOchSvarCreator;

    /*
     * (non-Javadoc)
     *
     * @see
     * se.inera.intyg.webcert.web.service.notification.NotificationMessageFactory#createNotificationMessage(se.inera.
     * intyg.webcert.web.
     * persistence.utkast.model.Utkast, se.inera.intyg.common.support.common.enumerations.HandelsekodEnum)
     */
    @Override
    public NotificationMessage createNotificationMessage(Utkast utkast, HandelsekodEnum handelse, SchemaVersion version, String reference) {

        String intygsId = utkast.getIntygsId();
        String intygsTyp = utkast.getIntygsTyp();

        LocalDateTime handelseTid = LocalDateTime.now();
        String logiskAdress = utkast.getEnhetsId();

        String utkastJson = utkast.getModel();

        if (SchemaVersion.VERSION_3 == version) {
            Pair<ArendeCount, ArendeCount> arenden = Pair.of(ArendeCount.getEmpty(), ArendeCount.getEmpty());

            // Add a count of questions to the message
            if (USES_FRAGOR_OCH_SVAR.contains(handelse)) {
                arenden = fragorOchSvarCreator.createArenden(intygsId, intygsTyp);
            }

            return new NotificationMessage(intygsId, intygsTyp, handelseTid, handelse, logiskAdress, utkastJson, null, arenden.getLeft(),
                    arenden.getRight(), version, reference);
        } else {
            FragorOchSvar fragaSvar = FragorOchSvar.getEmpty();

            // Add a count of questions to the message
            if (USES_FRAGOR_OCH_SVAR.contains(handelse)) {
                fragaSvar = fragorOchSvarCreator.createFragorOchSvar(intygsId);
            }

            return new NotificationMessage(intygsId, intygsTyp, handelseTid, handelse, logiskAdress, utkastJson, fragaSvar, null, null,
                    version, reference);
        }
    }

}
