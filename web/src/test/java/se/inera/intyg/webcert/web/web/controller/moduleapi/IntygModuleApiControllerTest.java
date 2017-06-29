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
package se.inera.intyg.webcert.web.web.controller.moduleapi;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.inera.intyg.common.fk7263.model.internal.Fk7263Utlatande;
import se.inera.intyg.common.support.model.CertificateState;
import se.inera.intyg.common.support.model.Status;
import se.inera.intyg.infra.security.authorities.AuthoritiesException;
import se.inera.intyg.infra.security.common.model.AuthoritiesConstants;
import se.inera.intyg.infra.security.common.model.Privilege;
import se.inera.intyg.infra.security.common.model.RequestOrigin;
import se.inera.intyg.schemas.contract.Personnummer;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceErrorCodeEnum;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceException;
import se.inera.intyg.webcert.web.service.feature.WebcertFeature;
import se.inera.intyg.webcert.web.service.intyg.IntygService;
import se.inera.intyg.webcert.web.service.intyg.dto.IntygContentHolder;
import se.inera.intyg.webcert.web.service.intyg.dto.IntygPdf;
import se.inera.intyg.webcert.web.service.intyg.dto.IntygServiceResult;
import se.inera.intyg.webcert.web.service.user.WebCertUserService;
import se.inera.intyg.webcert.web.service.user.dto.IntegrationParameters;
import se.inera.intyg.webcert.web.service.user.dto.WebCertUser;
import se.inera.intyg.webcert.web.service.utkast.CopyUtkastService;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateCompletionCopyRequest;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateCompletionCopyResponse;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateRenewalCopyRequest;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateRenewalCopyResponse;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateReplacementCopyRequest;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateReplacementCopyResponse;
import se.inera.intyg.webcert.web.web.controller.api.dto.CopyIntygRequest;
import se.inera.intyg.webcert.web.web.controller.api.dto.CopyIntygResponse;
import se.inera.intyg.webcert.web.web.controller.moduleapi.dto.RevokeSignedIntygParameter;
import se.inera.intyg.webcert.web.web.controller.moduleapi.dto.SendSignedIntygParameter;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author andreaskaltenbach
 */
@RunWith(MockitoJUnitRunner.class)
public class IntygModuleApiControllerTest {

    private static final String CERTIFICATE_ID = "123456";
    private static final String CERTIFICATE_TYPE = "fk7263";

    private static final byte[] PDF_DATA = "<pdf-data>".getBytes();
    private static final String PDF_NAME = "the-file.pdf";

    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    @Mock
    private IntygService intygService;

    @Mock
    private CopyUtkastService copyUtkastService;

    @Mock
    private WebCertUserService webcertUserService;

    @InjectMocks
    private IntygModuleApiController moduleApiController = new IntygModuleApiController();

    @BeforeClass
    public static void setupCertificateData() throws IOException {
        Fk7263Utlatande utlatande = new Fk7263Utlatande();
        utlatande.setId(CERTIFICATE_ID);
        utlatande.setTyp(CERTIFICATE_TYPE);

        List<Status> status = new ArrayList<>();
        status.add(new Status(CertificateState.RECEIVED, "HSVARD", LocalDateTime.now()));
        status.add(new Status(CertificateState.SENT, "FKASSA", LocalDateTime.now()));
    }

    @Test
    public void testGetIntygAsPdf() throws Exception {

        final String intygType = "fk7263";
        setupUser(AuthoritiesConstants.PRIVILEGE_VISA_INTYG, intygType, false, true, WebcertFeature.UTSKRIFT);
        IntygPdf pdfResponse = new IntygPdf(PDF_DATA, PDF_NAME);

        when(intygService.fetchIntygAsPdf(CERTIFICATE_ID, intygType, false)).thenReturn(pdfResponse);

        Response response = moduleApiController.getIntygAsPdf(intygType, CERTIFICATE_ID);

        verify(intygService).fetchIntygAsPdf(CERTIFICATE_ID, intygType, false);

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(PDF_DATA, response.getEntity());
        assertNotNull(response.getHeaders().get(CONTENT_DISPOSITION));
    }

