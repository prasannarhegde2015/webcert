/*
 * Copyright (C) 2015 Inera AB (http://www.inera.se)
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

package se.inera.intyg.webcert.persistence.utkast.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

@Entity
@Table(name = "SIGNATUR")
public class Signatur {

    /**
     * Id of the certificate being signed.
     */
    @Id
    @Column(name = "INTYG_ID")
    private String intygsId;

    @Column(name = "SIGNERINGS_DATUM")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
    private LocalDateTime signeringsDatum;

    /**
     * HSA id of the person performing the signing.
     */
    @Column(name = "SIGNERAD_AV")
    private String signeradAv;

    /**
     * The certificate data being signed.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "INTYG_DATA")
    private String intygData;

    /**
     * Hash value calculated from the signed data.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "INTYG_HASH")
    private String intygHash;

    /**
     * Data generated by the signing mechanism.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "SIGNATUR_DATA")
    private String signatur;

    public Signatur() {
    }

    public Signatur(LocalDateTime signeringsDatum, String signeradAv, String intygId, String intygData, String intygHash, String signatur) {
        this.signeringsDatum = signeringsDatum;
        this.signeradAv = signeradAv;
        this.intygsId = intygId;
        this.intygData = intygData;
        this.intygHash = intygHash;
        this.signatur = signatur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return getIntygsId().equals(((Signatur) o).getIntygsId());
    }

    @Override
    public int hashCode() {
        return getIntygsId().hashCode();
    }

    public LocalDateTime getSigneringsDatum() {
        return signeringsDatum;
    }

    public String getSigneradAv() {
        return signeradAv;
    }

    public String getIntygsId() {
        return intygsId;
    }

    public String getIntygData() {
        return intygData;
    }

    public String getIntygHash() {
        return intygHash;
    }

    public String getSignatur() {
        return signatur;
    }
}
