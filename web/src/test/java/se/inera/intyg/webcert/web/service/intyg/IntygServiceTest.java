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
package se.inera.intyg.webcert.web.service.intyg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebServiceException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.helpers.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.inera.intyg.common.fk7263.model.internal.Fk7263Utlatande;
import se.inera.intyg.common.support.common.enumerations.HandelsekodEnum;
import se.inera.intyg.common.support.common.enumerations.RelationKod;
import se.inera.intyg.common.support.model.CertificateState;
import se.inera.intyg.common.support.model.Status;
import se.inera.intyg.common.support.modules.registry.IntygModule;
import se.inera.intyg.common.support.modules.registry.IntygModuleRegistry;
import se.inera.intyg.common.support.modules.support.api.ModuleApi;
import se.inera.intyg.common.support.modules.support.api.dto.CertificateMetaData;
import se.inera.intyg.common.support.modules.support.api.dto.CertificateResponse;
import se.inera.intyg.common.support.modules.support.api.notification.ArendeCount;
import se.inera.intyg.common.util.integration.integration.json.CustomObjectMapper;
import se.inera.intyg.infra.integration.pu.model.Person;
import se.inera.intyg.infra.integration.pu.model.PersonSvar;
import se.inera.intyg.infra.integration.pu.services.PUService;
import se.inera.intyg.infra.security.authorities.AuthoritiesHelper;
import se.inera.intyg.schemas.contract.Personnummer;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceException;
import se.inera.intyg.webcert.persistence.handelse.model.Handelse;
import se.inera.intyg.webcert.persistence.utkast.model.Signatur;
import se.inera.intyg.webcert.persistence.utkast.model.Utkast;
import se.inera.intyg.webcert.persistence.utkast.model.UtkastStatus;
import se.inera.intyg.webcert.persistence.utkast.model.VardpersonReferens;
import se.inera.intyg.webcert.persistence.utkast.repository.UtkastRepository;
import se.inera.intyg.webcert.web.converter.IntygDraftsConverter;
import se.inera.intyg.webcert.web.security.WebCertUserOriginType;
import se.inera.intyg.webcert.web.service.arende.ArendeService;
import se.inera.intyg.webcert.web.service.certificatesender.CertificateSenderService;
import se.inera.intyg.webcert.web.service.intyg.converter.IntygModuleFacade;
import se.inera.intyg.webcert.web.service.intyg.converter.IntygModuleFacadeException;
import se.inera.intyg.webcert.web.service.intyg.decorator.UtkastIntygDecorator;
import se.inera.intyg.webcert.web.service.intyg.dto.IntygContentHolder;
import se.inera.intyg.webcert.web.service.intyg.dto.IntygPdf;
import se.inera.intyg.webcert.web.service.intyg.dto.IntygWithNotifications;
import se.inera.intyg.webcert.web.service.log.LogService;
import se.inera.intyg.webcert.web.service.log.dto.LogRequest;
import se.inera.intyg.webcert.web.service.monitoring.MonitoringLogService;
import se.inera.intyg.webcert.web.service.notification.FragorOchSvarCreator;
import se.inera.intyg.webcert.web.service.notification.NotificationService;
import se.inera.intyg.webcert.web.service.relation.RelationService;
import se.inera.intyg.webcert.web.service.user.WebCertUserService;
import se.inera.intyg.webcert.web.service.user.dto.IntegrationParameters;
import se.inera.intyg.webcert.web.service.user.dto.WebCertUser;
import se.inera.intyg.webcert.web.web.controller.api.dto.ListIntygEntry;
import se.riv.clinicalprocess.healthcond.certificate.listcertificatesforcare.v3.ListCertificatesForCareResponderInterface;
import se.riv.clinicalprocess.healthcond.certificate.listcertificatesforcare.v3.ListCertificatesForCareResponseType;
import se.riv.clinicalprocess.healthcond.certificate.listcertificatesforcare.v3.ListCertificatesForCareType;

/**
 * @author andreaskaltenbach
 */
@RunWith(MockitoJUnitRunner.class)
public class IntygServiceTest {
    private static final String HSA_ID = "HSA-123";
    private static final String CREATED_BY_NAME = "Läkare Läkarsson";
    private static final String SENAST_SPARAD_NAME = "Spara Sparasson";
    private static final String CERTIFICATE_ID = "123";
    private static final String CERTIFICATE_TYPE = "fk7263";
    private static final String LOGICAL_ADDRESS = "<logicalAddress>";

