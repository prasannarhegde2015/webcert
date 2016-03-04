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

package se.inera.intyg.webcert.web.integration.registry;

import java.util.Optional;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.inera.intyg.webcert.persistence.integreradenhet.model.IntegreradEnhet;
import se.inera.intyg.webcert.persistence.integreradenhet.model.SchemaVersion;
import se.inera.intyg.webcert.persistence.integreradenhet.repository.IntegreradEnhetRepository;
import se.inera.intyg.webcert.web.integration.registry.dto.IntegreradEnhetEntry;

@Service
public class IntegreradeEnheterRegistryImpl implements IntegreradeEnheterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(IntegreradeEnheterRegistryImpl.class);

    @Autowired
    private IntegreradEnhetRepository integreradEnhetRepository;

    /*
     * (non-Javadoc)
     *
     * @see
     * se.inera.intyg.webcert.web.service.integration.IntegreradeEnheterService#addIfNotExistsIntegreradEnhet(se.inera.intyg.webcert.web
     * .service.integration.dto.IntegreradEnhetEntry)
     */
    @Override
    @Transactional("jpaTransactionManager")
    public void putIntegreradEnhet(IntegreradEnhetEntry entry, SchemaVersion schemaVersion) {

        String enhetsId = entry.getEnhetsId();

        IntegreradEnhet intEnhet = getIntegreradEnhet(enhetsId);
        if (intEnhet != null) {
            LOG.debug("Unit {} is already registered", enhetsId);
            if (schemaVersion.isGreaterThan(intEnhet.getSchemaVersion())) {
                intEnhet.setSchemaVersion(schemaVersion);
                integreradEnhetRepository.save(intEnhet);
                LOG.debug("Unit {} schema version updated to {}", enhetsId, schemaVersion);
            }
            return;
        }

        IntegreradEnhet integreradEnhet = new IntegreradEnhet();
        integreradEnhet.setEnhetsId(enhetsId);
        integreradEnhet.setEnhetsNamn(entry.getEnhetsNamn());
        integreradEnhet.setVardgivarId(entry.getVardgivareId());
        integreradEnhet.setVardgivarNamn(entry.getVardgivareNamn());
        integreradEnhet.setSchemaVersion(schemaVersion);

        IntegreradEnhet savedIntegreradEnhet = integreradEnhetRepository.save(integreradEnhet);

        LOG.debug("Added unit to registry: {}", savedIntegreradEnhet.toString());
    }

    /*
     * (non-Javadoc)
     *
     * @see se.inera.intyg.webcert.web.service.integration.IntegreradeEnheterService#isEnhetIntegrerad(java.lang.String)
     */
    @Override
    @Transactional(value = "jpaTransactionManager", readOnly = true)
    public boolean isEnhetIntegrerad(String enhetsHsaId) {
        IntegreradEnhet ie = getIntegreradEnhet(enhetsHsaId);
        return (ie != null);
    }

    @Override
    @Transactional("jpaTransactionManager")
    public void addIfSameVardgivareButDifferentUnits(String orgEnhetsHsaId, IntegreradEnhetEntry newEntry) {

        IntegreradEnhet enhet = getIntegreradEnhet(orgEnhetsHsaId);
        IntegreradEnhetEntry orgEntry = getIntegreradEnhetEntry(enhet);

        if ((orgEntry != null) && (orgEntry.compareTo(newEntry) != 0)) {
            putIntegreradEnhet(newEntry, enhet.getSchemaVersion());
        }
    }

    @Override
    @Transactional("jpaTransactionManager")
    public void deleteIntegreradEnhet(String enhetsHsaId) {
        IntegreradEnhet enhet = integreradEnhetRepository.findOne(enhetsHsaId);
        if (enhet != null) {
            integreradEnhetRepository.delete(enhet);
            LOG.debug("IntegreradEnhet {} deleted", enhetsHsaId);
        }
    }

    @Override
    @Transactional(value = "jpaTransactionManager", readOnly = true)
    public Optional<SchemaVersion> getSchemaVersion(String enhetsHsaId) {
        IntegreradEnhet ie = getIntegreradEnhet(enhetsHsaId);
        if (ie == null) {
            return Optional.empty();
        }
        return Optional.of(ie.getSchemaVersion());
    }

    private IntegreradEnhet getIntegreradEnhet(String enhetsHsaId) {
        IntegreradEnhet enhet = integreradEnhetRepository.findOne(enhetsHsaId);

        if (enhet == null) {
            LOG.debug("Unit {} is not in the registry of integrated units", enhetsHsaId);
            return null;
        }

        // update entity with control date;
        enhet.setSenasteKontrollDatum(LocalDateTime.now());
        integreradEnhetRepository.save(enhet);

        return enhet;
    }

    private IntegreradEnhetEntry getIntegreradEnhetEntry(IntegreradEnhet enhet) {
        if (enhet == null) {
            return null;
        }
        return new IntegreradEnhetEntry(enhet.getEnhetsId(), enhet.getEnhetsNamn(), enhet.getVardgivarId(), enhet.getVardgivarNamn());
    }
}
