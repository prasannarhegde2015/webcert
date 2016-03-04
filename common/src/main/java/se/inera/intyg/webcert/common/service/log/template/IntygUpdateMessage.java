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

package se.inera.intyg.webcert.common.service.log.template;

import se.inera.intyg.common.logmessages.ActivityType;
import se.inera.intyg.common.logmessages.PdlLogMessage;

/**
 * @author andreaskaltenbach
 */
public abstract class IntygUpdateMessage {

    public static PdlLogMessage build(String intygId) {
        PdlLogMessage pdlLogMessage = new PdlLogMessage(ActivityType.UPDATE);
        pdlLogMessage.setActivityLevel(intygId);
        return pdlLogMessage;
    }
}