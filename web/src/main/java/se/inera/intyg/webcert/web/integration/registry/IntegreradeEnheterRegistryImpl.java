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
package se.inera.intyg.webcert.web.integration.registry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.inera.intyg.common.fk7263.support.Fk7263EntryPoint;
import se.inera.intyg.common.support.modules.support.api.notification.SchemaVersion;
import se.inera.intyg.webcert.persistence.integreradenhet.model.IntegreradEnhet;
import se.inera.intyg.webcert.persistence.integreradenhet.repository.IntegreradEnhetRepository;
import se.inera.intyg.webcert.web.integration.registry.dto.IntegreradEnhetEntry;
import se.inera.intyg.webcert.web.web.controller.testability.dto.IntegreradEnhetEntryWithSchemaVersion;

@Service
public class IntegreradeEnheterRegistryImpl implements IntegreradeEnheterRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(IntegreradeEnheterRegistryImpl.class);
    private final Set<String> oldIntygTypes = Stream.of(Fk7263EntryPoint.MODULE_ID).collect(Collectors.toSet());
    @Autowired
    private IntegreradEnhetRepository integreradEnhetRepository;

    /*
     * (non-Javadoc)
     *
     * @see
     * se.inera.intyg.webcert.web.service.integration.IntegreradeEnheterService#addIfNotExistsIntegreradEnhet(se.inera.
     * intyg.webcert.web
     * .service.integration.dto.IntegreradEnhetEntry)
     */
    @Override
    @Transactional("jpaTransactionManager")
    public void putIntegreradEnhet(IntegreradEnhetEntry entry, boolean schemaVersion1, boolean schemaVersion2) {

        String enhetsId = entry.getEnhetsId();

        IntegreradEnhet intEnhet = integreradEnhetRepository.findOne(enhetsId);
        if (intEnhet != null) {
            LOG.debug("Updating existing integrerad enhet", enhetsId);
            if (schemaVersion1) {
                intEnhet.setSchemaVersion1(schemaVersion1);
            }
            if (schemaVersion2) {
                intEnhet.setSchemaVersion2(schemaVersion2);
            }
        } else {
            intEnhet = new IntegreradEnhet();
            intEnhet.setEnhetsId(enhetsId);
            intEnhet.setEnhetsNamn(entry.getEnhetsNamn());
            intEnhet.setVardgivarId(entry.getVardgivareId());
            intEnhet.setVardgivarNamn(entry.getVardgivareNamn());
            intEnhet.setSchemaVersion1(schemaVersion1);
            intEnhet.setSchemaVersion2(schemaVersion2);
            LOG.debug("Adding unit to registry: {}", intEnhet.toString());
        }
        integreradEnhetRepository.save(intEnhet);
    }

    /*
     * (non-Javadoc)
     *
     * @see se.inera.intyg.webcert.web.service.integration.IntegreradeEnheterService#isEnhetIntegrerad(java.lang.String)
     */
    @Override
    @Transactional(value = "jpaTransactionManager", readOnly = true)
    public boolean isEnhetIntegrerad(String enhetsHsaId, String intygType) {
        Optional<SchemaVersion> schemaVersion = getSchemaVersion(enhetsHsaId, intygType);
        return schemaVersion.isPresent();
    }

    @Override
    @Transactional("jpaTransactionManager")
    public void addIfSameVardgivareButDifferentUnits(String orgEnhetsHsaId, IntegreradEnhetEntry newEntry, String intygType) {
        if (getSchemaVersion(orgEnhetsHsaId, intygType).isPresent()) {
            IntegreradEnhet enhet = getIntegreradEnhet(orgEnhetsHsaId);
            IntegreradEnhetEntry orgEntry = getIntegreradEnhetEntry(enhet);

            if (orgEntry != null && orgEntry.compareTo(newEntry) != 0) {
                putIntegreradEnhet(newEntry, enhet.isSchemaVersion1(), enhet.isSchemaVersion2());
            }
        }
    }

    @Override
    @Transactional("jpaTransactionManager")
    public void deleteIntegreradEnhet(String enhetsHsaId) {
        IntegreradEnhet unit = integreradEnhetRepository.findOne(enhetsHsaId);
        if (unit != null) {
            integreradEnhetRepository.delete(unit);
            LOG.debug("IntegreradEnhet {} deleted", enhetsHsaId);
        }
    }

    @Override
    @Transactional(value = "jpaTransactionManager", readOnly = true)
    public Optional<SchemaVersion> getSchemaVersion(String enhetsHsaId, String intygType) {
        IntegreradEnhet enhet = getIntegreradEnhet(enhetsHsaId);

        if (enhet == null) {
            return Optional.empty();
        }

        /*
         * This is complicated because we have a transition period for fk7263 where we still send notifications for
         * events for old certificates.
         *
         * First if-case handles new (which means rivta version 2 - SIT certificates, but also TS certificates).
         * These certificates should only use VERSION_3.
         *
         * Then we get to the case where fk7263 is handled during a transition period. When this is the case VERSION_1
         * will always be set to true - which means they have previously accepted notifications for certificates written
         * on this unit. If this is not the case we are dealing with Cambio which does not want notifications for these
         * old certificates but still receive mail that a question or answer was received.
         *
         * If VERSION_1 is set we use the latest version available.
         */
        if (!oldIntygTypes.contains(intygType)) {
            return enhet.isSchemaVersion2() ? Optional.of(SchemaVersion.VERSION_3) : Optional.empty();
        } else if (!enhet.isSchemaVersion1()) {
            return Optional.empty();
        } else {
            return enhet.isSchemaVersion2() ? Optional.of(SchemaVersion.VERSION_3) : Optional.of(SchemaVersion.VERSION_1);
        }
    }

    @Override
    @Transactional(value = "jpaTransactionManager", readOnly = true)
    public List<IntegreradEnhetEntryWithSchemaVersion> getIntegreradeVardenheter() {
        List<IntegreradEnhetEntryWithSchemaVersion> hsaIds = new ArrayList<>();
        integreradEnhetRepository.findAll().forEach(ive -> hsaIds.add(new IntegreradEnhetEntryWithSchemaVersion(ive)));
        return hsaIds;
    }

    private IntegreradEnhet getIntegreradEnhet(String enhetsHsaId) {
        IntegreradEnhet enhet = integreradEnhetRepository.findOne(enhetsHsaId);

        if (enhet == null) {
            LOG.debug("Unit {} is not in the registry of integrated units", enhetsHsaId);
            return null;
        }

        // update entity with control date
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