    private ListCertificatesForCareResponseType listResponse;
    private VardpersonReferens vardpersonReferens;
    private String json;

    @Mock
    private IntygModuleRegistry moduleRegistry;

    @Mock
    private ModuleApi moduleApi;

    @Mock
    private AuthoritiesHelper authoritiesHelper;

    @Mock
    private ListCertificatesForCareResponderInterface listCertificatesForCareResponder;

    @Mock
    private IntygModuleFacade moduleFacade;

    @Mock
    private UtkastRepository intygRepository;

    @Mock
    private LogService logservice;

    @Mock
    private WebCertUser webcertUser;

    @Mock
    private WebCertUserService webCertUserService;

    @Mock
    private MonitoringLogService mockMonitoringService;

    @Mock
    private RelationService relationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CertificateSenderService certificateSenderService;

    @Mock
    private ArendeService arendeService;

    @Mock
    private FragorOchSvarCreator fragorOchSvarCreator;

    @Mock
    private PUService puService;

    @Mock
    private UtkastIntygDecorator utkastIntygDecorator;

    @Spy
    private ObjectMapper objectMapper = new CustomObjectMapper();

    @InjectMocks
    private IntygDraftsConverter intygConverter = new IntygDraftsConverter();

    @InjectMocks
    private IntygServiceImpl intygService;

    @Before
    public void setupIntygstjanstResponse() throws Exception {
        vardpersonReferens = new VardpersonReferens();
        vardpersonReferens.setHsaId(HSA_ID);
        vardpersonReferens.setNamn(CREATED_BY_NAME);

        json = FileUtils.getStringFromFile(new ClassPathResource("IntygServiceTest/utlatande.json").getFile());
        Fk7263Utlatande utlatande = objectMapper.readValue(json, Fk7263Utlatande.class);

        CertificateMetaData metaData = new CertificateMetaData();
        metaData.setStatus(new ArrayList<>());

        CertificateResponse certificateResponse = new CertificateResponse(json, utlatande, metaData, false);
        when(moduleFacade.getCertificate(any(String.class), any(String.class))).thenReturn(certificateResponse);
        when(moduleFacade.getUtlatandeFromInternalModel(anyString(), anyString())).thenReturn(utlatande);
    }

    @Before
    public void setupIntygstjanstListResponse() throws Exception {
        ClassPathResource response = new ClassPathResource("IntygServiceTest/response-list-certificates.xml");

        JAXBContext context = JAXBContext.newInstance(ListCertificatesForCareResponseType.class);
        listResponse = context.createUnmarshaller()
                .unmarshal(new StreamSource(response.getInputStream()), ListCertificatesForCareResponseType.class)
                .getValue();
    }

    @Before
    public void setupDefaultAuthorization() {
        Set<String> set = new HashSet<>();
        set.add("fk7263");

        when(webCertUserService.getUser()).thenReturn(webcertUser);
        when(webcertUser.getOrigin()).thenReturn(WebCertUserOriginType.NORMAL.name());
        when(webCertUserService.isAuthorizedForUnit(any(String.class), any(String.class), eq(true))).thenReturn(true);
        when(authoritiesHelper.getIntygstyperForPrivilege(any(WebCertUser.class), anyString())).thenReturn(set);
    }

    @Before
    public void setupLogicalAddress() {
        intygService.setLogicalAddress(LOGICAL_ADDRESS);
    }

    @Before
    public void IntygServiceConverter() throws Exception {
        when(moduleRegistry.getModuleApi(any(String.class))).thenReturn(moduleApi);
        json = FileUtils.getStringFromFile(new ClassPathResource("IntygServiceTest/utlatande.json").getFile());
        Fk7263Utlatande utlatande = objectMapper.readValue(json, Fk7263Utlatande.class);
        when(moduleApi.getUtlatandeFromJson(anyString())).thenReturn(utlatande);

        // use reflection to set IntygDraftsConverter in IntygService
        Field field = IntygServiceImpl.class.getDeclaredField("intygConverter");
        field.setAccessible(true);
        field.set(intygService, intygConverter);

        when(moduleRegistry.getModuleIdFromExternalId(anyString()))
                .thenAnswer(invocation -> ((String) invocation.getArguments()[0]).toLowerCase());
    }

