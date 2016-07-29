package se.inera.intyg.webcert.web.service.arende;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.stream.Collectors;

import org.joda.time.DateTimeUtils;
import org.joda.time.LocalDateTime;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import se.inera.intyg.common.integration.hsa.model.Vardenhet;
import se.inera.intyg.common.integration.hsa.model.Vardgivare;
import se.inera.intyg.common.integration.hsa.services.HsaEmployeeService;
import se.inera.intyg.common.security.authorities.AuthoritiesHelper;
import se.inera.intyg.common.security.authorities.AuthoritiesResolverUtil;
import se.inera.intyg.common.security.common.model.*;
import se.inera.intyg.common.support.modules.support.api.dto.Personnummer;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceErrorCodeEnum;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceException;
import se.inera.intyg.webcert.persistence.arende.model.*;
import se.inera.intyg.webcert.persistence.arende.repository.ArendeRepository;
import se.inera.intyg.webcert.persistence.model.Filter;
import se.inera.intyg.webcert.persistence.model.Status;
import se.inera.intyg.webcert.persistence.utkast.model.*;
import se.inera.intyg.webcert.persistence.utkast.repository.UtkastRepository;
import se.inera.intyg.webcert.web.auth.bootstrap.AuthoritiesConfigurationTestSetup;
import se.inera.intyg.webcert.web.converter.util.ArendeViewConverter;
import se.inera.intyg.webcert.web.service.certificatesender.CertificateSenderException;
import se.inera.intyg.webcert.web.service.certificatesender.CertificateSenderService;
import se.inera.intyg.webcert.web.service.dto.Lakare;
import se.inera.intyg.webcert.web.service.fragasvar.FragaSvarService;
import se.inera.intyg.webcert.web.service.fragasvar.dto.*;
import se.inera.intyg.webcert.web.service.monitoring.MonitoringLogService;
import se.inera.intyg.webcert.web.service.notification.NotificationService;
import se.inera.intyg.webcert.web.service.user.WebCertUserService;
import se.inera.intyg.webcert.web.service.user.dto.WebCertUser;
import se.inera.intyg.webcert.web.web.controller.api.dto.*;
import se.inera.intyg.webcert.web.web.controller.util.CertificateTypes;

@RunWith(MockitoJUnitRunner.class)
public class ArendeServiceTest extends AuthoritiesConfigurationTestSetup {

    private static final long FIXED_TIME_MILLIS = 1456329300599L;
    private static final LocalDateTime JANUARY = new LocalDateTime("2013-01-12T11:22:11");
    private static final LocalDateTime FEBRUARY = new LocalDateTime("2013-02-12T11:22:11");
    private static final LocalDateTime DECEMBER_YEAR_9999 = new LocalDateTime("9999-12-11T10:22:00");
    private static final Personnummer PATIENT_ID = new Personnummer("19121212-1212");
    private static final String INTYG_ID = "intyg-1";
    private static final String ENHET_ID = "enhet";
    private static final String MEDDELANDE_ID = "meddelandeId";

    @Mock
    private ArendeRepository repo;

    @Mock
    private UtkastRepository utkastRepository;

    @Mock
    private WebCertUserService webcertUserService;

    @Mock
    private AuthoritiesHelper authoritiesHelper;

    @Mock
    private MonitoringLogService monitoringLog;

    @Spy
    private ArendeViewConverter arendeViewConverter;

    @Mock
    private HsaEmployeeService hsaEmployeeService;

    @Mock
    private FragaSvarService fragaSvarService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CertificateSenderService certificateSenderService;

    @InjectMocks
    private ArendeServiceImpl service;

    @Before
    public void setUp() {
        DateTimeUtils.setCurrentMillisFixed(FIXED_TIME_MILLIS);

        // always return the Arende that is saved
        when(repo.save(any(Arende.class))).thenAnswer(new Answer<Arende>() {
            @Override
            public Arende answer(InvocationOnMock invocation) throws Throwable {
                return (Arende) invocation.getArguments()[0];
            }

        });
    }

    @After
    public void cleanUp() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testProcessIncomingMessage() throws WebCertServiceException {
        final String signeratAvName = "signeratAvName";

        Arende arende = new Arende();
        arende.setIntygsId(INTYG_ID);

        Utkast utkast = buildUtkast();
        utkast.getSkapadAv().setNamn(signeratAvName);
        when(utkastRepository.findOne(INTYG_ID)).thenReturn(utkast);

        Arende res = service.processIncomingMessage(arende);

        assertNotNull(res);
        assertEquals(FIXED_TIME_MILLIS, res.getTimestamp().toDateTime().getMillis());
        assertEquals(FIXED_TIME_MILLIS, res.getSenasteHandelse().toDateTime().getMillis());
        assertEquals(utkast.getSignatur().getSigneradAv(), res.getSigneratAv());
        assertEquals(signeratAvName, res.getSigneratAvName());

        verify(utkastRepository).findOne(INTYG_ID);
    }

    @Test
    public void testProcessIncomingMessageSendsNotificationForQuestionReceived() throws WebCertServiceException {
        Arende arende = new Arende();
        arende.setIntygsId(INTYG_ID);

        Utkast utkast = buildUtkast();
        when(utkastRepository.findOne(INTYG_ID)).thenReturn(utkast);

        Arende res = service.processIncomingMessage(arende);

        assertNotNull(res);
        assertEquals(INTYG_ID, res.getIntygsId());

        verify(utkastRepository).findOne(INTYG_ID);
        verify(notificationService).sendNotificationForQuestionReceived(any(Arende.class));
    }

