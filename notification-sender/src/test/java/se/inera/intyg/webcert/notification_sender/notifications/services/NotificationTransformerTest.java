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
package se.inera.intyg.webcert.notification_sender.notifications.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import se.inera.intyg.common.fk7263.model.converter.Fk7263InternalToNotification;
import se.inera.intyg.common.fk7263.support.Fk7263EntryPoint;
import se.inera.intyg.common.support.common.enumerations.HandelsekodEnum;
import se.inera.intyg.common.support.modules.registry.IntygModuleRegistry;
import se.inera.intyg.common.support.modules.support.api.ModuleApi;
import se.inera.intyg.common.support.modules.support.api.exception.ModuleException;
import se.inera.intyg.common.support.modules.support.api.notification.ArendeCount;
import se.inera.intyg.common.support.modules.support.api.notification.FragorOchSvar;
import se.inera.intyg.common.support.modules.support.api.notification.NotificationMessage;
import se.inera.intyg.common.support.modules.support.api.notification.SchemaVersion;
import se.inera.intyg.webcert.notification_sender.notifications.routes.NotificationRouteHeaders;
import se.riv.clinicalprocess.healthcond.certificate.certificatestatusupdateforcareresponder.v1.CertificateStatusUpdateForCareType;
import se.riv.clinicalprocess.healthcond.certificate.certificatestatusupdateforcareresponder.v1.UtlatandeType;
import se.riv.clinicalprocess.healthcond.certificate.types.v1.UtlatandeId;
import se.riv.clinicalprocess.healthcond.certificate.types.v3.ArbetsplatsKod;
import se.riv.clinicalprocess.healthcond.certificate.types.v3.IntygId;
import se.riv.clinicalprocess.healthcond.certificate.v3.Enhet;
import se.riv.clinicalprocess.healthcond.certificate.v3.HosPersonal;
import se.riv.clinicalprocess.healthcond.certificate.v3.Intyg;
import se.riv.clinicalprocess.healthcond.certificate.v3.Vardgivare;

@RunWith(MockitoJUnitRunner.class)
public class NotificationTransformerTest {

    private static final String INTYGS_ID = "intyg1";
    private static final String LOGISK_ADRESS = "address1";
    private static final String FK7263 = Fk7263EntryPoint.MODULE_ID;
    private static final String LUSE = "luse";

    @Mock
    private IntygModuleRegistry moduleRegistry;

    @Mock
    private Fk7263InternalToNotification internalToNotification;

    @InjectMocks
    private NotificationTransformer transformer;

