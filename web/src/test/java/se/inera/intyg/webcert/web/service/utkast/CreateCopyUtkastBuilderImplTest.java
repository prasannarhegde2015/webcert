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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import se.inera.intyg.common.fk7263.model.internal.Fk7263Utlatande;
import se.inera.intyg.common.support.model.CertificateState;
import se.inera.intyg.common.support.model.Status;
import se.inera.intyg.common.support.model.common.internal.HoSPersonal;
import se.inera.intyg.common.support.model.common.internal.Patient;
import se.inera.intyg.common.support.model.common.internal.Vardenhet;
import se.inera.intyg.common.support.model.common.internal.Vardgivare;
import se.inera.intyg.common.support.modules.registry.IntygModuleRegistry;
import se.inera.intyg.common.support.modules.support.api.ModuleApi;
import se.inera.intyg.common.support.modules.support.api.dto.CreateDraftCopyHolder;
import se.inera.intyg.common.support.modules.support.api.dto.ValidateDraftResponse;
import se.inera.intyg.common.support.modules.support.api.dto.ValidationStatus;
import se.inera.intyg.common.util.integration.integration.json.CustomObjectMapper;
import se.inera.intyg.infra.integration.hsa.model.AbstractVardenhet;
import se.inera.intyg.infra.integration.pu.model.Person;
import se.inera.intyg.schemas.contract.Personnummer;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceException;
import se.inera.intyg.webcert.persistence.utkast.model.Utkast;
import se.inera.intyg.webcert.persistence.utkast.model.VardpersonReferens;
import se.inera.intyg.webcert.persistence.utkast.repository.UtkastRepository;
import se.inera.intyg.webcert.web.service.intyg.IntygService;
import se.inera.intyg.webcert.web.service.intyg.dto.IntygContentHolder;
import se.inera.intyg.webcert.web.service.user.WebCertUserService;
import se.inera.intyg.webcert.web.service.user.dto.WebCertUser;
import se.inera.intyg.webcert.web.service.utkast.dto.CopyUtkastBuilderResponse;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateRenewalCopyRequest;
import se.inera.intyg.webcert.web.service.utkast.util.CreateIntygsIdStrategy;
import se.inera.intyg.webcert.web.web.controller.api.dto.Relations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateCopyUtkastBuilderImplTest {

    private static final String INTYG_ID = "abc123";
    private static final String INTYG_COPY_ID = "def456";

    private static final String INTYG_JSON = "A bit of text representing json";

    private static final String INTYG_TYPE = "fk7263";

    private static final Personnummer PATIENT_SSN = new Personnummer("19121212-1212");
    private static final String PATIENT_FNAME = "Adam";
    private static final String PATIENT_MNAME = "Bertil";
    private static final String PATIENT_LNAME = "Caesarsson";

    private static final Personnummer PATIENT_NEW_SSN = new Personnummer("19121212-1414");

    private static final String VARDENHET_ID = "SE00001234-5678";
    private static final String VARDENHET_NAME = "Vårdenheten 1";

    private static final String VARDGIVARE_ID = "SE00001234-1234";
    private static final String VARDGIVARE_NAME = "Vårdgivaren 1";

    private static final String HOSPERSON_ID = "SE12345678-0001";
    private static final String HOSPERSON_NAME = "Dr Börje Dengroth";

    @Mock
    private IntygService mockIntygService;

    @Mock
    private UtkastRepository mockUtkastRepository;

    @Mock
    private IntygModuleRegistry moduleRegistry;

    @Mock
    private WebCertUserService webcertUserService;

    @Spy
    private CreateIntygsIdStrategy mockIdStrategy = new CreateIntygsIdStrategy() {
        @Override
        public String createId() {
            return INTYG_COPY_ID;
        }
    };

    private ModuleApi mockModuleApi;

    private HoSPersonal hoSPerson;

    private Patient patient;

    @InjectMocks
    private CreateCopyUtkastBuilder copyBuilder = new CreateCopyUtkastBuilder();

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
    public void expectCallToModuleRegistry() throws Exception {
        this.mockModuleApi = mock(ModuleApi.class);
        when(moduleRegistry.getModuleApi(INTYG_TYPE)).thenReturn(mockModuleApi);
    }

    @Before
    public void expectCallToWebcertUserService() {
        when(webcertUserService.getUser()).thenReturn(createWebcertUser());
        when(webcertUserService.isAuthorizedForUnit(VARDGIVARE_ID, VARDENHET_ID, true)).thenReturn(true);
    }

    @Test
    public void testPopulateCopyUtkastFromSignedIntyg() throws Exception {

        IntygContentHolder ich = createIntygContentHolder();
        when(mockIntygService.fetchIntygData(INTYG_ID, INTYG_TYPE, false)).thenReturn(ich);

        CreateRenewalCopyRequest renewalRequest = buildRenewalRequest();
        Person patientDetails = new Person(PATIENT_SSN, false, false, PATIENT_FNAME, PATIENT_MNAME, PATIENT_LNAME, "Postadr", "12345",
                "postort");

        when(mockModuleApi.createNewInternalFromTemplate(any(CreateDraftCopyHolder.class), anyString())).thenReturn(INTYG_JSON);

        ValidateDraftResponse vdr = new ValidateDraftResponse(ValidationStatus.VALID, new ArrayList<>());
        when(mockModuleApi.validateDraft(anyString())).thenReturn(vdr);

        CopyUtkastBuilderResponse builderResponse = copyBuilder.populateCopyUtkastFromSignedIntyg(renewalRequest, patientDetails, false,
                false, false);

        assertNotNull(builderResponse.getUtkastCopy());
        assertNotNull(builderResponse.getUtkastCopy().getModel());
        assertEquals(INTYG_TYPE, builderResponse.getUtkastCopy().getIntygsTyp());
        assertEquals(PATIENT_SSN, builderResponse.getUtkastCopy().getPatientPersonnummer());
        assertEquals(PATIENT_FNAME, builderResponse.getUtkastCopy().getPatientFornamn());
        assertEquals(PATIENT_MNAME, builderResponse.getUtkastCopy().getPatientMellannamn());
        assertEquals(PATIENT_LNAME, builderResponse.getUtkastCopy().getPatientEfternamn());

        ArgumentCaptor<CreateDraftCopyHolder> requestCaptor = ArgumentCaptor.forClass(CreateDraftCopyHolder.class);
        verify(mockModuleApi).createNewInternalFromTemplate(requestCaptor.capture(), anyString());

        // verify full name is set
        assertNotNull(requestCaptor.getValue().getPatient().getFullstandigtNamn());
        assertEquals(PATIENT_FNAME + " " + PATIENT_MNAME + " " + PATIENT_LNAME,
                requestCaptor.getValue().getPatient().getFullstandigtNamn());
    }

    @Test(expected = WebCertServiceException.class)
    public void testPopulateCopyUtkastFromSignedIntygEnforceVardenhet() throws Exception {

        IntygContentHolder ich = createIntygContentHolder();
        when(mockIntygService.fetchIntygData(INTYG_ID, INTYG_TYPE, true)).thenReturn(ich);

        CreateRenewalCopyRequest renewalRequest = buildRenewalRequest();
        Person patientDetails = new Person(PATIENT_SSN, false, false, PATIENT_FNAME, PATIENT_MNAME, PATIENT_LNAME, "Postadr", "12345",
                "postort");

        when(mockModuleApi.createNewInternalFromTemplate(any(CreateDraftCopyHolder.class), anyString())).thenReturn(INTYG_JSON);

        ValidateDraftResponse vdr = new ValidateDraftResponse(ValidationStatus.VALID, new ArrayList<>());
        when(mockModuleApi.validateDraft(anyString())).thenReturn(vdr);

        copyBuilder.populateCopyUtkastFromSignedIntyg(renewalRequest, patientDetails, false,
                true, true);
    }

    @Test
    public void testPopulateCopyUtkastFromOriginal() throws Exception {

        Utkast orgUtkast = createOriginalUtkast();
        when(mockUtkastRepository.findOne(INTYG_ID)).thenReturn(orgUtkast);

        CreateRenewalCopyRequest renewalRequest = buildRenewalRequest();
        Person patientDetails = new Person(PATIENT_SSN, false, false, PATIENT_FNAME, PATIENT_MNAME, PATIENT_LNAME, "Postadr", "12345",
                "postort");

        when(mockModuleApi.createNewInternalFromTemplate(any(CreateDraftCopyHolder.class), anyString())).thenReturn(INTYG_JSON);

        ValidateDraftResponse vdr = new ValidateDraftResponse(ValidationStatus.VALID, new ArrayList<>());
        when(mockModuleApi.validateDraft(anyString())).thenReturn(vdr);

        CopyUtkastBuilderResponse builderResponse = copyBuilder.populateCopyUtkastFromOrignalUtkast(renewalRequest, patientDetails, false,
                false, false);

        assertNotNull(builderResponse.getUtkastCopy());
        assertNotNull(builderResponse.getUtkastCopy().getModel());
        assertEquals(INTYG_TYPE, builderResponse.getUtkastCopy().getIntygsTyp());
        assertEquals(PATIENT_SSN, builderResponse.getUtkastCopy().getPatientPersonnummer());
        assertEquals(PATIENT_FNAME, builderResponse.getUtkastCopy().getPatientFornamn());
        assertNotNull(builderResponse.getUtkastCopy().getPatientMellannamn());
        assertEquals(PATIENT_LNAME, builderResponse.getUtkastCopy().getPatientEfternamn());
    }

    @Test(expected = WebCertServiceException.class)
    public void testPopulateCopyUtkastFromOriginalEnforceVardenhet() throws Exception {

        Utkast orgUtkast = createOriginalUtkast();
        orgUtkast.setEnhetsId("OTHER_ID");
        when(mockUtkastRepository.findOne(INTYG_ID)).thenReturn(orgUtkast);

        CreateRenewalCopyRequest renewalRequest = buildRenewalRequest();
        Person patientDetails = new Person(PATIENT_SSN, false, false, PATIENT_FNAME, PATIENT_MNAME, PATIENT_LNAME, "Postadr", "12345",
                "postort");

        copyBuilder.populateCopyUtkastFromOrignalUtkast(renewalRequest, patientDetails, false, true, true);
    }

    @Test
    public void testPopulateCopyUtkastFromOriginalWhenIntegratedAndWithUpdatedSSN() throws Exception {

        Utkast orgUtkast = createOriginalUtkast();
        when(mockUtkastRepository.findOne(INTYG_ID)).thenReturn(orgUtkast);

        CreateRenewalCopyRequest renewalRequest = buildRenewalRequest();
        renewalRequest.setNyttPatientPersonnummer(PATIENT_NEW_SSN);
        renewalRequest.setDjupintegrerad(true);

        when(mockModuleApi.createNewInternalFromTemplate(any(CreateDraftCopyHolder.class), anyString())).thenReturn(INTYG_JSON);

        ValidateDraftResponse vdr = new ValidateDraftResponse(ValidationStatus.VALID, new ArrayList<>());
        when(mockModuleApi.validateDraft(anyString())).thenReturn(vdr);

        CopyUtkastBuilderResponse builderResponse = copyBuilder.populateCopyUtkastFromOrignalUtkast(renewalRequest, null, false, false, false);

        assertNotNull(builderResponse.getUtkastCopy());
        assertNotNull(builderResponse.getUtkastCopy().getModel());
        assertEquals(INTYG_TYPE, builderResponse.getUtkastCopy().getIntygsTyp());
        assertEquals(PATIENT_NEW_SSN, builderResponse.getUtkastCopy().getPatientPersonnummer());
        assertEquals(PATIENT_FNAME, builderResponse.getUtkastCopy().getPatientFornamn());
        assertNotNull(builderResponse.getUtkastCopy().getPatientMellannamn());
        assertEquals(PATIENT_LNAME, builderResponse.getUtkastCopy().getPatientEfternamn());
    }

    @Test
    public void testPopulateCopyUtkastFromSignedIntygWithNoPatientDetails() throws Exception {

        IntygContentHolder ich = createIntygContentHolder();
        when(mockIntygService.fetchIntygData(INTYG_ID, INTYG_TYPE, false)).thenReturn(ich);

        CreateRenewalCopyRequest renewalRequest = buildRenewalRequest();

        when(mockModuleApi.createNewInternalFromTemplate(any(CreateDraftCopyHolder.class), anyString())).thenReturn(INTYG_JSON);

        ValidateDraftResponse vdr = new ValidateDraftResponse(ValidationStatus.VALID, new ArrayList<>());
        when(mockModuleApi.validateDraft(anyString())).thenReturn(vdr);

        CopyUtkastBuilderResponse builderResponse = copyBuilder.populateCopyUtkastFromSignedIntyg(renewalRequest, null, false, false, false);

        assertNotNull(builderResponse.getUtkastCopy());
        assertNotNull(builderResponse.getUtkastCopy().getModel());
        assertEquals(INTYG_TYPE, builderResponse.getUtkastCopy().getIntygsTyp());
        assertEquals(PATIENT_SSN, builderResponse.getUtkastCopy().getPatientPersonnummer());
        assertEquals("Test", builderResponse.getUtkastCopy().getPatientFornamn());
        assertNull(builderResponse.getUtkastCopy().getPatientMellannamn());
        assertEquals("Testorsson", builderResponse.getUtkastCopy().getPatientEfternamn());
    }

    @Test
    public void testExtractNamePartsFromFullName() {

        String[] res = copyBuilder.extractNamePartsFromFullName(null);
        assertNotNull(res);

        res = copyBuilder.extractNamePartsFromFullName("");
        assertNotNull(res);

        res = copyBuilder.extractNamePartsFromFullName("  ");
        assertNotNull(res);
        assertEquals("", res[0]);
        assertEquals("", res[1]);

        res = copyBuilder.extractNamePartsFromFullName("Adam");
        assertNotNull(res);
        assertEquals("Adam", res[0]);
        assertEquals("", res[1]);

        res = copyBuilder.extractNamePartsFromFullName("Adam Caesarsson");
        assertNotNull(res);
        assertEquals("Adam", res[0]);
        assertEquals("Caesarsson", res[1]);

        res = copyBuilder.extractNamePartsFromFullName("Adam Bertil Caesarsson");
        assertNotNull(res);
        assertEquals("Adam Bertil", res[0]);
        assertEquals("Caesarsson", res[1]);
    }

    private CreateRenewalCopyRequest buildRenewalRequest() {
        return new CreateRenewalCopyRequest(INTYG_ID, INTYG_TYPE, patient, hoSPerson);
    }

    private IntygContentHolder createIntygContentHolder() throws Exception {
        List<Status> status = new ArrayList<>();
        status.add(new Status(CertificateState.RECEIVED, "HSVARD", LocalDateTime.now()));
        status.add(new Status(CertificateState.SENT, "FKASSA", LocalDateTime.now()));
        Fk7263Utlatande utlatande = new CustomObjectMapper().readValue(new ClassPathResource(
                "IntygDraftServiceImplTest/utlatande.json").getFile(), Fk7263Utlatande.class);
        return IntygContentHolder.builder()
                .setContents("<external-json/>")
                .setUtlatande(utlatande)
                .setStatuses(status)
                .setRevoked(false)
                .setRelations(new Relations())
                .setDeceased(false)
                .build();
    }

    private Utkast createOriginalUtkast() {

        Utkast orgUtkast = new Utkast();
        orgUtkast.setIntygsId(INTYG_COPY_ID);
        orgUtkast.setIntygsTyp(INTYG_TYPE);
        orgUtkast.setPatientPersonnummer(PATIENT_SSN);
        orgUtkast.setPatientFornamn(PATIENT_FNAME);
        orgUtkast.setPatientMellannamn(PATIENT_MNAME);
        orgUtkast.setPatientEfternamn(PATIENT_LNAME);
        orgUtkast.setEnhetsId(VARDENHET_ID);
        orgUtkast.setEnhetsNamn(VARDENHET_NAME);
        orgUtkast.setVardgivarId(VARDGIVARE_ID);
        orgUtkast.setVardgivarNamn(VARDGIVARE_NAME);
        orgUtkast.setModel(INTYG_JSON);

        VardpersonReferens vpRef = new VardpersonReferens();
        vpRef.setHsaId(HOSPERSON_ID);
        vpRef.setNamn(HOSPERSON_NAME);

        orgUtkast.setSenastSparadAv(vpRef);
        orgUtkast.setSkapadAv(vpRef);

        return orgUtkast;
    }

    private WebCertUser createWebcertUser() {
        WebCertUser user = new WebCertUser();
        user.setHsaId(HOSPERSON_ID);
        user.setNamn(HOSPERSON_NAME);
        se.inera.intyg.infra.integration.hsa.model.Vardgivare vGivare = new se.inera.intyg.infra.integration.hsa.model.Vardgivare();
        vGivare.setId(VARDGIVARE_ID);
        vGivare.setNamn(VARDENHET_NAME);
        user.setVardgivare(Arrays.asList(vGivare));
        AbstractVardenhet vardenhet = new se.inera.intyg.infra.integration.hsa.model.Vardenhet();
        vardenhet.setId(VARDENHET_ID);
        vardenhet.setNamn(VARDENHET_NAME);
        user.setValdVardenhet(vardenhet);
        user.setValdVardgivare(vGivare);
        return user;
    }
}
