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
package se.inera.intyg.webcert.web.service.utkast;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.inera.intyg.common.support.common.enumerations.RelationKod;
import se.inera.intyg.common.support.model.common.internal.HoSPersonal;
import se.inera.intyg.common.support.model.common.internal.Patient;
import se.inera.intyg.common.support.model.common.internal.Vardenhet;
import se.inera.intyg.common.support.model.common.internal.Vardgivare;
import se.inera.intyg.common.support.modules.registry.ModuleNotFoundException;
import se.inera.intyg.common.support.modules.support.api.exception.ModuleException;
import se.inera.intyg.infra.integration.pu.model.Person;
import se.inera.intyg.infra.integration.pu.model.PersonSvar;
import se.inera.intyg.infra.integration.pu.services.PUService;
import se.inera.intyg.schemas.contract.Personnummer;
import se.inera.intyg.webcert.common.model.UtkastStatus;
import se.inera.intyg.webcert.common.model.WebcertCertificateRelation;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceException;
import se.inera.intyg.webcert.persistence.utkast.model.Utkast;
import se.inera.intyg.webcert.persistence.utkast.model.VardpersonReferens;
import se.inera.intyg.webcert.persistence.utkast.repository.UtkastRepository;
import se.inera.intyg.webcert.web.integration.registry.IntegreradeEnheterRegistry;
import se.inera.intyg.webcert.web.integration.registry.dto.IntegreradEnhetEntry;
import se.inera.intyg.webcert.web.service.intyg.IntygService;
import se.inera.intyg.webcert.web.service.log.LogService;
import se.inera.intyg.webcert.web.service.log.dto.LogRequest;
import se.inera.intyg.webcert.web.service.monitoring.MonitoringLogService;
import se.inera.intyg.webcert.web.service.notification.NotificationService;
import se.inera.intyg.webcert.web.service.relation.CertificateRelationService;
import se.inera.intyg.webcert.web.service.user.WebCertUserService;
import se.inera.intyg.webcert.web.service.user.dto.IntegrationParameters;
import se.inera.intyg.webcert.web.service.user.dto.WebCertUser;
import se.inera.intyg.webcert.web.service.utkast.dto.CopyUtkastBuilderResponse;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateCompletionCopyRequest;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateCompletionCopyResponse;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateRenewalCopyRequest;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateRenewalCopyResponse;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateReplacementCopyRequest;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateReplacementCopyResponse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static se.inera.intyg.webcert.web.security.WebCertUserOriginType.DJUPINTEGRATION;

@RunWith(MockitoJUnitRunner.class)
public class CopyUtkastServiceImplTest {

    private static final String INTYG_ID = "abc123";
    private static final String INTYG_COPY_ID = "def456";

    private static final String INTYG_JSON = "A bit of text representing json";

    private static final String INTYG_TYPE = "fk7263";

    private static final Personnummer PATIENT_SSN = new Personnummer("19121212-1212");
    private static final Personnummer PATIENT_NEW_SSN = new Personnummer("19121212-1414");
    private static final String PATIENT_FNAME = "Adam";
    private static final String PATIENT_MNAME = "Bertil";
    private static final String PATIENT_LNAME = "Caesarsson";

    private static final String VARDENHET_ID = "SE00001234-5678";
    private static final String VARDENHET_NAME = "Vårdenheten 1";

    private static final String VARDGIVARE_ID = "SE00001234-1234";
    private static final String VARDGIVARE_NAME = "Vårdgivaren 1";

    private static final String HOSPERSON_ID = "SE12345678-0001";
    private static final String HOSPERSON_NAME = "Dr Börje Dengroth";
    private static final String MEDDELANDE_ID = "13";

    @Mock
    private UtkastRepository mockUtkastRepository;

    @Mock
    private IntygService intygService;

    @Mock
    private CertificateRelationService certificateRelationService;

    @Mock
    private PUService mockPUService;

//    @Mock(name = "createCopyUtkastBuilder")
//    private CreateCopyUtkastBuilder mockUtkastBuilder;

    @Mock(name = "copyCompletionUtkastBuilder")
    private CopyCompletionUtkastBuilder copyCompletionUtkastBuilder;