    @Before
    public void setupPUService() {
        when(puService.getPerson(any(Personnummer.class))).thenReturn(getPersonSvar(false));
    }

    @Test
    public void testFetchIntyg() throws Exception {

        IntygContentHolder intygData = intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);

        // ensure that correctcall is made to intygstjanst
        verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(puService).getPerson(any(Personnummer.class));

        verify(mockMonitoringService).logIntygRead(CERTIFICATE_ID, CERTIFICATE_TYPE);

        assertEquals(json, intygData.getContents());
        assertEquals(CERTIFICATE_ID, intygData.getUtlatande().getId());
        assertEquals("19121212-1212", intygData.getUtlatande().getGrundData().getPatient().getPersonId().getPersonnummer());
        assertFalse(intygData.isDeceased());
    }

    @Test(expected = WebCertServiceException.class)
    public void testFetchIntygWithFailingIntygstjanst() throws IntygModuleFacadeException {

        when(moduleFacade.getCertificate(any(String.class), any(String.class))).thenThrow(new IntygModuleFacadeException(""));

        intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
    }

    @Test(expected = WebCertServiceException.class)
    public void testFetchIntygWithFailingAuth() {
        when(webCertUserService.isAuthorizedForUnit(any(String.class), any(String.class), eq(true))).thenReturn(false);

        intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
    }

    @Test
    public void testFetchIntygData() throws Exception {

        intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);

        verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verifyNoMoreInteractions(intygRepository);
        verify(logservice).logReadIntyg(any(LogRequest.class));
        verify(mockMonitoringService).logIntygRead(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verifyNoMoreInteractions(relationService);
    }

    @Test
    public void testFetchIntygDataWithRelation() throws Exception {
        when(relationService.getRelations(eq(CERTIFICATE_ID))).thenReturn(Optional.of(new ArrayList<>()));

        IntygContentHolder res = intygService.fetchIntygDataWithRelations(CERTIFICATE_ID, CERTIFICATE_TYPE, false);

        assertNotNull(res);
        assertNotNull(res.getRelations());

        verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(logservice).logReadIntyg(any(LogRequest.class));
        verify(mockMonitoringService).logIntygRead(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(relationService).getRelations(eq(CERTIFICATE_ID));
    }

    @Test
    public void testFetchIntygDataWithRelationNotFoundInIT() throws Exception {
        when(moduleFacade.getCertificate(any(String.class), any(String.class))).thenThrow(new IntygModuleFacadeException(""));
        when(intygRepository.findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE))
                .thenReturn(getIntyg(CERTIFICATE_ID, null, null));
        when(relationService.getRelations(eq(CERTIFICATE_ID))).thenReturn(Optional.of(new ArrayList<>()));

        IntygContentHolder res = intygService.fetchIntygDataWithRelations(CERTIFICATE_ID, CERTIFICATE_TYPE, false);

        assertNotNull(res);
        assertNotNull(res.getRelations());

        verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(intygRepository).findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(logservice).logReadIntyg(any(LogRequest.class));
        verify(mockMonitoringService).logIntygRead(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(relationService).getRelations(eq(CERTIFICATE_ID));
    }

    @Test
    public void testFetchIntygDataWithRelationITUnavailable() throws Exception {
        when(moduleFacade.getCertificate(any(String.class), any(String.class))).thenThrow(new WebServiceException(""));
        when(intygRepository.findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE))
                .thenReturn(getIntyg(CERTIFICATE_ID, null, null));
        when(relationService.getRelations(eq(CERTIFICATE_ID))).thenReturn(Optional.of(new ArrayList<>()));

        IntygContentHolder res = intygService.fetchIntygDataWithRelations(CERTIFICATE_ID, CERTIFICATE_TYPE, false);

        assertNotNull(res);
        assertNotNull(res.getRelations());

        verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(intygRepository).findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(logservice).logReadIntyg(any(LogRequest.class));
        verify(mockMonitoringService).logIntygRead(CERTIFICATE_ID, CERTIFICATE_TYPE);
        verify(relationService).getRelations(eq(CERTIFICATE_ID));
    }

    @Test
    public void testListIntyg() {
        final String enhetsId = "enhet-1";

        // setup intygstjansten WS mock to return intyg information
        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList(enhetsId),
                new Personnummer("19121212-1212"));

        ArgumentCaptor<ListCertificatesForCareType> argument = ArgumentCaptor.forClass(ListCertificatesForCareType.class);

        verify(listCertificatesForCareResponder).listCertificatesForCare(eq(LOGICAL_ADDRESS), argument.capture());

        assertEquals(2, intygItemListResponse.getLeft().size());

        ListIntygEntry meta = intygItemListResponse.getLeft().get(0);

        assertEquals("1", meta.getIntygId());
        assertEquals("fk7263", meta.getIntygType());
        assertEquals(CertificateState.SENT.name(), meta.getStatus());
        assertTrue(new Personnummer("191212121212").equals(meta.getPatientId()));
        assertEquals(1, argument.getValue().getEnhetsId().size());
        assertNotNull(argument.getValue().getEnhetsId().get(0).getRoot());
        assertEquals(enhetsId, argument.getValue().getEnhetsId().get(0).getExtension());
        assertNotNull(argument.getValue().getPersonId().getRoot());
        assertEquals("191212121212", argument.getValue().getPersonId().getExtension());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListIntygWithIntygstjanstUnavailable() throws IOException {

        // setup intygstjansten WS mock to throw WebServiceException
        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenThrow(
                        WebServiceException.class);
        when(intygRepository.findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet())).thenReturn(
                buildDraftList(false, null, null));

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));
        assertNotNull(intygItemListResponse);
        assertEquals(1, intygItemListResponse.getLeft().size());

        // Assert pdl log not performed, e.g. listing is not a PDL loggable op.
        verifyZeroInteractions(logservice);
    }

    @Test
    public void testListIntygFiltersList() {
        // no intygstyper for user
        when(authoritiesHelper.getIntygstyperForPrivilege(any(WebCertUser.class), anyString())).thenReturn(new HashSet<>());
        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));

        assertTrue(intygItemListResponse.getLeft().isEmpty());
    }

    @Test
    public void testListIntygFiltersNoMatch() {
        Set<String> set = new HashSet<>();
        set.add("luse");

        when(authoritiesHelper.getIntygstyperForPrivilege(any(WebCertUser.class), anyString())).thenReturn(set);
        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));

        assertTrue(intygItemListResponse.getLeft().isEmpty());
    }

    @Test
    public void testListIntygFiltersMatch() {
        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));

        assertEquals(2, intygItemListResponse.getLeft().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchIntygDataWhenIntygstjanstIsUnavailable() throws Exception {
        when(moduleFacade.getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE)).thenThrow(WebServiceException.class);
        when(intygRepository.findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE))
                .thenReturn(getIntyg(CERTIFICATE_ID, null, null));
        IntygContentHolder intygContentHolder = intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertEquals(intygContentHolder.getStatuses().size(), 1);
        assertNotNull(intygContentHolder.getUtlatande());

        // ensure that correct call is made to moduleFacade
        verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
        // Assert pdl log
        verify(logservice).logReadIntyg(any(LogRequest.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchIntygDataHasSentStatusWhenIntygstjanstIsUnavailableAndDraftHadSentDate() throws Exception {
        when(moduleFacade.getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE)).thenThrow(WebServiceException.class);
        when(intygRepository.findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE))
                .thenReturn(getIntyg(CERTIFICATE_ID, LocalDateTime.now(), null));
        IntygContentHolder intygContentHolder = intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertEquals(intygContentHolder.getStatuses().size(), 2);
        assertEquals(intygContentHolder.getStatuses().get(0).getType(), CertificateState.SENT);
        assertNotNull(intygContentHolder.getUtlatande());

        // ensure that correct call is made to moduleFacade
        verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
        // Assert pdl log
        verify(logservice).logReadIntyg(any(LogRequest.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchIntygDataHasSentAndRevokedStatusesWhenIntygstjanstIsUnavailableAndDraftHadSentDateAndRevokedDate()
            throws Exception {
        when(moduleFacade.getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE)).thenThrow(WebServiceException.class);
        when(intygRepository.findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE))
                .thenReturn(getIntyg(CERTIFICATE_ID, LocalDateTime.now(), LocalDateTime.now()));
        IntygContentHolder intygContentHolder = intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertEquals(intygContentHolder.getStatuses().size(), 3);
        assertEquals(intygContentHolder.getStatuses().get(0).getType(), CertificateState.SENT);
        assertEquals(intygContentHolder.getStatuses().get(1).getType(), CertificateState.CANCELLED);
        assertEquals(intygContentHolder.getStatuses().get(2).getType(), CertificateState.RECEIVED);
        assertNotNull(intygContentHolder.getUtlatande());

        // ensure that correct call is made to moduleFacade
        verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
        // Assert pdl log
        verify(logservice).logReadIntyg(any(LogRequest.class));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = WebCertServiceException.class)
    public void testFetchIntygDataFailsWhenIntygstjanstIsUnavailableAndUtkastInNotFound() throws Exception {
        when(moduleFacade.getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE)).thenThrow(WebServiceException.class);
        when(intygRepository.findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE)).thenReturn(null);
        try {
            intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        } catch (Exception e) {
            // ensure that correct call is made to moduleFacade
            verify(moduleFacade).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
            verify(intygRepository).findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE);
            // Assert pdl log
            verifyZeroInteractions(logservice);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDraftAddedToListResponseIfUnique() throws Exception {
        when(intygRepository.findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet()))
                .thenReturn(buildDraftList(true, null, null));
        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));

        assertEquals(3, intygItemListResponse.getLeft().size());
        verify(intygRepository).findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDraftNotAddedToListResponseIfNotUnique() throws Exception {
        when(intygRepository.findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet()))
                .thenReturn(buildDraftList(false, null, null));

        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));
        assertEquals("Dr. Who", intygItemListResponse.getLeft().get(0).getUpdatedSignedBy());
        assertEquals(2, intygItemListResponse.getLeft().size());
        verify(intygRepository).findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDraftAddedWithSkapadAvNameIfMatching() throws Exception {
        when(intygRepository.findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet())).thenReturn(
                buildDraftList(true, vardpersonReferens, null));

        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));
        assertEquals(3, intygItemListResponse.getLeft().size());

        // Se till att posten vi lade till från "drafts" har fått namnet från Utkastet, inte signaturen där HsaId står.
        assertEquals(CREATED_BY_NAME, intygItemListResponse.getLeft().get(2).getUpdatedSignedBy());
        verify(intygRepository).findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDraftAddedWithSenastSparadAvNameIfMatching() throws Exception {
        vardpersonReferens.setNamn(SENAST_SPARAD_NAME);
        when(intygRepository.findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet())).thenReturn(
                buildDraftList(true, null, vardpersonReferens));

        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(
                        listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));
        assertEquals(3, intygItemListResponse.getLeft().size());

        // Se till att posten vi lade till från "drafts" har fått namnet från Utkastet, inte signaturen där HsaId står.
        assertEquals(SENAST_SPARAD_NAME, intygItemListResponse.getLeft().get(2).getUpdatedSignedBy());
        verify(intygRepository).findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDraftAddedWithHsaIdIfNoneMatching() throws Exception {
        vardpersonReferens.setNamn(SENAST_SPARAD_NAME);
        when(intygRepository.findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet())).thenReturn(
                buildDraftList(true, null, null));

        when(listCertificatesForCareResponder.listCertificatesForCare(eq(LOGICAL_ADDRESS), any(ListCertificatesForCareType.class)))
                .thenReturn(
                        listResponse);

        Pair<List<ListIntygEntry>, Boolean> intygItemListResponse = intygService.listIntyg(Collections.singletonList("enhet-1"),
                new Personnummer("19121212-1212"));
        assertEquals(3, intygItemListResponse.getLeft().size());

        // Se till att posten vi lade till från "drafts" har fått namnet från Utkastet, inte signaturen där HsaId står.
        assertEquals(HSA_ID, intygItemListResponse.getLeft().get(2).getUpdatedSignedBy());
        verify(intygRepository).findDraftsByPatientAndEnhetAndStatus(anyString(), anyList(), anyList(), anySet());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchIntygAsPdfFromWebCert() throws IOException, IntygModuleFacadeException {
        when(intygRepository.findOne(CERTIFICATE_ID)).thenReturn(getIntyg(CERTIFICATE_ID, LocalDateTime.now(), null));
        when(moduleFacade.convertFromInternalToPdfDocument(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(buildPdfDocument());
        IntygPdf intygPdf = intygService.fetchIntygAsPdf(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertNotNull(intygPdf);

        verify(intygRepository, times(1)).findOne(anyString());
        verify(logservice).logPrintIntygAsPDF(any(LogRequest.class));
        verifyNoMoreInteractions(logservice);
        verify(moduleFacade, times(0)).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchRevokedIntygAsPdfFromWebCert() throws IOException, IntygModuleFacadeException {
        when(intygRepository.findOne(CERTIFICATE_ID)).thenReturn(getIntyg(CERTIFICATE_ID, LocalDateTime.now(), LocalDateTime.now()));
        when(moduleFacade.convertFromInternalToPdfDocument(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(buildPdfDocument());
        IntygPdf intygPdf = intygService.fetchIntygAsPdf(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertNotNull(intygPdf);

        verify(intygRepository, times(1)).findOne(anyString());
        verify(logservice).logPrintRevokedIntygAsPDF(any(LogRequest.class));
        verifyNoMoreInteractions(logservice);
        verify(moduleFacade, times(0)).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchIntygAsPdfFromIntygstjansten() throws IOException, IntygModuleFacadeException {
        when(intygRepository.findOne(CERTIFICATE_ID)).thenReturn(null);
        when(moduleFacade.convertFromInternalToPdfDocument(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(buildPdfDocument());
        IntygPdf intygPdf = intygService.fetchIntygAsPdf(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertNotNull(intygPdf);

        verify(logservice).logPrintIntygAsPDF(any(LogRequest.class));
        verify(intygRepository, times(1)).findOne(anyString());
        verify(moduleFacade, times(1)).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = WebCertServiceException.class)
    public void testFetchIntygAsPdfNoIntygFound() throws IntygModuleFacadeException {
        when(intygRepository.findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE)).thenReturn(null);
        when(moduleFacade.getCertificate(anyString(), anyString())).thenThrow(IntygModuleFacadeException.class);

        try {
            intygService.fetchIntygAsPdf(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        } catch (Exception e) {
            verify(moduleFacade, times(1)).getCertificate(anyString(), anyString());
            verify(intygRepository, times(1)).findOne(CERTIFICATE_ID);
            verify(intygRepository, times(1)).findOneByIntygsIdAndIntygsTyp(CERTIFICATE_ID, CERTIFICATE_TYPE);
            verifyZeroInteractions(logservice);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLoggingFetchIntygAsPdfWithDraft() throws IOException, IntygModuleFacadeException {
        final Utkast draft = getDraft(CERTIFICATE_ID);
        when(intygRepository.findOne(CERTIFICATE_ID)).thenReturn(draft);

        Fk7263Utlatande utlatande = objectMapper.readValue(draft.getModel(), Fk7263Utlatande.class);

        when(moduleFacade.getUtlatandeFromInternalModel(anyString(), anyString())).thenReturn(utlatande);
        when(moduleFacade.convertFromInternalToPdfDocument(anyString(), anyString(), anyList(), anyBoolean()))
                .thenReturn(buildPdfDocument());
        IntygPdf intygPdf = intygService.fetchIntygAsPdf(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertNotNull(intygPdf);

        verify(intygRepository).findOne(anyString());
        verify(logservice).logPrintIntygAsDraft(any(LogRequest.class));
        verifyNoMoreInteractions(logservice);
        verify(moduleFacade, times(0)).getCertificate(CERTIFICATE_ID, CERTIFICATE_TYPE);
    }

    @Test
    public void testHandleSignedCompletion() throws Exception {
        final String intygId = "intygId";
        final String intygTyp = "intygTyp";
        final String relationIntygId = "relationIntygId";
        final String recipient = "recipient";
        final Personnummer personnummer = new Personnummer("19121212-1212");

        Fk7263Utlatande utlatande = objectMapper.readValue(json, Fk7263Utlatande.class);
        utlatande.setId(intygId);
        utlatande.setTyp(intygTyp);
        utlatande.getGrundData().getPatient().setPersonId(personnummer);

        Utkast utkast = new Utkast();
        utkast.setIntygsId(intygId);
        utkast.setIntygsTyp(intygTyp);
        utkast.setRelationKod(RelationKod.KOMPLT);
        utkast.setRelationIntygsId(relationIntygId);
        utkast.setModel(json);

        when(intygRepository.findOne(intygId)).thenReturn(utkast);
        when(moduleFacade.getUtlatandeFromInternalModel(eq(intygTyp), anyString())).thenReturn(utlatande);

        intygService.handleSignedCompletion(utkast, recipient);

        verify(certificateSenderService).sendCertificate(eq(intygId), eq(personnummer), anyString(), eq(recipient));
        verify(mockMonitoringService).logIntygSent(intygId, recipient);
        verify(logservice).logSendIntygToRecipient(any(LogRequest.class));
        verify(arendeService).closeCompletionsAsHandled(relationIntygId, intygTyp);
        verify(notificationService).sendNotificationForIntygSent(intygId);
        ArgumentCaptor<Utkast> utkastCaptor = ArgumentCaptor.forClass(Utkast.class);
        verify(intygRepository).save(utkastCaptor.capture());
        assertNotNull(utkastCaptor.getValue().getSkickadTillMottagareDatum());
        assertEquals(recipient, utkastCaptor.getValue().getSkickadTillMottagare());
    }

    @Test
    public void testGetIssuingVardenhetsIdForUtkast() {
        String issuingVardenhetHsaId = intygService.getIssuingVardenhetHsaId(CERTIFICATE_ID, CERTIFICATE_TYPE);
        assertEquals("VardenhetY", issuingVardenhetHsaId);
    }

    @Test
    public void testIsRevoked() {
        boolean revoked = intygService.isRevoked(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertFalse(revoked);
        verify(mockMonitoringService).logIntygRevokeStatusRead(CERTIFICATE_ID, CERTIFICATE_TYPE);
    }

    @Test
    public void testListCertificatesForCareWithQAOk() throws Exception {
        final String personnummer = "personnummer";
        final List<String> enhetList = Arrays.asList("enhet");
        final String intygType = "intygType";
        final String intygId = "intygId";
        Handelse handelse = new Handelse();
        final LocalDateTime localDateTime = LocalDateTime.of(2017, Month.JANUARY, 1, 1, 1);
        handelse.setTimestamp(localDateTime);
        handelse.setCode(HandelsekodEnum.SKAPAT);
        Fk7263Utlatande utlatande = objectMapper.readValue(json, Fk7263Utlatande.class);
        ArendeCount sent = new ArendeCount(1, 2, 3, 4);
        ArendeCount received = new ArendeCount(5, 6, 7, 8);

        when(moduleRegistry.listAllModules()).thenReturn(Arrays.asList(new IntygModule(intygType, "", "", "", "", "", "")));
        when(intygRepository.findDraftsByPatientAndEnhetAndStatus(eq(personnummer), eq(enhetList), eq(Arrays.asList(UtkastStatus.values())),
                eq(Collections.singleton(intygType)))).thenReturn(Arrays.asList(getDraft(intygId)));
        when(notificationService.getNotifications(eq(intygId))).thenReturn(Arrays.asList(handelse));
        when(moduleRegistry.getModuleApi(any(String.class))).thenReturn(moduleApi);
        when(moduleApi.getUtlatandeFromJson(anyString())).thenReturn(utlatande);
        when(fragorOchSvarCreator.createArenden(eq(intygId), anyString())).thenReturn(Pair.of(sent, received));

        List<IntygWithNotifications> res = intygService.listCertificatesForCareWithQA(new Personnummer(personnummer), enhetList);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals(1, res.get(0).getNotifications().size());
        assertEquals(HandelsekodEnum.SKAPAT, res.get(0).getNotifications().get(0).getCode());
        assertEquals(localDateTime, res.get(0).getNotifications().get(0).getTimestamp());
        assertEquals(1, res.get(0).getSentQuestions().getTotalt());
        assertEquals(2, res.get(0).getSentQuestions().getEjBesvarade());
        assertEquals(3, res.get(0).getSentQuestions().getBesvarade());
        assertEquals(4, res.get(0).getSentQuestions().getHanterade());
        assertEquals(5, res.get(0).getReceivedQuestions().getTotalt());
        assertEquals(6, res.get(0).getReceivedQuestions().getEjBesvarade());
        assertEquals(7, res.get(0).getReceivedQuestions().getBesvarade());
        assertEquals(8, res.get(0).getReceivedQuestions().getHanterade());
    }

    @Test
    public void testListCertificatesForCareWithQANoNotifications() throws Exception {
        final String personnummer = "personnummer";
        final List<String> enhetList = Arrays.asList("enhet");
        final String intygType = "intygType";
        final String intygId = "intygId";

        when(moduleRegistry.listAllModules()).thenReturn(Arrays.asList(new IntygModule(intygType, "", "", "", "", "", "")));
        when(intygRepository.findDraftsByPatientAndEnhetAndStatus(eq(personnummer), eq(enhetList), eq(Arrays.asList(UtkastStatus.values())),
                eq(Collections.singleton(intygType)))).thenReturn(Arrays.asList(getDraft(intygId)));
        when(notificationService.getNotifications(eq(intygId))).thenReturn(Collections.emptyList());

        List<IntygWithNotifications> res = intygService.listCertificatesForCareWithQA(new Personnummer(personnummer), enhetList);

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    public void testDeceasedIsSetForDeadPatientNormal() {
        when(puService.getPerson(any(Personnummer.class))).thenReturn(getPersonSvar(true));
        IntygContentHolder intygData = intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertTrue(intygData.isDeceased());
    }

    @Test
    public void testDeceasedIsNotSetForAlivePatientDjupintegration() {
        when(webcertUser.getOrigin()).thenReturn(WebCertUserOriginType.DJUPINTEGRATION.name());
        when(webcertUser.getParameters()).thenReturn(new IntegrationParameters("", "", "", "", "", "", "", "", "", false, false, false));
        IntygContentHolder intygData = intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertFalse(intygData.isDeceased());
    }

    @Test
    public void testDeceasedIsSetForDeadPatientDjupintegration() {
        when(webcertUser.getOrigin()).thenReturn(WebCertUserOriginType.DJUPINTEGRATION.name());
        when(webcertUser.getParameters()).thenReturn(new IntegrationParameters("", "", "", "", "", "", "", "", "", false, true, false));
        IntygContentHolder intygData = intygService.fetchIntygData(CERTIFICATE_ID, CERTIFICATE_TYPE, false);
        assertTrue(intygData.isDeceased());
    }

    private IntygPdf buildPdfDocument() {
        IntygPdf pdf = new IntygPdf("fake".getBytes(), "fakepdf.pdf");
        return pdf;
    }

    private List<Utkast> buildDraftList(boolean unique, VardpersonReferens skapadAv, VardpersonReferens senastSparadAv) throws IOException {
        List<Utkast> draftList = new ArrayList<>();
        Utkast draft = getIntyg(unique ? "LONG-UNIQUE-ID" : "1", LocalDateTime.now(), null);
        draft.setSkapadAv(skapadAv);
        draft.setSenastSparadAv(senastSparadAv);
        draftList.add(draft);
        return draftList;
    }

    private Utkast getIntyg(String intygsId, LocalDateTime sendDate, LocalDateTime revokeDate) throws IOException {
        Utkast utkast = new Utkast();
        String json = IOUtils.toString(new ClassPathResource(
                "FragaSvarServiceImplTest/utlatande.json").getInputStream(), "UTF-8");
        utkast.setModel(json);
        utkast.setIntygsId(intygsId);
        utkast.setSkickadTillMottagareDatum(sendDate);
        utkast.setAterkalladDatum(revokeDate);
        utkast.setStatus(UtkastStatus.SIGNED);
        Signatur signatur = new Signatur(LocalDateTime.now(), HSA_ID, CERTIFICATE_ID, "", "", "");
        utkast.setSignatur(signatur);

        return utkast;
    }

    private Utkast getDraft(String intygsId) throws IOException {
        Utkast utkast = new Utkast();
        String json = IOUtils.toString(new ClassPathResource(
                "IntygServiceTest/utkast-utlatande.json").getInputStream(), "UTF-8");
        utkast.setModel(json);
        utkast.setIntygsId(intygsId);
        utkast.setStatus(UtkastStatus.DRAFT_INCOMPLETE);

        return utkast;
    }

    private PersonSvar getPersonSvar(boolean deceased) {
        return new PersonSvar(
                new Person(new Personnummer("19121212-1212"), false, deceased, "fornamn", "mellannamn", "efternamn", "postadress",
                        "postnummer", "postort"),
                null);
    }
}