    @Test
    public void testProcessIncomingMessageSendsNotificationForAnswerRecieved() throws WebCertServiceException {
        final String frageid = "frageid";

        Arende fragearende = new Arende();

        Arende svararende = new Arende();
        svararende.setIntygsId(INTYG_ID);
        svararende.setSvarPaId(frageid);

        Utkast utkast = buildUtkast();
        when(utkastRepository.findOne(INTYG_ID)).thenReturn(utkast);
        when(repo.findOneByMeddelandeId(eq(frageid))).thenReturn(fragearende);

        Arende res = service.processIncomingMessage(svararende);

        assertNotNull(res);
        assertEquals(INTYG_ID, res.getIntygsId());

        verify(repo).findOneByMeddelandeId(eq(frageid));
        verify(repo, times(2)).save(any(Arende.class));
        verify(notificationService).sendNotificationForAnswerRecieved(any(Arende.class));
    }

    @Test
    public void testProcessIncomingMessageSendsNotificationForQuestionRecievedIfPaminnelse() throws WebCertServiceException {
        final String paminnelseMeddelandeId = "paminnelseMeddelandeId";

        Arende arende = new Arende();
        arende.setIntygsId(INTYG_ID);
        arende.setPaminnelseMeddelandeId(paminnelseMeddelandeId);

        Utkast utkast = buildUtkast();
        when(utkastRepository.findOne(INTYG_ID)).thenReturn(utkast);

        Arende res = service.processIncomingMessage(arende);

        assertNotNull(res);
        assertEquals(INTYG_ID, res.getIntygsId());

        verify(utkastRepository).findOne(INTYG_ID);
        verify(notificationService).sendNotificationForQuestionReceived(any(Arende.class));
    }

    @Test
    public void testProcessIncomingMessageUpdatingRelatedSvar() throws WebCertServiceException {
        final String frageid = "frageid";

        Arende fragearende = new Arende();

        Arende svararende = new Arende();
        svararende.setIntygsId(INTYG_ID);
        svararende.setSvarPaId(frageid);

        Utkast utkast = buildUtkast();
        utkast.setIntygsTyp("intygstyp");
        utkast.setEnhetsId(ENHET_ID);

        when(utkastRepository.findOne(eq(INTYG_ID))).thenReturn(utkast);
        when(repo.findOneByMeddelandeId(eq(frageid))).thenReturn(fragearende);

        Arende res = service.processIncomingMessage(svararende);
        assertEquals(Status.ANSWERED, res.getStatus());
        assertEquals(FIXED_TIME_MILLIS, res.getSenasteHandelse().toDateTime().getMillis());

        verify(repo).findOneByMeddelandeId(eq(frageid));
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo, times(2)).save(arendeCaptor.capture());