    @Test
    public void testSend() throws Exception {
        // Given
        NotificationMessage notificationMessage = new NotificationMessage(INTYGS_ID, FK7263, LocalDateTime.now(), HandelsekodEnum.SKAPAT,
                LOGISK_ADRESS, "{ }", FragorOchSvar.getEmpty(), null, null, SchemaVersion.VERSION_1, "ref");
        Message message = spy(new DefaultMessage());
        message.setBody(notificationMessage);

        setupInternalToNotification();

        // When
        transformer.process(message);

        // Then
        assertEquals(INTYGS_ID, ((CertificateStatusUpdateForCareType) message.getBody()).getUtlatande().getUtlatandeId().getExtension());
        assertEquals(HandelsekodEnum.SKAPAT.value(), message.getHeader(NotificationRouteHeaders.HANDELSE));
        assertEquals(INTYGS_ID, message.getHeader(NotificationRouteHeaders.INTYGS_ID));
        assertEquals(LOGISK_ADRESS, message.getHeader(NotificationRouteHeaders.LOGISK_ADRESS));
        assertEquals(SchemaVersion.VERSION_1.name(), message.getHeader(NotificationRouteHeaders.VERSION));

        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.LOGISK_ADRESS), eq(LOGISK_ADRESS));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.INTYGS_ID), eq(INTYGS_ID));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.HANDELSE), eq(HandelsekodEnum.SKAPAT.value()));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.VERSION), eq(SchemaVersion.VERSION_1.name()));
        verify(internalToNotification, times(1)).createCertificateStatusUpdateForCareType(any());
    }

    @Test
    public void testSendBackwardsCompatibility() throws Exception {
        // Given
        NotificationMessage notificationMessage = new NotificationMessage(INTYGS_ID, FK7263, LocalDateTime.now(), HandelsekodEnum.SKAPAT,
                LOGISK_ADRESS, "{ }", FragorOchSvar.getEmpty(), null, null, null, "ref");
        Message message = spy(new DefaultMessage());
        message.setBody(notificationMessage);

        setupInternalToNotification();

        // When
        transformer.process(message);

        // Then
        assertEquals(INTYGS_ID, ((CertificateStatusUpdateForCareType) message.getBody()).getUtlatande().getUtlatandeId().getExtension());
        assertEquals(HandelsekodEnum.SKAPAT.value(), message.getHeader(NotificationRouteHeaders.HANDELSE));
        assertEquals(INTYGS_ID, message.getHeader(NotificationRouteHeaders.INTYGS_ID));
        assertEquals(LOGISK_ADRESS, message.getHeader(NotificationRouteHeaders.LOGISK_ADRESS));
        assertEquals(SchemaVersion.VERSION_1.name(), message.getHeader(NotificationRouteHeaders.VERSION));

        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.LOGISK_ADRESS), eq(LOGISK_ADRESS));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.INTYGS_ID), eq(INTYGS_ID));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.HANDELSE), eq(HandelsekodEnum.SKAPAT.value()));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.VERSION), eq(SchemaVersion.VERSION_1.name()));
        verify(internalToNotification, times(1)).createCertificateStatusUpdateForCareType(any());
    }

    @Test
    public void testSchemaVersion2Transformation() throws Exception {
        NotificationMessage notificationMessage = new NotificationMessage(INTYGS_ID, LUSE, LocalDateTime.now(), HandelsekodEnum.SKAPAT,
                LOGISK_ADRESS, "{ }", null, ArendeCount.getEmpty(), ArendeCount.getEmpty(), SchemaVersion.VERSION_3, "ref");
        Message message = spy(new DefaultMessage());
        message.setBody(notificationMessage);

        ModuleApi moduleApi = mock(ModuleApi.class);
        when(moduleRegistry.getModuleApi(eq(LUSE))).thenReturn(moduleApi);
        Intyg intyg = new Intyg();
        IntygId intygsId = new IntygId();
        intygsId.setExtension(INTYGS_ID);
        intyg.setIntygsId(intygsId);
        HosPersonal hosPersonal = new HosPersonal();
        Enhet enhet = new Enhet();
        enhet.setArbetsplatskod(new ArbetsplatsKod());
        enhet.setVardgivare(new Vardgivare());
        hosPersonal.setEnhet(enhet);
        intyg.setSkapadAv(hosPersonal);

        when(moduleApi.getIntygFromUtlatande(any())).thenReturn(intyg);

        transformer.process(message);

        assertEquals(INTYGS_ID,
                ((se.riv.clinicalprocess.healthcond.certificate.certificatestatusupdateforcareresponder.v3.CertificateStatusUpdateForCareType) message
                        .getBody()).getIntyg().getIntygsId().getExtension());
        assertEquals(HandelsekodEnum.SKAPAT.value(), message.getHeader(NotificationRouteHeaders.HANDELSE));
        assertEquals(INTYGS_ID, message.getHeader(NotificationRouteHeaders.INTYGS_ID));
        assertEquals(LOGISK_ADRESS, message.getHeader(NotificationRouteHeaders.LOGISK_ADRESS));
        assertEquals(SchemaVersion.VERSION_3.name(), message.getHeader(NotificationRouteHeaders.VERSION));

        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.LOGISK_ADRESS), eq(LOGISK_ADRESS));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.INTYGS_ID), eq(INTYGS_ID));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.HANDELSE), eq(HandelsekodEnum.SKAPAT.value()));
        verify(message, times(1)).setHeader(eq(NotificationRouteHeaders.VERSION), eq(SchemaVersion.VERSION_3.name()));
        verify(moduleRegistry, times(1)).getModuleApi(eq(LUSE));
        verify(moduleApi, times(1)).getUtlatandeFromJson(any());
        verify(moduleApi, times(1)).getIntygFromUtlatande(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSituationanpassatCertificateOnSchemaVersion1() throws Exception {
        NotificationMessage notificationMessage = new NotificationMessage(INTYGS_ID, LUSE, LocalDateTime.now(), HandelsekodEnum.SKAPAT,
                LOGISK_ADRESS, "{ }", FragorOchSvar.getEmpty(), null, null, SchemaVersion.VERSION_1, "ref");
        Message message = new DefaultMessage();
        message.setBody(notificationMessage);
        transformer.process(message);
    }

    private void setupInternalToNotification() throws ModuleException {
        when(internalToNotification.createCertificateStatusUpdateForCareType(any())).thenAnswer(invocation -> {
            NotificationMessage msg = (NotificationMessage) invocation.getArguments()[0];
            if (msg == null) {
                return null;
            }
            CertificateStatusUpdateForCareType request = new CertificateStatusUpdateForCareType();
            UtlatandeType utlatande = new UtlatandeType();
            UtlatandeId utlatandeId = new UtlatandeId();
            utlatandeId.setExtension(msg.getIntygsId());
            utlatande.setUtlatandeId(utlatandeId);
            request.setUtlatande(utlatande);
            return request;
        });
    }
}