    @Test(expected = AuthoritiesException.class)
    public void testGetIntygAsPdfNotAuthorised() throws Exception {
        setupUser("", "", false, true);
        moduleApiController.getIntygAsPdf("fk7263", CERTIFICATE_ID);

    }

    @Test
    public void testGetIntygAsPdfForEmployer() throws Exception {

        final String intygType = "fk7263";
        setupUser(AuthoritiesConstants.PRIVILEGE_VISA_INTYG, intygType, false, true, WebcertFeature.ARBETSGIVARUTSKRIFT);
        IntygPdf pdfResponse = new IntygPdf(PDF_DATA, PDF_NAME);

        when(intygService.fetchIntygAsPdf(CERTIFICATE_ID, intygType, true)).thenReturn(pdfResponse);

        Response response = moduleApiController.getIntygAsPdfForEmployer(intygType, CERTIFICATE_ID);

        verify(intygService).fetchIntygAsPdf(CERTIFICATE_ID, intygType, true);

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(PDF_DATA, response.getEntity());
        assertNotNull(response.getHeaders().get(CONTENT_DISPOSITION));
    }

    @Test(expected = AuthoritiesException.class)
    public void testGetIntygAsPdfForEmployerNotAuthorised() throws Exception {
        setupUser("", "", false, true);
        moduleApiController.getIntygAsPdf("fk7263", CERTIFICATE_ID);
    }

    @Test
    public void testGetIntyg() {
        final String intygsTyp = "fk7263";
        final String intygContent = "CONTENTS";

        setupUser(AuthoritiesConstants.PRIVILEGE_VISA_INTYG, intygsTyp, false, true);

        IntygContentHolder content = mock(IntygContentHolder.class);
        when(content.getContents()).thenReturn(intygContent);
        when(intygService.fetchIntygDataWithRelations(eq(CERTIFICATE_ID), eq(intygsTyp), eq(false))).thenReturn(content);

        Response response = moduleApiController.getIntyg(intygsTyp, CERTIFICATE_ID);

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(intygContent, ((IntygContentHolder) response.getEntity()).getContents());
        verify(intygService).fetchIntygDataWithRelations(eq(CERTIFICATE_ID), eq(intygsTyp), eq(false));
    }

    @Test
    public void testGetIntygWithCoherentJournaling() {
        final String intygsTyp = "fk7263";
        final String intygContent = "CONTENTS";

        setupUser(AuthoritiesConstants.PRIVILEGE_VISA_INTYG, intygsTyp, true, true);

        IntygContentHolder content = mock(IntygContentHolder.class);
        when(content.getContents()).thenReturn(intygContent);
        when(intygService.fetchIntygDataWithRelations(eq(CERTIFICATE_ID), eq(intygsTyp), eq(true))).thenReturn(content);

        Response response = moduleApiController.getIntyg(intygsTyp, CERTIFICATE_ID);

        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(intygContent, ((IntygContentHolder) response.getEntity()).getContents());
        verify(intygService).fetchIntygDataWithRelations(eq(CERTIFICATE_ID), eq(intygsTyp), eq(true));
    }

    @Test(expected = AuthoritiesException.class)
    public void testGetIntygNotAuthorised() {
        setupUser("", "", false, true);
        moduleApiController.getIntyg("fk7263", CERTIFICATE_ID);
    }

    @Test
    public void testCreateNewRenewWithCopyOkParamFalse() {
        //Given
        final boolean copyOk = false;
        final String intygsTyp = "fk7263";
        setupUser(AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG, intygsTyp, true, copyOk, WebcertFeature.KOPIERA_INTYG);

        //When
        try {
            moduleApiController.createRenewal(new CopyIntygRequest(), intygsTyp, "");
        } catch (WebCertServiceException wcse) {
            //Then an exception is thrown
            assertEquals(WebCertServiceErrorCodeEnum.AUTHORIZATION_PROBLEM, wcse.getErrorCode());
            assertEquals("Authorization failed due to false kopieraOK-parameter", wcse.getMessage());
            return;
        }
        fail("No or wrong exception was thrown");
    }