    @Mock(name = "createRenewalUtkastBuilder")
    private CreateRenewalCopyUtkastBuilder createRenewalCopyUtkastBuilder;

    @Mock(name = "createReplacementUtkastBuilder")
    private CopyUtkastBuilder<CreateReplacementCopyRequest> createReplacementUtkastBuilder;

    @Mock
    private NotificationService mockNotificationService;

    @Mock
    private MonitoringLogService mockMonitoringService;

    @Mock
    private LogService logService;

    @Mock
    private WebCertUserService userService;

    @Mock
    private IntegreradeEnheterRegistry mockIntegreradeEnheterRegistry;

    @InjectMocks
    private CopyUtkastService copyService = new CopyUtkastServiceImpl();

    private HoSPersonal hoSPerson;

    private Patient patient;

    @Before
    public void setup() {
        hoSPerson = new HoSPersonal();
        hoSPerson.setPersonId(HOSPERSON_ID);
        hoSPerson.setFullstandigtNamn(HOSPERSON_NAME);

        Vardgivare vardgivare = new Vardgivare();
        vardgivare.setVardgivarid(VARDGIVARE_ID);
        vardgivare.setVardgivarnamn(VARDGIVARE_NAME);

        Vardenhet vardenhet = new Vardenhet();
        vardenhet.setEnhetsid(VARDENHET_ID);
        vardenhet.setEnhetsnamn(VARDENHET_NAME);
        vardenhet.setVardgivare(vardgivare);
        hoSPerson.setVardenhet(vardenhet);

        patient = new Patient();
        patient.setPersonId(PATIENT_SSN);
    }

    @Before
    public void expectCallToPUService() throws Exception {
        PersonSvar personSvar = new PersonSvar(
                new Person(PATIENT_SSN, false, false, "Adam", "Bertilsson", "Cedergren", "Testgatan 12", "12345", "Testberga"),
                PersonSvar.Status.FOUND);
        when(mockPUService.getPerson(PATIENT_SSN)).thenReturn(personSvar);
    }

