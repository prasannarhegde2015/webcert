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

package se.inera.intyg.webcert.web.web.controller.moduleapi.dto;

import se.inera.intyg.common.support.modules.support.api.dto.ValidationMessageType;

public class SaveDraftValidationMessage {

    private String field;

    private ValidationMessageType type;

    private String message;

    private String dynamicKey;

    public SaveDraftValidationMessage() {

    }

    public SaveDraftValidationMessage(String field, ValidationMessageType type, String message, String dynamicKey) {
        super();
        this.field = field;
        this.type = type;
        this.message = message;
        this.dynamicKey = dynamicKey;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public ValidationMessageType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDynamicKey() {
        return dynamicKey;
    }
}