    @Test
    public void testSendSignedIntyg() {
        final String intygType = "fk7263";
        final String recipient = "recipient";

        setupUser("", intygType, false, true, WebcertFeature.SKICKA_INTYG);

        when(intygService.sendIntyg(eq(CERTIFICATE_ID), eq(intygType), eq(recipient))).thenReturn(IntygServiceResult.OK);

        SendSignedIntygParameter param = new SendSignedIntygParameter();
        param.setRecipient(recipient);
        Response response = moduleApiController.sendSignedIntyg(intygType, CERTIFICATE_ID, param);

        verify(intygService).sendIntyg(eq(CERTIFICATE_ID), eq(intygType), eq(recipient));
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(IntygServiceResult.OK, response.getEntity());
    }

    @Test(expected = AuthoritiesException.class)
    public void testSendSignedIntygNotAuthorised() {
        setupUser("", "", false, true);
        moduleApiController.sendSignedIntyg("intygType", CERTIFICATE_ID, null);
    }

    @Test
    public void testRevokeSignedIntyg() {
        final String intygType = "fk7263";
        final String revokeMessage = "revokeMessage";
        final String revokeReason = "revokeReason";

        setupUser(AuthoritiesConstants.PRIVILEGE_MAKULERA_INTYG, intygType, false, true, WebcertFeature.MAKULERA_INTYG);

        when(intygService.revokeIntyg(CERTIFICATE_ID, intygType, revokeMessage, revokeReason)).thenReturn(IntygServiceResult.OK);

        RevokeSignedIntygParameter param = new RevokeSignedIntygParameter();
        param.setMessage(revokeMessage);
        param.setReason(revokeReason);
        Response response = moduleApiController.revokeSignedIntyg(intygType, CERTIFICATE_ID, param);

        verify(intygService).revokeIntyg(CERTIFICATE_ID, intygType, revokeMessage, revokeReason);
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertEquals(IntygServiceResult.OK, response.getEntity());
    }

    @Test(expected = AuthoritiesException.class)
    public void testRevokeSignedIntygNotAuthorised() {
        setupUser("", "", false, true);
        moduleApiController.revokeSignedIntyg("intygType", CERTIFICATE_ID, null);
    }

//    @Test
//    public void testCreateNewCopy() {
//        final String personnummer = "191212121212";
//        final String newIntygId = "newIntygId";
//
//        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();
//        copyIntygRequest.setPatientPersonnummer(new Personnummer(personnummer));
//
//        WebCertUser user = new WebCertUser();
//        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
//        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG);
//        user.setOrigin("NORMAL");
//
//        ArgumentCaptor<CreateNewDraftCopyRequest> captor = ArgumentCaptor.forClass(CreateNewDraftCopyRequest.class);
//        when(copyUtkastService.createCopy(captor.capture())).thenReturn(new CreateNewDraftCopyResponse(CERTIFICATE_TYPE, newIntygId));
//        when(webcertUserService.getUser()).thenReturn(user);
//
//        Response response = moduleApiController.createNewCopy(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);
//
//        verify(copyUtkastService).createCopy(any());
//        verifyNoMoreInteractions(copyUtkastService);
//        assertEquals(newIntygId, ((CopyIntygResponse) response.getEntity()).getIntygsUtkastId());
//        assertEquals(personnummer, captor.getValue().getPatient().getPersonId().getPersonnummer());
//        assertNull(captor.getValue().getPatient().getFornamn());
//        assertNull(captor.getValue().getPatient().getEfternamn());
//        assertNull(captor.getValue().getPatient().getMellannamn());
//        assertNull(captor.getValue().getPatient().getPostadress());
//        assertNull(captor.getValue().getPatient().getPostnummer());
//        assertNull(captor.getValue().getPatient().getPostort());
//        assertNull(captor.getValue().getNyttPatientPersonnummer());
//    }