    @Before
    public void expectSaveOfUtkast() {
        when(mockUtkastRepository.save(any(Utkast.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    @Before
    public void expectIsRevokedCallToIntygService() {
        when(intygService.isRevoked(anyString(), anyString(), anyBoolean())).thenReturn(false);
    }

    @Before
    public void byDefaultReturnNoRelationsFromRelationService() {
        when(certificateRelationService.getNewestRelationOfType(anyString(), any(RelationKod.class), any(List.class)))
                .thenReturn(Optional.empty());
    }

//    @Test
//    public void testCreateCopy() throws Exception {
//
//        final String reference = "ref";
//        WebCertUser user = new WebCertUser();
//        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", false, false, false, true));
//        when(userService.getUser()).thenReturn(user);
//
//        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.FALSE);
//
//        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
//        when(mockUtkastBuilder.populateCopyUtkastFromSignedIntyg(any(CreateNewDraftCopyRequest.class), any(Person.class),
//                any(boolean.class), any(boolean.class), eq(false))).thenReturn(resp);
//
//        when(certificateRelationService.getNewestRelationOfType(INTYG_ID, RelationKod.ERSATT, Arrays.asList(UtkastStatus.SIGNED)))
//                .thenReturn(Optional.empty());
//
//        CreateNewDraftCopyRequest copyReq = buildCopyRequest();
//
//        CreateNewDraftCopyResponse copyResp = copyService.createCopy(copyReq);
//
//        assertNotNull(copyResp);
//        assertEquals(INTYG_COPY_ID, copyResp.getNewDraftIntygId());
//        assertEquals(INTYG_TYPE, copyResp.getNewDraftIntygType());
//
//        verify(mockPUService).getPerson(PATIENT_SSN);
//        verify(mockUtkastBuilder).populateCopyUtkastFromSignedIntyg(any(CreateNewDraftCopyRequest.class), any(Person.class),
//                any(boolean.class), any(boolean.class), eq(false));
//        verify(mockUtkastRepository).save(any(Utkast.class));
//        verify(mockNotificationService).sendNotificationForDraftCreated(any(Utkast.class), eq(reference));
//        verify(userService).getUser();
//        verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);
//
//        // Assert pdl log
//        verify(logService).logCreateIntyg(any(LogRequest.class));
//    }

    @Test(expected = WebCertServiceException.class)
    public void testRenewalCopyFailIfSignedReplacementExists() throws Exception {

        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", false, false, false, true));
        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.FALSE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(createRenewalCopyUtkastBuilder.populateCopyUtkastFromSignedIntyg(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false))).thenReturn(resp);

        WebcertCertificateRelation ersattRelation = new WebcertCertificateRelation(INTYG_ID, RelationKod.ERSATT, LocalDateTime.now(),
                UtkastStatus.SIGNED);
        when(certificateRelationService.getNewestRelationOfType(INTYG_ID, RelationKod.ERSATT, Arrays.asList(UtkastStatus.SIGNED)))
                .thenReturn(Optional.of(ersattRelation));

        CreateRenewalCopyRequest renewalCopyRequest = buildRenewalRequest();

        try {
            CreateRenewalCopyResponse copyResp = copyService.createRenewalCopy(renewalCopyRequest);
            fail("An exception should have been thrown.");
        } catch (Exception e) {
            verifyZeroInteractions(mockUtkastRepository);
            verifyZeroInteractions(mockNotificationService);
            verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);
            // Assert no pdl logging
            verifyZeroInteractions(logService);
            throw e;
        }
    }

    @Test
    public void testCreateReplacementCopy() throws Exception {

        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", false, false, false, true));
        when(userService.getUser()).thenReturn(user);

        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.FALSE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(createReplacementUtkastBuilder.populateCopyUtkastFromSignedIntyg(any(CreateReplacementCopyRequest.class), any(Person.class),
                eq(true), any(boolean.class), eq(true))).thenReturn(resp);

        CreateReplacementCopyRequest copyReq = buildReplacementCopyRequest();

        CreateReplacementCopyResponse copyResp = copyService.createReplacementCopy(copyReq);

        assertNotNull(copyResp);
        assertEquals(INTYG_COPY_ID, copyResp.getNewDraftIntygId());
        assertEquals(INTYG_TYPE, copyResp.getNewDraftIntygType());

        verify(mockPUService).getPerson(PATIENT_SSN);
        verify(createReplacementUtkastBuilder).populateCopyUtkastFromSignedIntyg(any(CreateReplacementCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(true));
        verify(mockUtkastRepository).save(any(Utkast.class));
        verify(mockNotificationService).sendNotificationForDraftCreated(any(Utkast.class), eq(reference));
        verify(userService).getUser();
        verify(logService).logCreateIntyg(any(LogRequest.class));
        verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);
    }

    @Test(expected = WebCertServiceException.class)
    public void testCreateReplacementCopyFailedIfAlreadyReplacedBySignedIntyg() throws Exception {

        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", false, false, false, true));
        when(userService.getUser()).thenReturn(user);

        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.FALSE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(createReplacementUtkastBuilder.populateCopyUtkastFromSignedIntyg(any(CreateReplacementCopyRequest.class), any(Person.class),
                eq(true), any(boolean.class), eq(true))).thenReturn(resp);
        WebcertCertificateRelation ersattRelation = new WebcertCertificateRelation(INTYG_ID, RelationKod.ERSATT, LocalDateTime.now(),
                UtkastStatus.SIGNED);
        when(certificateRelationService.getNewestRelationOfType(INTYG_ID, RelationKod.ERSATT, Arrays.asList(UtkastStatus.values())))
                .thenReturn(Optional.of(ersattRelation));

        CreateReplacementCopyRequest copyReq = buildReplacementCopyRequest();

        CreateReplacementCopyResponse copyResp = copyService.createReplacementCopy(copyReq);

        assertNotNull(copyResp);
        assertEquals(INTYG_COPY_ID, copyResp.getNewDraftIntygId());
        assertEquals(INTYG_TYPE, copyResp.getNewDraftIntygType());

        verify(mockPUService).getPerson(PATIENT_SSN);
        verify(createReplacementUtkastBuilder).populateCopyUtkastFromSignedIntyg(any(CreateReplacementCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(true));
        verify(mockUtkastRepository).save(any(Utkast.class));
        verify(mockNotificationService).sendNotificationForDraftCreated(any(Utkast.class), eq(reference));
        verify(userService).getUser();
        verify(logService).logCreateIntyg(any(LogRequest.class));
        verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);
    }

    @Test
    public void testCreateCompletion() throws Exception {

        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", true, false, false, true));
        when(userService.getUser()).thenReturn(user);

        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.TRUE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(copyCompletionUtkastBuilder.populateCopyUtkastFromOrignalUtkast(any(CreateCompletionCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false))).thenReturn(resp);

        CreateCompletionCopyRequest copyReq = buildCompletionRequest();

        CreateCompletionCopyResponse copyResp = copyService.createCompletion(copyReq);

        assertNotNull(copyResp);
        assertEquals(INTYG_COPY_ID, copyResp.getNewDraftIntygId());
        assertEquals(INTYG_TYPE, copyResp.getNewDraftIntygType());
        assertEquals(INTYG_ID, copyResp.getOriginalIntygId());

        verify(mockPUService).getPerson(PATIENT_SSN);
        verify(copyCompletionUtkastBuilder).populateCopyUtkastFromOrignalUtkast(any(CreateCompletionCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false));
        verify(mockUtkastRepository).save(any(Utkast.class));
        verify(mockNotificationService).sendNotificationForDraftCreated(any(Utkast.class), eq(reference));
        verify(userService).getUser();
        verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);
    }

    @Test
    public void testCreateRenewal() throws Exception {

        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", false, false, false, true));
        when(userService.getUser()).thenReturn(user);

        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.TRUE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(createRenewalCopyUtkastBuilder.populateCopyUtkastFromOrignalUtkast(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false))).thenReturn(resp);

        when(certificateRelationService.getNewestRelationOfType(INTYG_ID, RelationKod.ERSATT, Arrays.asList(UtkastStatus.SIGNED)))
                .thenReturn(Optional.empty());

        CreateRenewalCopyRequest copyReq = buildRenewalRequest();

        CreateRenewalCopyResponse renewalResponse = copyService.createRenewalCopy(copyReq);

        assertNotNull(renewalResponse);
        assertEquals(INTYG_COPY_ID, renewalResponse.getNewDraftIntygId());
        assertEquals(INTYG_TYPE, renewalResponse.getNewDraftIntygType());
        assertEquals(INTYG_ID, renewalResponse.getOriginalIntygId());

        verify(mockPUService).getPerson(PATIENT_SSN);
        verify(createRenewalCopyUtkastBuilder).populateCopyUtkastFromOrignalUtkast(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), eq(false), eq(false));
        verify(mockUtkastRepository).save(any(Utkast.class));
        verify(mockNotificationService).sendNotificationForDraftCreated(any(Utkast.class), eq(reference));
        verify(userService).getUser();
        verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);
    }

    @Test(expected = WebCertServiceException.class)
    public void testCreateRenewalFailsWhenReplacedBySignedIntyg() throws Exception {

        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", false, false, false, true));
        when(userService.getUser()).thenReturn(user);

        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.TRUE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(createRenewalCopyUtkastBuilder.populateCopyUtkastFromOrignalUtkast(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false))).thenReturn(resp);

        WebcertCertificateRelation ersattRelation = new WebcertCertificateRelation(INTYG_ID, RelationKod.ERSATT, LocalDateTime.now(),
                UtkastStatus.SIGNED);
        when(certificateRelationService.getNewestRelationOfType(INTYG_ID, RelationKod.ERSATT, Arrays.asList(UtkastStatus.SIGNED)))
                .thenReturn(Optional.of(ersattRelation));

        CreateRenewalCopyRequest copyReq = buildRenewalRequest();

        try {
            copyService.createRenewalCopy(copyReq);
            fail("An exception should have been thrown.");
        } catch (Exception e) {
            verifyZeroInteractions(mockUtkastRepository);
            verifyZeroInteractions(mockNotificationService);
            verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);
            // Assert no pdl logging
            verifyZeroInteractions(logService);
            throw e;
        }
    }

    @Test
    public void testCreateRenewalWhenIntegratedAndSjfTrue() throws Exception {

        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setOrigin(DJUPINTEGRATION.name());
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", true, false, false, true));
        when(userService.getUser()).thenReturn(user);

        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.TRUE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(createRenewalCopyUtkastBuilder.populateCopyUtkastFromOrignalUtkast(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false))).thenReturn(resp);

        CreateRenewalCopyRequest copyReq = buildRenewalRequest();
        copyReq.setDjupintegrerad(true);

        CreateRenewalCopyResponse renewalResponse = copyService.createRenewalCopy(copyReq);

        assertNotNull(renewalResponse);
        assertEquals(INTYG_COPY_ID, renewalResponse.getNewDraftIntygId());
        assertEquals(INTYG_TYPE, renewalResponse.getNewDraftIntygType());
        assertEquals(INTYG_ID, renewalResponse.getOriginalIntygId());

        verify(createRenewalCopyUtkastBuilder).populateCopyUtkastFromOrignalUtkast(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), eq(true), eq(false));
        verify(mockUtkastRepository).save(any(Utkast.class));
        verify(mockNotificationService).sendNotificationForDraftCreated(any(Utkast.class), eq(reference));
        verify(userService).getUser();
        verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, true);
        verify(mockIntegreradeEnheterRegistry).addIfSameVardgivareButDifferentUnits(any(String.class), any(IntegreradEnhetEntry.class),
                anyString());
    }

    @Test
    public void testCreateRenewalWhenIntegrated() throws Exception {

        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", false, false, false, true));
        when(userService.getUser()).thenReturn(user);

        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.FALSE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(createRenewalCopyUtkastBuilder.populateCopyUtkastFromSignedIntyg(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false))).thenReturn(resp);

        CreateRenewalCopyRequest renewRequest = buildRenewalRequest();
        renewRequest.setDjupintegrerad(true);

        CreateRenewalCopyResponse renewalCopyResponse = copyService.createRenewalCopy(renewRequest);

        assertNotNull(renewalCopyResponse);
        assertEquals(INTYG_COPY_ID, renewalCopyResponse.getNewDraftIntygId());
        assertEquals(INTYG_TYPE, renewalCopyResponse.getNewDraftIntygType());

        verifyZeroInteractions(mockPUService);
        verify(createRenewalCopyUtkastBuilder).populateCopyUtkastFromSignedIntyg(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false));
        verify(mockUtkastRepository).save(any(Utkast.class));
        verify(mockIntegreradeEnheterRegistry).addIfSameVardgivareButDifferentUnits(any(String.class), any(IntegreradEnhetEntry.class),
                anyString());
        verify(mockNotificationService).sendNotificationForDraftCreated(any(Utkast.class), eq(reference));
        verify(userService).getUser();
        verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);

        // Assert pdl log
        verify(logService).logCreateIntyg(any(LogRequest.class));

    }

    @Test
    public void testCreateCopyWhenIntegratedAndWithUpdatedSSN() throws Exception {
        final String reference = "ref";
        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(reference, "", "", "", "", "", "", "", "", false, false, false, true));
        when(userService.getUser()).thenReturn(user);

        when(mockUtkastRepository.exists(INTYG_ID)).thenReturn(Boolean.FALSE);

        CopyUtkastBuilderResponse resp = createCopyUtkastBuilderResponse();
        when(createRenewalCopyUtkastBuilder.populateCopyUtkastFromSignedIntyg(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false))).thenReturn(resp);

        CreateRenewalCopyRequest copyReq = buildRenewalRequest();
        copyReq.setNyttPatientPersonnummer(PATIENT_NEW_SSN);
        copyReq.setDjupintegrerad(true);

        CreateRenewalCopyResponse renewalCopyResponse = copyService.createRenewalCopy(copyReq);

        assertNotNull(renewalCopyResponse);
        assertEquals(INTYG_COPY_ID, renewalCopyResponse.getNewDraftIntygId());
        assertEquals(INTYG_TYPE, renewalCopyResponse.getNewDraftIntygType());

        verifyZeroInteractions(mockPUService);
        verify(createRenewalCopyUtkastBuilder).populateCopyUtkastFromSignedIntyg(any(CreateRenewalCopyRequest.class), any(Person.class),
                any(boolean.class), any(boolean.class), eq(false));
        verify(mockUtkastRepository).save(any(Utkast.class));
        verify(mockIntegreradeEnheterRegistry).addIfSameVardgivareButDifferentUnits(any(String.class), any(IntegreradEnhetEntry.class),
                anyString());
        verify(mockNotificationService).sendNotificationForDraftCreated(any(Utkast.class), eq(reference));
        verify(userService).getUser();
        verify(intygService).isRevoked(INTYG_ID, INTYG_TYPE, false);

        // Assert pdl log
        verify(logService).logCreateIntyg(any(LogRequest.class));

    }

    @Test(expected = WebCertServiceException.class)
    public void testRenewThrowsExceptionWhenOriginalCertificateIsRevoked() throws ModuleException, ModuleNotFoundException {
        when(intygService.isRevoked(anyString(), anyString(), anyBoolean())).thenReturn(true);

        CreateRenewalCopyRequest copyReq = buildRenewalRequest();
        copyService.createRenewalCopy(copyReq);
    }

    @Test(expected = WebCertServiceException.class)
    public void testCompletionThrowsExceptionWhenOriginalCertificateIsRevoked() throws ModuleException, ModuleNotFoundException {
        when(intygService.isRevoked(anyString(), anyString(), anyBoolean())).thenReturn(true);

        CreateCompletionCopyRequest completionRequest = buildCompletionRequest();
        copyService.createCompletion(completionRequest);
    }

    @Test(expected = WebCertServiceException.class)
    public void testRenewalThrowsExceptionWhenOriginalCertificateIsRevoked() throws ModuleException, ModuleNotFoundException {
        when(intygService.isRevoked(anyString(), anyString(), anyBoolean())).thenReturn(true);

        CreateRenewalCopyRequest renewalRequest = buildRenewalRequest();
        copyService.createRenewalCopy(renewalRequest);
    }

    private CopyUtkastBuilderResponse createCopyUtkastBuilderResponse() {
        CopyUtkastBuilderResponse resp = new CopyUtkastBuilderResponse();
        resp.setOrginalEnhetsId(VARDENHET_ID);
        resp.setUtkastCopy(createCopyUtkast());
        return resp;
    }

    private Utkast createCopyUtkast() {

        Utkast utkast = new Utkast();
        utkast.setIntygsId(INTYG_COPY_ID);
        utkast.setIntygsTyp(INTYG_TYPE);
        utkast.setPatientPersonnummer(PATIENT_SSN);
        utkast.setPatientFornamn(PATIENT_FNAME);
        utkast.setPatientMellannamn(PATIENT_MNAME);
        utkast.setPatientEfternamn(PATIENT_LNAME);
        utkast.setEnhetsId(VARDENHET_ID);
        utkast.setEnhetsNamn(VARDENHET_NAME);
        utkast.setVardgivarId(VARDGIVARE_ID);
        utkast.setVardgivarNamn(VARDGIVARE_NAME);
        utkast.setModel(INTYG_JSON);

        VardpersonReferens vpRef = new VardpersonReferens();
        vpRef.setHsaId(HOSPERSON_ID);
        vpRef.setNamn(HOSPERSON_NAME);

        utkast.setSenastSparadAv(vpRef);
        utkast.setSkapadAv(vpRef);

        return utkast;
    }

//    private CreateNewDraftCopyRequest buildCopyRequest() {
//        return new CreateNewDraftCopyRequest(INTYG_ID, INTYG_TYPE, patient, hoSPerson, false);
//    }

    private CreateCompletionCopyRequest buildCompletionRequest() {
        return new CreateCompletionCopyRequest(INTYG_ID, INTYG_TYPE, MEDDELANDE_ID, patient, hoSPerson);
    }

    private CreateRenewalCopyRequest buildRenewalRequest() {
        return new CreateRenewalCopyRequest(INTYG_ID, INTYG_TYPE, patient, hoSPerson);
    }

    private CreateReplacementCopyRequest buildReplacementCopyRequest() {
        return new CreateReplacementCopyRequest(INTYG_ID, INTYG_TYPE, patient, hoSPerson, false);

    }
}