        Arende updatedQuestion = arendeCaptor.getAllValues().get(1);
        assertEquals(FIXED_TIME_MILLIS, updatedQuestion.getSenasteHandelse().toDateTime().getMillis());
        assertEquals(Status.ANSWERED, updatedQuestion.getStatus());
    }

    @Test
    public void testProcessIncomingMessageUpdatingRelatedPaminnelse() throws WebCertServiceException {
        final String paminnelseid = "paminnelseid";

        Arende paminnelse = new Arende();

        Arende svararende = new Arende();
        svararende.setIntygsId(INTYG_ID);
        svararende.setPaminnelseMeddelandeId(paminnelseid);

        Utkast utkast = buildUtkast();
        utkast.setIntygsTyp("intygstyp");
        utkast.setEnhetsId(ENHET_ID);

        when(utkastRepository.findOne(eq(INTYG_ID))).thenReturn(utkast);
        when(repo.findOneByMeddelandeId(eq(paminnelseid))).thenReturn(paminnelse);

        Arende res = service.processIncomingMessage(svararende);
        assertEquals(FIXED_TIME_MILLIS, res.getSenasteHandelse().toDateTime().getMillis());

        verify(repo).findOneByMeddelandeId(eq(paminnelseid));
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo, times(2)).save(arendeCaptor.capture());

        Arende updatedQuestion = arendeCaptor.getAllValues().get(1);
        assertEquals(FIXED_TIME_MILLIS, updatedQuestion.getSenasteHandelse().toDateTime().getMillis());
    }

    @Test
    public void testProcessIncomingMessageCertificateNotFound() {
        when(utkastRepository.findOne(anyString())).thenReturn(null);
        try {
            service.processIncomingMessage(new Arende());
            fail("Should throw");
        } catch (WebCertServiceException e) {
            assertEquals(WebCertServiceErrorCodeEnum.DATA_NOT_FOUND, e.getErrorCode());
        }
    }

    @Test
    public void testProcessIncomingMessageCertificateNotSigned() {
        when(utkastRepository.findOne(anyString())).thenReturn(new Utkast());
        try {
            service.processIncomingMessage(new Arende());
            fail("Should throw");
        } catch (WebCertServiceException e) {
            assertEquals(WebCertServiceErrorCodeEnum.INVALID_STATE, e.getErrorCode());
        }
    }

    @Test
    public void testProcessIncomingMessageMEDDELANDE_IDNotUnique() {
        when(repo.findOneByMeddelandeId(anyString())).thenReturn(new Arende());
        try {
            service.processIncomingMessage(new Arende());
            fail("Should throw");
        } catch (WebCertServiceException e) {
            assertEquals(WebCertServiceErrorCodeEnum.INVALID_STATE, e.getErrorCode());
        }
    }

    public void createQuestionTest() throws CertificateSenderException {
        LocalDateTime now = LocalDateTime.now();
        when(utkastRepository.findOne(anyString())).thenReturn(buildUtkast());
        when(webcertUserService.isAuthorizedForUnit(anyString(), anyBoolean())).thenReturn(true);
        when(webcertUserService.getUser()).thenReturn(new WebCertUser());
        Arende arende = new Arende();
        arende.setSenasteHandelse(now);
        when(arendeViewConverter.convert(any(Arende.class))).thenReturn(mock(ArendeView.class));
        ArendeConversationView result = service.createMessage("INTYG_ID", ArendeAmne.KONTKT, "rubrik", "meddelande");
        assertNotNull(result.getFraga());
        assertNull(result.getSvar());
        assertEquals(now, result.getSenasteHandelse());
        verify(webcertUserService).isAuthorizedForUnit(anyString(), anyBoolean());
        verify(repo).save(any(Arende.class));
        verify(monitoringLog).logArendeCreated(anyString(), anyString(), anyString(), anyString());
        verify(certificateSenderService).sendMessageToRecipient(anyString(), anyString());
        verify(notificationService).sendNotificationForQuestionSent(any(Arende.class));
    }

    @Test
    public void createQuestionInvalidAmneTest() {
        try {
            service.createMessage("INTYG_ID", ArendeAmne.KOMPLT, "rubrik", "meddelande");
            fail("should throw exception");
        } catch (WebCertServiceException e) {
            assertEquals(WebCertServiceErrorCodeEnum.INTERNAL_PROBLEM, e.getErrorCode());
        }
    }

    @Test
    public void createQuestionCertificateDoesNotExistTest() {
        when(utkastRepository.findOne(anyString())).thenReturn(null);
        try {
            service.createMessage("INTYG_ID", ArendeAmne.KONTKT, "rubrik", "meddelande");
            fail("should throw exception");
        } catch (WebCertServiceException e) {
            assertEquals(WebCertServiceErrorCodeEnum.DATA_NOT_FOUND, e.getErrorCode());
        }
    }

    @Test
    public void createQuestionCertificateNotSignedTest() {
        when(utkastRepository.findOne(anyString())).thenReturn(new Utkast());
        try {
            service.createMessage("INTYG_ID", ArendeAmne.KONTKT, "rubrik", "meddelande");
            fail("should throw exception");
        } catch (WebCertServiceException e) {
            assertEquals(WebCertServiceErrorCodeEnum.INVALID_STATE, e.getErrorCode());
        }
    }

    @Test
    public void createQuestionInvalidCertificateTypeTest() {
        Utkast utkast = new Utkast();
        utkast.setSignatur(new Signatur());
        utkast.setIntygsTyp(CertificateTypes.FK7263.toString());
        when(utkastRepository.findOne(anyString())).thenReturn(utkast);
        try {
            service.createMessage("INTYG_ID", ArendeAmne.KONTKT, "rubrik", "meddelande");
            fail("should throw exception");
        } catch (WebCertServiceException e) {
            assertEquals(WebCertServiceErrorCodeEnum.INVALID_STATE, e.getErrorCode());
        }
    }

    @Test
    public void createQuestionUnauthorizedTest() {
        Utkast utkast = new Utkast();
        utkast.setSignatur(new Signatur());
        when(utkastRepository.findOne(anyString())).thenReturn(utkast);
        when(webcertUserService.isAuthorizedForUnit(anyString(), anyBoolean())).thenReturn(false);
        try {
            service.createMessage("INTYG_ID", ArendeAmne.KONTKT, "rubrik", "meddelande");
            fail("should throw exception");
        } catch (WebCertServiceException e) {
            assertEquals(WebCertServiceErrorCodeEnum.AUTHORIZATION_PROBLEM, e.getErrorCode());
        }
    }

    @Test
    public void answerTest() throws CertificateSenderException {
        final String svarPaMeddelandeId = "svarPaMeddelandeId";
        LocalDateTime now = LocalDateTime.now();
        Arende fraga = buildArende(svarPaMeddelandeId, null);
        fraga.setAmne(ArendeAmne.OVRIGT);
        fraga.setSenasteHandelse(now);
        fraga.setStatus(Status.PENDING_INTERNAL_ACTION);
        fraga.setPatientPersonId("191212121212");
        when(repo.findOneByMeddelandeId(svarPaMeddelandeId)).thenReturn(fraga);
        when(webcertUserService.isAuthorizedForUnit(anyString(), anyBoolean())).thenReturn(true);
        when(webcertUserService.getUser()).thenReturn(new WebCertUser());
        ArendeConversationView result = service.answer(svarPaMeddelandeId, "svarstext");
        assertNotNull(result.getFraga());
        assertNotNull(result.getSvar());
        assertEquals(now, result.getSenasteHandelse());
        verify(webcertUserService).isAuthorizedForUnit(anyString(), anyBoolean());
        verify(repo, times(2)).save(any(Arende.class));
        verify(monitoringLog).logArendeCreated(anyString(), anyString(), anyString(), anyString());
        verify(certificateSenderService).sendMessageToRecipient(anyString(), anyString());
        verify(notificationService).sendNotificationForQuestionHandled(any(Arende.class));
    }

    @Test(expected = WebCertServiceException.class)
    public void answerSvarsTextNullTest() throws CertificateSenderException {
        service.answer("svarPaMeddelandeId", null);
    }

    @Test(expected = WebCertServiceException.class)
    public void answerSvarsTextEmptyTest() throws CertificateSenderException {
        service.answer("svarPaMeddelandeId", "");
    }

    @Test(expected = WebCertServiceException.class)
    public void answerQuestionWithInvalidStatusTest() throws CertificateSenderException {
        final String svarPaMeddelandeId = "svarPaMeddelandeId";
        Arende fraga = new Arende();
        fraga.setStatus(Status.PENDING_EXTERNAL_ACTION);
        when(repo.findOneByMeddelandeId(svarPaMeddelandeId)).thenReturn(fraga);
        when(webcertUserService.isAuthorizedForUnit(anyString(), anyBoolean())).thenReturn(true);
        service.answer(svarPaMeddelandeId, "svarstext");
    }

    @Test(expected = WebCertServiceException.class)
    public void answerPaminnQuestionTest() throws CertificateSenderException {
        final String svarPaMeddelandeId = "svarPaMeddelandeId";
        Arende fraga = new Arende();
        fraga.setStatus(Status.PENDING_INTERNAL_ACTION);
        fraga.setAmne(ArendeAmne.PAMINN);
        when(repo.findOneByMeddelandeId(svarPaMeddelandeId)).thenReturn(fraga);
        when(webcertUserService.isAuthorizedForUnit(anyString(), anyBoolean())).thenReturn(true);
        service.answer(svarPaMeddelandeId, "svarstext");
    }

    @Test(expected = WebCertServiceException.class)
    public void answerKompltQuestionUnauthorizedTest() throws CertificateSenderException {
        final String svarPaMeddelandeId = "svarPaMeddelandeId";
        Arende fraga = new Arende();
        fraga.setStatus(Status.PENDING_INTERNAL_ACTION);
        fraga.setAmne(ArendeAmne.KOMPLT);
        when(repo.findOneByMeddelandeId(svarPaMeddelandeId)).thenReturn(fraga);
        when(webcertUserService.isAuthorizedForUnit(anyString(), anyBoolean())).thenReturn(true);
        when(webcertUserService.getUser()).thenReturn(new WebCertUser());
        service.answer(svarPaMeddelandeId, "svarstext");
    }

    @Test
    public void answerKompltQuestionAuthorizedTest() throws CertificateSenderException {
        final String svarPaMeddelandeId = "svarPaMeddelandeId";
        Arende fraga = buildArende(svarPaMeddelandeId, null);
        fraga.setStatus(Status.PENDING_INTERNAL_ACTION);
        fraga.setAmne(ArendeAmne.KOMPLT);
        fraga.setPatientPersonId("191212121212");
        when(repo.findOneByMeddelandeId(svarPaMeddelandeId)).thenReturn(fraga);
        when(webcertUserService.isAuthorizedForUnit(anyString(), anyBoolean())).thenReturn(true);
        WebCertUser webcertUser = new WebCertUser();
        webcertUser.setAuthorities(new HashMap<>());
        Privilege privilege = new Privilege();
        privilege.setRequestOrigins(new ArrayList<>());
        webcertUser.getAuthorities().put(AuthoritiesConstants.PRIVILEGE_BESVARA_KOMPLETTERINGSFRAGA, privilege);
        when(webcertUserService.getUser()).thenReturn(webcertUser);

        ArendeConversationView result = service.answer(svarPaMeddelandeId, "svarstext");

        assertNotNull(result.getFraga());
        assertNotNull(result.getSvar());
    }

    @Test
    public void answerUpdatesQuestionTest() throws CertificateSenderException {
        final String svarPaMeddelandeId = "svarPaMeddelandeId";
        LocalDateTime now = LocalDateTime.now();
        Arende fraga = buildArende(svarPaMeddelandeId, null);
        fraga.setAmne(ArendeAmne.OVRIGT);
        fraga.setMeddelandeId(svarPaMeddelandeId);
        fraga.setStatus(Status.PENDING_INTERNAL_ACTION);
        fraga.setPatientPersonId("191212121212");
        when(repo.findOneByMeddelandeId(svarPaMeddelandeId)).thenReturn(fraga);
        when(webcertUserService.isAuthorizedForUnit(anyString(), anyBoolean())).thenReturn(true);
        when(webcertUserService.getUser()).thenReturn(new WebCertUser());
        ArendeConversationView result = service.answer(svarPaMeddelandeId, "svarstext");
        assertNotNull(result.getFraga());
        assertNotNull(result.getSvar());
        assertEquals(now, result.getSenasteHandelse());
        verify(webcertUserService).isAuthorizedForUnit(anyString(), anyBoolean());
        verify(monitoringLog).logArendeCreated(anyString(), anyString(), anyString(), anyString());
        verify(certificateSenderService).sendMessageToRecipient(anyString(), anyString());
        verify(notificationService).sendNotificationForQuestionHandled(any(Arende.class));
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo, times(2)).save(arendeCaptor.capture());

        Arende updatedQuestion = arendeCaptor.getAllValues().get(1);
        assertEquals(FIXED_TIME_MILLIS, updatedQuestion.getSenasteHandelse().toDateTime().getMillis());
        assertEquals(Status.CLOSED, updatedQuestion.getStatus());
    }

    @Test(expected = WebCertServiceException.class)
    public void setForwardedArendeNotFoundTest() {
        service.setForwarded(MEDDELANDE_ID, true);
    }

    @Test
    public void setForwardedTrueTest() {
        Arende arende = buildArende(MEDDELANDE_ID, null);
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);
        service.setForwarded(MEDDELANDE_ID, true);

        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo).save(arendeCaptor.capture());
        verify(repo).findBySvarPaId(MEDDELANDE_ID); // lookup answers for response
        verify(repo).findByPaminnelseMeddelandeId(MEDDELANDE_ID); // lookup reminders for response

        assertEquals(true, arendeCaptor.getValue().getVidarebefordrad());
    }

    @Test
    public void setForwardedFalseTest() {
        Arende arende = buildArende(MEDDELANDE_ID, null);
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);
        service.setForwarded(MEDDELANDE_ID, false);

        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo).save(arendeCaptor.capture());
        verify(repo).findBySvarPaId(MEDDELANDE_ID); // lookup answers for response
        verify(repo).findByPaminnelseMeddelandeId(MEDDELANDE_ID); // lookup reminders for response

        assertEquals(false, arendeCaptor.getValue().getVidarebefordrad());
    }

    @Test(expected = WebCertServiceException.class)
    public void closeArendeAsHandledArendeNotFoundTest() {
        service.closeArendeAsHandled(MEDDELANDE_ID);
    }

    @Test
    public void closeArendeAsHandledTest() {
        Arende arende = buildArende(MEDDELANDE_ID, null);
        arende.setSkickatAv(FrageStallare.FORSAKRINGSKASSAN.getKod());
        arende.setStatus(Status.PENDING_INTERNAL_ACTION);
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);

        service.closeArendeAsHandled(MEDDELANDE_ID);
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo).save(arendeCaptor.capture());
        assertEquals(Status.CLOSED, arendeCaptor.getValue().getStatus());
        verify(notificationService).sendNotificationForQuestionHandled(any(Arende.class));
    }

    @Test
    public void closeArendeAsHandledFromWCNoAnswerTest() {
        Arende arende = buildArende(MEDDELANDE_ID, null);
        arende.setSkickatAv(FrageStallare.WEBCERT.getKod());
        arende.setStatus(Status.PENDING_EXTERNAL_ACTION);
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);

        service.closeArendeAsHandled(MEDDELANDE_ID);
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo).save(arendeCaptor.capture());
        assertEquals(Status.CLOSED, arendeCaptor.getValue().getStatus());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void closeArendeAsHandledAnswerTest() {
        Arende arende = buildArende(MEDDELANDE_ID, null);
        arende.setSkickatAv(FrageStallare.WEBCERT.getKod());
        arende.setStatus(Status.ANSWERED);
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);
        when(repo.findBySvarPaId(MEDDELANDE_ID)).thenReturn(Arrays.asList(buildArende(UUID.randomUUID().toString(), null))); // there are answers

        service.closeArendeAsHandled(MEDDELANDE_ID);
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo).save(arendeCaptor.capture());
        assertEquals(Status.CLOSED, arendeCaptor.getValue().getStatus());
        verify(notificationService).sendNotificationForAnswerHandled(any(Arende.class));
    }

    @Test(expected = WebCertServiceException.class)
    public void openArendeAsUnhandledArendeNotFoundTest() {
        service.openArendeAsUnhandled(MEDDELANDE_ID);
    }

    @Test(expected = WebCertServiceException.class)
    public void openArendeAsUnhandledFromFKAndAnsweredTest() {
        Arende arende = new Arende();
        arende.setSkickatAv(FrageStallare.FORSAKRINGSKASSAN.getKod());
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);
        when(repo.findBySvarPaId(MEDDELANDE_ID)).thenReturn(Arrays.asList(new Arende())); // there are answers

        service.openArendeAsUnhandled(MEDDELANDE_ID);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void openArendeAsUnhandledQuestionFromFK() {
        Arende arende = buildArende(MEDDELANDE_ID, null);
        arende.setSkickatAv(FrageStallare.FORSAKRINGSKASSAN.getKod());
        arende.setStatus(Status.CLOSED);
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);

        service.openArendeAsUnhandled(MEDDELANDE_ID);
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo).save(arendeCaptor.capture());
        assertEquals(Status.PENDING_INTERNAL_ACTION, arendeCaptor.getValue().getStatus());
        verify(notificationService).sendNotificationForQuestionHandled(any(Arende.class));
    }

    @Test
    public void openArendeAsUnhandledAnswerFromFK() {
        Arende arende = buildArende(MEDDELANDE_ID, null);
        arende.setSkickatAv(FrageStallare.WEBCERT.getKod());
        arende.setStatus(Status.CLOSED);
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);
        when(repo.findBySvarPaId(MEDDELANDE_ID)).thenReturn(Arrays.asList(buildArende(UUID.randomUUID().toString(), null))); // there are answers

        service.openArendeAsUnhandled(MEDDELANDE_ID);
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo).save(arendeCaptor.capture());
        assertEquals(Status.ANSWERED, arendeCaptor.getValue().getStatus());
        verify(notificationService).sendNotificationForAnswerHandled(any(Arende.class));
    }

    @Test
    public void openArendeAsUnhandledQuestionFromWCTest() {
        Arende arende = buildArende(MEDDELANDE_ID, null);
        arende.setSkickatAv(FrageStallare.WEBCERT.getKod());
        arende.setStatus(Status.CLOSED);
        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);

        service.openArendeAsUnhandled(MEDDELANDE_ID);
        ArgumentCaptor<Arende> arendeCaptor = ArgumentCaptor.forClass(Arende.class);
        verify(repo).save(arendeCaptor.capture());
        assertEquals(Status.PENDING_EXTERNAL_ACTION, arendeCaptor.getValue().getStatus());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void testListSignedByForUnits() {
        final List<String> selectedUnits = Arrays.asList("enhet1", "enhet2");
        final String[] lakare1 = { "hsaid1", "namn1" };
        final String[] lakare2 = { "hsaid2", "namn2" };
        final String[] lakare3 = { "hsaid3", "namn3" };
        final String[] lakare4 = { "hsaid4", "namn4" };
        final List<Object[]> repoResult = Arrays.asList(lakare1, lakare2, lakare3);
        final List<Object[]> expected = Arrays.asList(lakare1, lakare2, lakare3, lakare4);

        WebCertUser user = Mockito.mock(WebCertUser.class);
        when(user.getIdsOfSelectedVardenhet()).thenReturn(selectedUnits);
        when(webcertUserService.getUser()).thenReturn(user);
        when(repo.findSigneratAvByEnhet(selectedUnits)).thenReturn(repoResult);
        when(fragaSvarService.getFragaSvarHsaIdByEnhet(eq(null))).thenReturn(Arrays.asList(new Lakare(lakare4[0], lakare4[1])));

        List<Lakare> res = service.listSignedByForUnits(null);

        assertEquals(expected.stream().map(arr -> new Lakare((String) arr[0], (String) arr[1])).collect(Collectors.toList()), res);

        verify(webcertUserService).getUser();
        verify(repo).findSigneratAvByEnhet(selectedUnits);
    }

    @Test
    public void testListSignedByForUnitsSpecifiedUnit() {
        final List<String> selectedUnit = Arrays.asList("enhet1");
        final String[] lakare1 = { "hsaid1", "namn1" };
        final String[] lakare2 = { "hsaid2", "namn2" };
        final String[] lakare3 = { "hsaid3", "namn3" };
        final List<Object[]> repoResult = Arrays.asList(lakare1, lakare2, lakare3);

        when(webcertUserService.isAuthorizedForUnit(anyString(), eq(true))).thenReturn(true);
        when(repo.findSigneratAvByEnhet(selectedUnit)).thenReturn(repoResult);
        when(fragaSvarService.getFragaSvarHsaIdByEnhet(eq(null))).thenReturn(new ArrayList<>());

        List<Lakare> res = service.listSignedByForUnits(selectedUnit.get(0));

        assertEquals(repoResult.stream().map(arr -> new Lakare((String) arr[0], (String) arr[1])).collect(Collectors.toList()), res);

        verify(repo).findSigneratAvByEnhet(selectedUnit);
    }

    @Test
    public void testGetArendeForIntyg() {
        List<Arende> arendeList = new ArrayList<>();

        arendeList.add(buildArende(UUID.randomUUID().toString(), DECEMBER_YEAR_9999, FEBRUARY));
        arendeList.add(buildArende(UUID.randomUUID().toString(), JANUARY, JANUARY));
        arendeList.get(1).setSvarPaId(arendeList.get(0).getMeddelandeId()); // svar
        arendeList.add(buildArende(UUID.randomUUID().toString(), DECEMBER_YEAR_9999, DECEMBER_YEAR_9999));
        arendeList.get(2).setAmne(ArendeAmne.PAMINN);
        arendeList.get(2).setPaminnelseMeddelandeId(arendeList.get(0).getMeddelandeId()); // paminnelse
        arendeList.add(buildArende(UUID.randomUUID().toString(), FEBRUARY, FEBRUARY));
        arendeList.add(buildArende(UUID.randomUUID().toString(), DECEMBER_YEAR_9999, DECEMBER_YEAR_9999));
        arendeList.add(buildArende(UUID.randomUUID().toString(), JANUARY, JANUARY));

        when(repo.findByIntygsId(INTYG_ID)).thenReturn(arendeList);

        when(webcertUserService.getUser()).thenReturn(createUser());

        List<ArendeConversationView> result = service.getArenden(INTYG_ID);

        verify(repo).findByIntygsId(INTYG_ID);
        verify(webcertUserService).getUser();

        assertEquals(4, result.size());
        assertEquals(1, result.get(0).getPaminnelser().size());
        assertEquals(arendeList.get(0).getMeddelandeId(), result.get(0).getFraga().getInternReferens());
        assertEquals(arendeList.get(1).getMeddelandeId(), result.get(0).getSvar().getInternReferens());
        assertEquals(arendeList.get(2).getMeddelandeId(), result.get(0).getPaminnelser().get(0).getInternReferens());
        assertEquals(arendeList.get(3).getMeddelandeId(), result.get(2).getFraga().getInternReferens());
        assertEquals(arendeList.get(4).getMeddelandeId(), result.get(1).getFraga().getInternReferens());
        assertEquals(arendeList.get(5).getMeddelandeId(), result.get(3).getFraga().getInternReferens());
        assertEquals(DECEMBER_YEAR_9999, result.get(0).getSenasteHandelse());
        assertEquals(DECEMBER_YEAR_9999, result.get(1).getSenasteHandelse());
        assertEquals(FEBRUARY, result.get(2).getSenasteHandelse());
        assertEquals(JANUARY, result.get(3).getSenasteHandelse());
    }

    @Test
    public void testGetArendenFiltersOnEnhet() {
        List<Arende> arendeList = new ArrayList<>();

        arendeList.add(buildArende(UUID.randomUUID().toString(), ENHET_ID));
        arendeList.get(0).setSenasteHandelse(FEBRUARY);
        arendeList.add(buildArende(UUID.randomUUID().toString(), "otherUnit"));
        arendeList.get(1).setSenasteHandelse(JANUARY);
        arendeList.add(buildArende(UUID.randomUUID().toString(), ENHET_ID));
        arendeList.get(2).setSenasteHandelse(DECEMBER_YEAR_9999);
        arendeList.add(buildArende(UUID.randomUUID().toString(), "unit-123"));
        arendeList.get(3).setSenasteHandelse(FEBRUARY);

        when(repo.findByIntygsId(INTYG_ID)).thenReturn(arendeList);

        when(webcertUserService.getUser()).thenReturn(createUser());

        List<ArendeConversationView> result = service.getArenden(INTYG_ID);

        verify(repo).findByIntygsId(INTYG_ID);
        verify(webcertUserService).getUser();
        verify(arendeViewConverter).convert(arendeList.get(0));
        verify(arendeViewConverter).convert(arendeList.get(2));
        verify(arendeViewConverter, never()).convert(arendeList.get(1));
        verify(arendeViewConverter, never()).convert(arendeList.get(3));

        assertEquals(2, result.size());
    }

    @Test(expected = WebCertServiceException.class)
    public void testFilterArendeWithAuthFail() {
        WebCertUser webCertUser = createUser();
        when(webcertUserService.getUser()).thenReturn(webCertUser);

        QueryFragaSvarParameter params = new QueryFragaSvarParameter();
        params.setEnhetId("no-auth");

        service.filterArende(params);
    }

    @Test
    public void testFilterArendeWithEnhetsIdAsParam() {
        WebCertUser webCertUser = createUser();
        when(webcertUserService.isAuthorizedForUnit(any(String.class), eq(true))).thenReturn(true);

        List<Arende> queryResults = new ArrayList<>();
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now(), null));
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now().minusDays(1), null));

        when(repo.filterArende(any(Filter.class))).thenReturn(queryResults);
        when(repo.filterArendeCount(any(Filter.class))).thenReturn(queryResults.size() + 1);

        QueryFragaSvarResponse fsResponse = new QueryFragaSvarResponse();
        fsResponse.setResults(new ArrayList<>());
        fsResponse.setTotalCount(0);

        when(fragaSvarService.filterFragaSvar(any(Filter.class))).thenReturn(fsResponse);

        QueryFragaSvarParameter params = new QueryFragaSvarParameter();
        params.setEnhetId(webCertUser.getValdVardenhet().getId());

        QueryFragaSvarResponse response = service.filterArende(params);

        verify(webcertUserService).isAuthorizedForUnit(anyString(), eq(true));

        verify(repo).filterArende(any(Filter.class));
        verify(repo).filterArendeCount(any(Filter.class));
        verify(fragaSvarService).filterFragaSvar(any(Filter.class));

        assertEquals(2, response.getResults().size());
        assertEquals(3, response.getTotalCount());
    }

    @Test
    public void testFilterArendeWithNoEnhetsIdAsParam() {
        when(webcertUserService.getUser()).thenReturn(createUser());

        List<Arende> queryResults = new ArrayList<>();
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now(), null));
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now().plusDays(1), null));

        when(repo.filterArende(any(Filter.class))).thenReturn(queryResults);
        when(repo.filterArendeCount(any(Filter.class))).thenReturn(queryResults.size() + 1);

        QueryFragaSvarResponse fsResponse = new QueryFragaSvarResponse();
        fsResponse.setResults(new ArrayList<>());
        fsResponse.setTotalCount(0);

        when(fragaSvarService.filterFragaSvar(any(Filter.class))).thenReturn(fsResponse);

        QueryFragaSvarParameter params = new QueryFragaSvarParameter();

        QueryFragaSvarResponse response = service.filterArende(params);

        verify(webcertUserService).getUser();

        verify(repo).filterArende(any(Filter.class));
        verify(repo).filterArendeCount(any(Filter.class));
        verify(fragaSvarService).filterFragaSvar(any(Filter.class));

        assertEquals(2, response.getResults().size());
        assertEquals(3, response.getTotalCount());
    }

    @Test
    public void testFilterArendeMergesFragaSvar() {
        when(webcertUserService.getUser()).thenReturn(createUser());

        List<Arende> queryResults = new ArrayList<>();
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now(), null));
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now().plusDays(1), null));

        when(repo.filterArende(any(Filter.class))).thenReturn(queryResults);
        when(repo.filterArendeCount(any(Filter.class))).thenReturn(queryResults.size() + 1);

        QueryFragaSvarResponse fsResponse = new QueryFragaSvarResponse();
        fsResponse.setResults(new ArrayList<>());
        fsResponse.getResults().add(buildArendeListItem("intyg1", LocalDateTime.now().minusDays(1)));
        fsResponse.setTotalCount(1);

        when(fragaSvarService.filterFragaSvar(any(Filter.class))).thenReturn(fsResponse);

        QueryFragaSvarParameter params = new QueryFragaSvarParameter();

        QueryFragaSvarResponse response = service.filterArende(params);

        verify(webcertUserService).getUser();

        verify(repo).filterArende(any(Filter.class));
        verify(repo).filterArendeCount(any(Filter.class));
        verify(fragaSvarService).filterFragaSvar(any(Filter.class));

        assertEquals(3, response.getResults().size());
        assertEquals(4, response.getTotalCount());
    }

    @Test
    public void testFilterArendeInvalidStartPosition() {
        when(webcertUserService.getUser()).thenReturn(createUser());

        List<Arende> queryResults = new ArrayList<>();
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now(), null));
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now().plusDays(1), null));

        when(repo.filterArende(any(Filter.class))).thenReturn(queryResults);
        when(repo.filterArendeCount(any(Filter.class))).thenReturn(queryResults.size() + 1);

        QueryFragaSvarResponse fsResponse = new QueryFragaSvarResponse();
        fsResponse.setResults(new ArrayList<>());
        fsResponse.getResults().add(buildArendeListItem("intyg1", LocalDateTime.now().minusDays(1)));
        fsResponse.setTotalCount(1);

        when(fragaSvarService.filterFragaSvar(any(Filter.class))).thenReturn(fsResponse);

        QueryFragaSvarParameter params = new QueryFragaSvarParameter();
        params.setStartFrom(5);

        QueryFragaSvarResponse response = service.filterArende(params);

        verify(webcertUserService).getUser();

        verify(repo).filterArende(any(Filter.class));
        verify(repo).filterArendeCount(any(Filter.class));
        verify(fragaSvarService).filterFragaSvar(any(Filter.class));

        assertEquals(0, response.getResults().size());
        assertEquals(4, response.getTotalCount());
    }

    @Test
    public void testFilterArendeSelection() {
        when(webcertUserService.getUser()).thenReturn(createUser());
        when(authoritiesHelper.getIntygstyperForPrivilege(any(UserDetails.class), anyString())).thenReturn(new HashSet<>());

        List<Arende> queryResults = new ArrayList<>();
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now(), null));
        queryResults.add(buildArende(UUID.randomUUID().toString(), LocalDateTime.now().plusDays(1), null));

        when(repo.filterArende(any(Filter.class))).thenReturn(queryResults);
        when(repo.filterArendeCount(any(Filter.class))).thenReturn(queryResults.size() + 1);

        QueryFragaSvarResponse fsResponse = new QueryFragaSvarResponse();
        fsResponse.setResults(new ArrayList<>());
        fsResponse.getResults().add(buildArendeListItem("intyg1", LocalDateTime.now().minusDays(1)));
        fsResponse.setTotalCount(1);

        when(fragaSvarService.filterFragaSvar(any(Filter.class))).thenReturn(fsResponse);

        QueryFragaSvarParameter params = new QueryFragaSvarParameter();
        params.setStartFrom(2);
        params.setPageSize(10);

        QueryFragaSvarResponse response = service.filterArende(params);

        verify(webcertUserService).getUser();

        verify(repo).filterArende(any(Filter.class));
        verify(repo).filterArendeCount(any(Filter.class));
        verify(fragaSvarService).filterFragaSvar(any(Filter.class));

        assertEquals(1, response.getResults().size());
        assertEquals(4, response.getTotalCount());
    }

    @Test
    public void testFilterArendeSortsArendeListItemsByReceivedDate() {
        final String intygId1 = "intygId1";
        final String intygId2 = "intygId2";
        final String intygId3 = "intygId3";
        final String MEDDELANDE_ID = "arendeWithPaminnelseMEDDELANDE_ID";

        when(webcertUserService.getUser()).thenReturn(createUser());

        List<Arende> queryResults = new ArrayList<>();
        queryResults.add(buildArende(UUID.randomUUID().toString(), intygId3, LocalDateTime.now().plusDays(2), null, ENHET_ID));

        Arende arendeWithPaminnelse = buildArende(UUID.randomUUID().toString(), intygId2, LocalDateTime.now(), null, ENHET_ID);
        arendeWithPaminnelse.setMeddelandeId(MEDDELANDE_ID);
        queryResults.add(arendeWithPaminnelse);

        when(repo.filterArende(any(Filter.class))).thenReturn(queryResults);
        when(repo.filterArendeCount(any(Filter.class))).thenReturn(queryResults.size());
        when(repo.findByPaminnelseMeddelandeId(eq(MEDDELANDE_ID))).thenReturn(Arrays.asList(new Arende()));

        QueryFragaSvarResponse fsResponse = new QueryFragaSvarResponse();
        fsResponse.setResults(new ArrayList<>());
        fsResponse.getResults().add(buildArendeListItem(intygId1, LocalDateTime.now().minusDays(1)));
        fsResponse.setTotalCount(1);

        when(fragaSvarService.filterFragaSvar(any(Filter.class))).thenReturn(fsResponse);

        QueryFragaSvarParameter params = new QueryFragaSvarParameter();

        QueryFragaSvarResponse response = service.filterArende(params);

        assertEquals(3, response.getResults().size());
        assertEquals(intygId3, response.getResults().get(0).getIntygId());
        assertEquals(intygId2, response.getResults().get(1).getIntygId());
        assertEquals(intygId1, response.getResults().get(2).getIntygId());
    }

    @Test
    public void testGetArende() {
        final String MEDDELANDE_ID = "med0123";
        final String id = UUID.randomUUID().toString();
        Arende arende = buildArende(id, LocalDateTime.now(), null);

        when(repo.findOneByMeddelandeId(MEDDELANDE_ID)).thenReturn(arende);

        Arende res = service.getArende(MEDDELANDE_ID);
        assertEquals(id, res.getMeddelandeId());
    }

    private Arende buildArende(String MEDDELANDE_ID, String enhetId) {
        return buildArende(MEDDELANDE_ID, "<intygsId>", LocalDateTime.now(), LocalDateTime.now(), enhetId);
    }

    private Arende buildArende(String MEDDELANDE_ID, LocalDateTime senasteHandelse, LocalDateTime timestamp) {
        return buildArende(MEDDELANDE_ID, "<intygsId>", senasteHandelse, timestamp, ENHET_ID);
    }

    private Arende buildArende(String meddelandeId, String intygId, LocalDateTime senasteHandelse, LocalDateTime timestamp, String enhetId) {
        Arende arende = new Arende();
        arende.setStatus(Status.PENDING_INTERNAL_ACTION);
        arende.setAmne(ArendeAmne.OVRIGT);
        arende.setReferensId("<fk-extern-referens>");
        arende.setMeddelandeId(meddelandeId);
        arende.setEnhetId(enhetId);
        arende.setSenasteHandelse(senasteHandelse);
        arende.setMeddelande("frageText");
        arende.setTimestamp(timestamp);
        List<MedicinsktArende> komplettering = new ArrayList<>();
        arende.setIntygsId(intygId);
        arende.setPatientPersonId(PATIENT_ID.getPersonnummer());
        arende.setSigneratAv("Signatur");
        arende.setSistaDatumForSvar(senasteHandelse.plusDays(7).toLocalDate());
        arende.setKomplettering(komplettering);
        arende.setRubrik("rubrik");
        arende.setSkickatAv("Avsandare");
        arende.setVidarebefordrad(false);

        return arende;
    }

    private ArendeListItem buildArendeListItem(String INTYG_ID, LocalDateTime receivedDate) {
        ArendeListItem arende = new ArendeListItem();
        arende.setIntygId(INTYG_ID);
        arende.setReceivedDate(receivedDate);

        return arende;
    }

    private Utkast buildUtkast() {
        final String signeratAv = "signeratAv";
        Utkast utkast = new Utkast();
        utkast.setSkapadAv(new VardpersonReferens());
        utkast.getSkapadAv().setHsaId(signeratAv);
        utkast.setSignatur(mock(Signatur.class));
        when(utkast.getSignatur().getSigneradAv()).thenReturn(signeratAv);
        return utkast;
    }

    private WebCertUser createUser() {
        Role role = AUTHORITIES_RESOLVER.getRole(AuthoritiesConstants.ROLE_LAKARE);

        WebCertUser user = new WebCertUser();
        user.setRoles(AuthoritiesResolverUtil.toMap(role));
        user.setAuthorities(AuthoritiesResolverUtil.toMap(role.getPrivileges()));
        user.setHsaId("testuser");
        user.setNamn("test userman");

        Vardenhet vardenhet = new Vardenhet(ENHET_ID, "enhet");

        Vardgivare vardgivare = new Vardgivare("vardgivare", "Vardgivaren");
        vardgivare.getVardenheter().add(vardenhet);

        user.setVardgivare(Collections.singletonList(vardgivare));
        user.setValdVardenhet(vardenhet);

        return user;
    }

}