    @Test
    public void testCreateRenewalWithNewPatientInfo() {
        final String personnummer = "191212121212";
        final String newIntygId = "newIntygId";
        final String efternamn = "efternamn";
        final String fornamn = "fornamn";
        final String mellannamn = "mellannamn";
        final String postadress = "postadress";
        final String postort = "postort";
        final String postnummer = "postnummer";
        final String newPersonnummer = "201212121212";

        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();
        copyIntygRequest.setPatientPersonnummer(new Personnummer(personnummer));

        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(null, null, newPersonnummer, fornamn, mellannamn, efternamn, postadress, postnummer,
                postort, false, false, false, true));
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG);
        user.setOrigin("NORMAL");

        ArgumentCaptor<CreateRenewalCopyRequest> captor = ArgumentCaptor.forClass(CreateRenewalCopyRequest.class);
        when(copyUtkastService.createRenewalCopy(captor.capture())).thenReturn(new CreateRenewalCopyResponse(CERTIFICATE_TYPE, newIntygId, CERTIFICATE_ID));
        when(webcertUserService.getUser()).thenReturn(user);

        Response response = moduleApiController.createRenewal(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);

        verify(copyUtkastService).createRenewalCopy(any());
        verifyNoMoreInteractions(copyUtkastService);
        assertEquals(newIntygId, ((CopyIntygResponse) response.getEntity()).getIntygsUtkastId());
        assertEquals(personnummer, captor.getValue().getPatient().getPersonId().getPersonnummer());
        assertEquals(fornamn, captor.getValue().getPatient().getFornamn());
        assertEquals(efternamn, captor.getValue().getPatient().getEfternamn());
        assertEquals(mellannamn, captor.getValue().getPatient().getMellannamn());
        assertEquals(postadress, captor.getValue().getPatient().getPostadress());
        assertEquals(postnummer, captor.getValue().getPatient().getPostnummer());
        assertEquals(postort, captor.getValue().getPatient().getPostort());
        assertEquals(newPersonnummer, captor.getValue().getNyttPatientPersonnummer().getPersonnummer());
    }

    /**
     * Verify that a non-valid personnr/samordningsnummer (i.e a "reservnummer") supplied as NyttPatientPersonnummer
     * will not be applied to the new utkast.
     */
    @Test
    public void testCreateRenewalCopyWithNewPatientReservnummerDefaultsToPreviousPersonnummer() {
        final String personnummer = "191212121212";
        final String newIntygId = "newIntygId";
        final String efternamn = "efternamn";
        final String fornamn = "fornamn";
        final String mellannamn = "mellannamn";
        final String postadress = "postadress";
        final String postort = "postort";
        final String postnummer = "postnummer";
        final String newReservnummer = "A20090122";

        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();
        copyIntygRequest.setPatientPersonnummer(new Personnummer(personnummer));

        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(null, null, newReservnummer, fornamn, mellannamn, efternamn, postadress, postnummer,
                postort, false, false, false, true));
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG);
        user.setOrigin("NORMAL");

        ArgumentCaptor<CreateRenewalCopyRequest> captor = ArgumentCaptor.forClass(CreateRenewalCopyRequest.class);
        when(copyUtkastService.createRenewalCopy(captor.capture())).thenReturn(new CreateRenewalCopyResponse(CERTIFICATE_TYPE, newIntygId, CERTIFICATE_ID));
        when(webcertUserService.getUser()).thenReturn(user);

        Response response = moduleApiController.createRenewal(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);

        verify(copyUtkastService).createRenewalCopy(any());
        verifyNoMoreInteractions(copyUtkastService);
        assertEquals(newIntygId, ((CopyIntygResponse) response.getEntity()).getIntygsUtkastId());
        assertEquals(personnummer, captor.getValue().getPatient().getPersonId().getPersonnummer());
        assertEquals(fornamn, captor.getValue().getPatient().getFornamn());
        assertEquals(efternamn, captor.getValue().getPatient().getEfternamn());
        assertEquals(mellannamn, captor.getValue().getPatient().getMellannamn());
        assertEquals(postadress, captor.getValue().getPatient().getPostadress());
        assertEquals(postnummer, captor.getValue().getPatient().getPostnummer());
        assertEquals(postort, captor.getValue().getPatient().getPostort());
        assertNull(captor.getValue().getNyttPatientPersonnummer());
    }

    @Test(expected = AuthoritiesException.class)
    public void testCreateRenewalCopyMissingFeature() {
        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();

        WebCertUser user = new WebCertUser();
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG);
        user.setOrigin("NORMAL");
        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createRenewal(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    @Test(expected = AuthoritiesException.class)
    public void testCreateRenewalCopyMissingPrivilege() {
        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();

        WebCertUser user = new WebCertUser();
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        user.setOrigin("NORMAL");
        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createRenewal(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    @Test(expected = WebCertServiceException.class)
    public void testCreateRenewalCopyInvalidRequest() {
        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();

        WebCertUser user = new WebCertUser();
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG);
        user.setOrigin("NORMAL");

        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createRenewal(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    @Test
    public void testReplaceIntyg() {
        final String personnummer = "191212121212";
        final String newIntygId = "newIntygId";

        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();
        copyIntygRequest.setPatientPersonnummer(new Personnummer(personnummer));

        WebCertUser user = new WebCertUser();
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_ERSATTA_INTYG);
        user.setOrigin("NORMAL");

        ArgumentCaptor<CreateReplacementCopyRequest> captor = ArgumentCaptor.forClass(CreateReplacementCopyRequest.class);
        when(copyUtkastService.createReplacementCopy(captor.capture())).thenReturn(new CreateReplacementCopyResponse(CERTIFICATE_TYPE, newIntygId, CERTIFICATE_ID));
        when(webcertUserService.getUser()).thenReturn(user);

        Response response = moduleApiController.createReplacement(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);

        verify(copyUtkastService).createReplacementCopy(any());
        verifyNoMoreInteractions(copyUtkastService);
        assertEquals(newIntygId, ((CopyIntygResponse) response.getEntity()).getIntygsUtkastId());
    }

    @Test(expected = AuthoritiesException.class)
    public void testReplaceIntygWithInvalidOrigin() {
        final String personnummer = "191212121212";

        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();
        copyIntygRequest.setPatientPersonnummer(new Personnummer(personnummer));

        WebCertUser user = new WebCertUser();
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_ERSATTA_INTYG);
        user.setOrigin("UTHOPP");

        when(webcertUserService.getUser()).thenReturn(user);
        try {
            moduleApiController.createReplacement(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);
            fail("Expected exception!");
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }

    }

    @Test(expected = AuthoritiesException.class)
    public void testReplaceIntygWithInvalidPriviledge() {
        final String personnummer = "191212121212";

        CopyIntygRequest copyIntygRequest = new CopyIntygRequest();
        copyIntygRequest.setPatientPersonnummer(new Personnummer(personnummer));

        WebCertUser user = new WebCertUser();
        user.setOrigin("NORMAL");

        when(webcertUserService.getUser()).thenReturn(user);
        try {
            moduleApiController.createReplacement(copyIntygRequest, CERTIFICATE_TYPE, CERTIFICATE_ID);
            fail("Expected exception!");
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }

    }
    @Test
    public void testCreateNewCompletion() {
        final String personnummer = "191212121212";
        final String newIntygId = "newIntygId";
        final String meddelandeId = "meddelandeId";
        final String newPersonnummer = "201212121212";
        final String efternamn = "efternamn";
        final String fornamn = "fornamn";
        final String mellannamn = "mellannamn";
        final String postadress = "postadress";
        final String postort = "postort";
        final String postnummer = "postnummer";

        final CopyIntygRequest request = new CopyIntygRequest();
        request.setPatientPersonnummer(new Personnummer(personnummer));

        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(null, null, newPersonnummer, fornamn, mellannamn, efternamn, postadress, postnummer,
                postort, false, false, false, true));
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_SVARA_MED_NYTT_INTYG);
        user.setOrigin("NORMAL");

        ArgumentCaptor<CreateCompletionCopyRequest> captor = ArgumentCaptor.forClass(CreateCompletionCopyRequest.class);
        when(copyUtkastService.createCompletion(captor.capture()))
                .thenReturn(new CreateCompletionCopyResponse(CERTIFICATE_TYPE, newIntygId, CERTIFICATE_ID));
        when(webcertUserService.getUser()).thenReturn(user);

        Response response = moduleApiController.createCompletion(request, CERTIFICATE_TYPE, CERTIFICATE_ID, meddelandeId);

        verify(copyUtkastService).createCompletion(any());
        verifyNoMoreInteractions(copyUtkastService);
        assertEquals(newIntygId, ((CopyIntygResponse) response.getEntity()).getIntygsUtkastId());
        assertEquals(personnummer, captor.getValue().getPatient().getPersonId().getPersonnummer());
        assertEquals(fornamn, captor.getValue().getPatient().getFornamn());
        assertEquals(efternamn, captor.getValue().getPatient().getEfternamn());
        assertEquals(mellannamn, captor.getValue().getPatient().getMellannamn());
        assertEquals(postadress, captor.getValue().getPatient().getPostadress());
        assertEquals(postnummer, captor.getValue().getPatient().getPostnummer());
        assertEquals(postort, captor.getValue().getPatient().getPostort());
        assertEquals(newPersonnummer, captor.getValue().getNyttPatientPersonnummer().getPersonnummer());
    }

    @Test(expected = AuthoritiesException.class)
    public void testCreateNewCompletionMissingFeature() {
        WebCertUser user = new WebCertUser();
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_SVARA_MED_NYTT_INTYG);
        user.setOrigin("NORMAL");

        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createCompletion(null, CERTIFICATE_TYPE, null, null);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    @Test(expected = AuthoritiesException.class)
    public void testCreateNewCompletionMissingPrivilege() {
        WebCertUser user = new WebCertUser();
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        user.setOrigin("NORMAL");

        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createCompletion(null, CERTIFICATE_TYPE, null, null);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    @Test(expected = WebCertServiceException.class)
    public void testCreateNewCompletionMissingRequest() {
        WebCertUser user = new WebCertUser();
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_SVARA_MED_NYTT_INTYG);
        user.setOrigin("NORMAL");

        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createCompletion(new CopyIntygRequest(), CERTIFICATE_TYPE, null, null);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    @Test
    public void testCreateRenewal() {
        final String newDraftIntygId = "newDraftIntygId";
        final String personnummer = "191212121212";
        final String newPersonnummer = "201212121212";
        final String efternamn = "efternamn";
        final String fornamn = "fornamn";
        final String mellannamn = "mellannamn";
        final String postadress = "postadress";
        final String postort = "postort";
        final String postnummer = "postnummer";

        CopyIntygRequest request = new CopyIntygRequest();
        request.setPatientPersonnummer(new Personnummer(personnummer));

        WebCertUser user = new WebCertUser();
        user.setParameters(new IntegrationParameters(null, null, newPersonnummer, fornamn, mellannamn, efternamn, postadress, postnummer,
                postort, false, false, false, true));
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG);
        user.setOrigin("NORMAL");

        ArgumentCaptor<CreateRenewalCopyRequest> captor = ArgumentCaptor.forClass(CreateRenewalCopyRequest.class);
        when(copyUtkastService.createRenewalCopy(captor.capture()))
                .thenReturn(new CreateRenewalCopyResponse(CERTIFICATE_TYPE, newDraftIntygId, CERTIFICATE_ID));
        when(webcertUserService.getUser()).thenReturn(user);

        Response response = moduleApiController.createRenewal(request, CERTIFICATE_TYPE, CERTIFICATE_ID);

        verify(copyUtkastService).createRenewalCopy(any());
        verifyNoMoreInteractions(copyUtkastService);
        assertEquals(newDraftIntygId, ((CopyIntygResponse) response.getEntity()).getIntygsUtkastId());
        assertEquals(personnummer, captor.getValue().getPatient().getPersonId().getPersonnummer());
        assertEquals(fornamn, captor.getValue().getPatient().getFornamn());
        assertEquals(efternamn, captor.getValue().getPatient().getEfternamn());
        assertEquals(mellannamn, captor.getValue().getPatient().getMellannamn());
        assertEquals(postadress, captor.getValue().getPatient().getPostadress());
        assertEquals(postnummer, captor.getValue().getPatient().getPostnummer());
        assertEquals(postort, captor.getValue().getPatient().getPostort());
        assertEquals(newPersonnummer, captor.getValue().getNyttPatientPersonnummer().getPersonnummer());
    }

    @Test(expected = AuthoritiesException.class)
    public void testCreateRenewalMissingFeature() {
        WebCertUser user = new WebCertUser();
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG);
        user.setOrigin("NORMAL");

        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createRenewal(null, CERTIFICATE_TYPE, CERTIFICATE_ID);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    @Test(expected = AuthoritiesException.class)
    public void testCreateRenewalMissingPrivilege() {
        WebCertUser user = new WebCertUser();
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        user.setOrigin("NORMAL");

        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createRenewal(null, CERTIFICATE_TYPE, CERTIFICATE_ID);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    @Test(expected = WebCertServiceException.class)
    public void testCreateRenewalInvalidRequest() {
        WebCertUser user = new WebCertUser();
        addFeatures(user, CERTIFICATE_TYPE, WebcertFeature.KOPIERA_INTYG);
        addPrivileges(user, CERTIFICATE_TYPE, AuthoritiesConstants.PRIVILEGE_KOPIERA_INTYG);
        user.setOrigin("NORMAL");

        when(webcertUserService.getUser()).thenReturn(user);

        try {
            moduleApiController.createRenewal(new CopyIntygRequest(), CERTIFICATE_TYPE, CERTIFICATE_ID);
        } finally {
            verifyZeroInteractions(copyUtkastService);
        }
    }

    private void setupUser(String privilegeString, String intygType, boolean coherentJournaling, boolean copyOk, WebcertFeature... features) {
        WebCertUser user = new WebCertUser();
        user.setAuthorities(new HashMap<>());
        user.setFeatures(Stream.of(features).map(WebcertFeature::getName).collect(Collectors.toSet()));
        user.getFeatures().addAll(Stream.of(features).map(f -> f.getName() + "." + intygType).collect(Collectors.toSet()));
        user.setParameters(
                new IntegrationParameters(null, null, null, null, null, null, null, null, null, coherentJournaling, false, false, copyOk));
        Privilege privilege = new Privilege();
        privilege.setIntygstyper(Arrays.asList(intygType));
        RequestOrigin requestOrigin = new RequestOrigin();
        requestOrigin.setName("NORMAL");
        requestOrigin.setIntygstyper(privilege.getIntygstyper());
        privilege.setRequestOrigins(Arrays.asList(requestOrigin));
        user.getAuthorities().put(privilegeString, privilege);
        user.setOrigin("NORMAL");
        when(webcertUserService.getUser()).thenReturn(user);
    }

    private void addFeatures(WebCertUser user, String intygType, WebcertFeature... features) {
        user.setFeatures(Stream.of(features).map(WebcertFeature::getName).collect(Collectors.toSet()));
        user.getFeatures().addAll(Stream.of(features).map(f -> f.getName() + "." + intygType).collect(Collectors.toSet()));
    }

    private void addPrivileges(WebCertUser user, String intygType, String... privileges) {
        user.setAuthorities(new HashMap<>());
        Privilege privilege = new Privilege();
        privilege.setIntygstyper(Arrays.asList(intygType));
        RequestOrigin requestOrigin = new RequestOrigin();
        requestOrigin.setName("NORMAL");
        requestOrigin.setIntygstyper(privilege.getIntygstyper());
        privilege.setRequestOrigins(Arrays.asList(requestOrigin));
        for (String privilegeString : privileges) {
            user.getAuthorities().put(privilegeString, privilege);
        }
    }
}
