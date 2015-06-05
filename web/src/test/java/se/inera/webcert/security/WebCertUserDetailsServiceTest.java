package se.inera.webcert.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cxf.helpers.XMLUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.NameID;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.saml.SAMLCredential;
import org.w3c.dom.Document;
import se.inera.auth.WebCertUserDetailsService;
import se.inera.auth.exceptions.HsaServiceException;
import se.inera.auth.exceptions.MissingMedarbetaruppdragException;
import se.inera.webcert.hsa.model.Vardenhet;
import se.inera.webcert.hsa.model.Vardgivare;
import se.inera.webcert.hsa.model.WebCertUser;
import se.inera.webcert.hsa.services.GetEmployeeService;
import se.inera.webcert.hsa.services.HsaOrganizationsService;
import se.inera.webcert.service.feature.WebcertFeatureService;
import se.inera.webcert.service.monitoring.MonitoringLogService;
import se.riv.infrastructure.directory.v1.PaTitleType;
import se.riv.infrastructure.directory.v1.PersonInformationType;

import javax.xml.transform.stream.StreamSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author andreaskaltenbach
 */
@RunWith(MockitoJUnitRunner.class)
public class WebCertUserDetailsServiceTest {

    private static final String PERSONAL_HSA_ID = "TST5565594230-106J";
    private static final String ENHET_HSA_ID = "IFV1239877878-103H";
    private static final String FIRST_NAME = "Markus";
    private static final String LAST_NAME = "Gran";
    private static final String TITLE = "Överläkare";
    private static final String BEFATTNING = "Överläkare";
    private static final String BEFATTNING_KOD = "201010";

    private Vardgivare vardgivare;

    @Value("${hsa.ws.service.logicaladdress}")
    private String hsaLogicalAddress;

    @InjectMocks
    private WebCertUserDetailsService userDetailsService = new WebCertUserDetailsService();

    @Mock
    private HsaOrganizationsService hsaOrganizationsService;

//    @Mock
//    private HsaPersonService hsaPersonService;

    @Mock
    private GetEmployeeService getEmployeeService;

    @Mock
    private WebcertFeatureService webcertFeatureService;

    @Mock
    private MonitoringLogService monitoringLogService;

    @BeforeClass
    public static void bootstrapOpenSaml() throws Exception {
        DefaultBootstrap.bootstrap();
    }

    @Test
    public void testPopulatingWebCertUser() throws Exception {

        setupCallToAuthorizedEnheterForHosPerson();
        //setupCallToGetHsaPersonInfo();
        setupCallToGetEmployee();
        setupCallToWebcertFeatureService();

        SAMLCredential samlCredential = createSamlCredential("saml-assertion-with-title-code-lakare.xml");

        WebCertUser webCertUser = (WebCertUser) userDetailsService.loadUserBySAML(samlCredential);

        assertEquals(PERSONAL_HSA_ID, webCertUser.getHsaId());
        assertEquals(getUserFullName(), webCertUser.getNamn());
        assertEquals(1, webCertUser.getVardgivare().size());
        assertEquals("vg", webCertUser.getVardgivare().get(0).getId());
        assertEquals(vardgivare, webCertUser.getVardgivare().get(0));
        assertEquals(vardgivare, webCertUser.getValdVardgivare());
        assertNotNull(webCertUser.getValdVardenhet());
        assertEquals(ENHET_HSA_ID, webCertUser.getValdVardenhet().getId());
        assertEquals(3, webCertUser.getSpecialiseringar().size());
        assertEquals(2, webCertUser.getLegitimeradeYrkesgrupper().size());
        assertEquals(TITLE, webCertUser.getTitel());
        assertFalse(webCertUser.getAktivaFunktioner().isEmpty());

        verify(hsaOrganizationsService).getAuthorizedEnheterForHosPerson(PERSONAL_HSA_ID);
        verify(getEmployeeService).getEmployeeInformation(hsaLogicalAddress, PERSONAL_HSA_ID, null);
        verify(webcertFeatureService).getActiveFeatures();
    }

    @Test
    public void testPopulatingWebCertUserWithTwoUserTypes() throws Exception {

        // Given
        setupCallToAuthorizedEnheterForHosPerson();

        List<String> userType1Licenses = Arrays.asList("Läkare");
        List<String> userType2Licenses = Arrays.asList("Psykoterapeut");
        List<String> userType1Specialities = Arrays.asList("Kirurgi", "Öron-, näs- och halssjukdomar");
        List<String> userType2Specialities = Arrays.asList("Kirurgi", "Reumatologi");

        PersonInformationType userType1 = buildPersonInformationType(PERSONAL_HSA_ID, FIRST_NAME, LAST_NAME,
                userType1Licenses, userType1Specialities);
        PersonInformationType userType2 = buildPersonInformationType(PERSONAL_HSA_ID, FIRST_NAME, LAST_NAME,
                userType2Licenses, userType2Specialities);

        List<PersonInformationType> userTypes = Arrays.asList(userType1, userType2);

        // When
        when(getEmployeeService.getEmployeeInformation(hsaLogicalAddress, PERSONAL_HSA_ID, null)).thenReturn(userTypes);

        // -- make the call
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-with-title-code-lakare.xml");
        WebCertUser webCertUser = (WebCertUser) userDetailsService.loadUserBySAML(samlCredential);

        // Then
        assertEquals(PERSONAL_HSA_ID, webCertUser.getHsaId());
        assertEquals(getUserFullName(), webCertUser.getNamn());
        assertEquals(3, webCertUser.getSpecialiseringar().size());
        assertEquals(2, webCertUser.getLegitimeradeYrkesgrupper().size());
        assertEquals(TITLE + ", " + TITLE, webCertUser.getTitel());

        verify(hsaOrganizationsService).getAuthorizedEnheterForHosPerson(PERSONAL_HSA_ID);
        verify(getEmployeeService).getEmployeeInformation(hsaLogicalAddress, PERSONAL_HSA_ID, null);
    }
    
    private void setupCallToAuthorizedEnheterForHosPerson() {
        vardgivare = new Vardgivare("vg", "Landstinget Ingenmansland");
        vardgivare.getVardenheter().add(new Vardenhet("vardcentralen", "Vårdcentralen"));
        vardgivare.getVardenheter().add(new Vardenhet(ENHET_HSA_ID, "TestVårdEnhet2A VårdEnhet2A"));

        List<Vardgivare> vardgivareList = Collections.singletonList(vardgivare);

        when(hsaOrganizationsService.getAuthorizedEnheterForHosPerson(PERSONAL_HSA_ID)).thenReturn(
                vardgivareList);
    }

    private void setupCallToGetEmployee() {

        List<String> licenses = Arrays.asList("Läkare", "Psykoterapeut");
        List<String> specialities = Arrays.asList("Kirurgi", "Öron-, näs- och halssjukdomar", "Reumatologi");

        List<PersonInformationType> userTypes = Arrays.asList(buildPersonInformationType(PERSONAL_HSA_ID,
                FIRST_NAME, LAST_NAME, licenses, specialities));

        when(getEmployeeService.getEmployeeInformation(hsaLogicalAddress, PERSONAL_HSA_ID, null)).thenReturn(userTypes);
    }

    private void setupCallToWebcertFeatureService() {
        Set<String> availableFeatures = new TreeSet<String>();
        availableFeatures.add("feature1");
        availableFeatures.add("feature2");

        when(webcertFeatureService.getActiveFeatures()).thenReturn(availableFeatures);
    }
    
    @Test
    public void testLakareTitle() throws Exception {
        setupCallToAuthorizedEnheterForHosPerson();
        setupCallToGetEmployee();
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-with-title-lakare.xml");
        WebCertUser webCertUser = (WebCertUser) userDetailsService.loadUserBySAML(samlCredential);
        assertTrue(webCertUser.isLakare());
    }

    @Test
    public void testLakareTitleCode() throws Exception {
        setupCallToAuthorizedEnheterForHosPerson();
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-with-title-code-lakare.xml");
        WebCertUser webCertUser = (WebCertUser) userDetailsService.loadUserBySAML(samlCredential);
        assertTrue(webCertUser.isLakare());
    }

    @Test
    public void testNoLakare() throws Exception {
        setupCallToAuthorizedEnheterForHosPerson();
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-no-lakare.xml");
        WebCertUser webCertUser = (WebCertUser) userDetailsService.loadUserBySAML(samlCredential);
        assertFalse(webCertUser.isLakare());
    }

    @Test
    public void testNoGivenName() throws Exception {
        setupCallToAuthorizedEnheterForHosPerson();
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-no-givenname.xml");
        WebCertUser webCertUser = (WebCertUser) userDetailsService.loadUserBySAML(samlCredential);
        assertEquals(LAST_NAME, webCertUser.getNamn());
    }

    @Test
    public void testMultipleTitleCodes() throws Exception {
        setupCallToAuthorizedEnheterForHosPerson();
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-with-multiple-title-codes.xml");
        WebCertUser webCertUser = (WebCertUser) userDetailsService.loadUserBySAML(samlCredential);
        assertTrue(webCertUser.isLakare());
    }

    @Test
    public void testMultipleTitles() throws Exception {
        setupCallToAuthorizedEnheterForHosPerson();
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-with-multiple-titles.xml");
        WebCertUser webCertUser = (WebCertUser) userDetailsService.loadUserBySAML(samlCredential);
        assertTrue(webCertUser.isLakare());
    }

    @Test(expected = MissingMedarbetaruppdragException.class)
    public void testMissingMedarbetaruppdrag() throws Exception {
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-no-lakare.xml");
        userDetailsService.loadUserBySAML(samlCredential);
    }

    @Test(expected = MissingMedarbetaruppdragException.class)
    public void testMissingSelectedUnit() throws Exception {
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-without-enhet.xml");
        userDetailsService.loadUserBySAML(samlCredential);
    }

    @Test(expected = HsaServiceException.class)
    public void unexpectedExceptionWhenprocessingData() throws Exception {
        SAMLCredential samlCredential = createSamlCredential("saml-assertion-with-title-lakare.xml");
        when(hsaOrganizationsService.getAuthorizedEnheterForHosPerson(anyString())).thenThrow(new NullPointerException());
        userDetailsService.loadUserBySAML(samlCredential);
        fail("Expected exception");
    }

    private SAMLCredential createSamlCredential(String filename) throws Exception {
        Document doc = (Document) XMLUtils.fromSource(new StreamSource(new ClassPathResource(
                "WebCertUserDetailsServiceTest/" + filename).getInputStream()));
        UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(Assertion.DEFAULT_ELEMENT_NAME);

        Assertion assertion = (Assertion) unmarshaller.unmarshall(doc.getDocumentElement());
        NameID nameId = assertion.getSubject().getNameID();
        return new SAMLCredential(nameId, assertion, "remoteId", "localId");
    }

    private PersonInformationType buildPersonInformationType(String hsaId, String fName, String lName, List<String> licenses, List<String> specialities) {

        PaTitleType titleType = new PaTitleType();
        titleType.setPaTitleCode(BEFATTNING_KOD);
        titleType.setPaTitleName(BEFATTNING);

        PersonInformationType informationType = new PersonInformationType();
        informationType.setPersonHsaId(hsaId);
        informationType.setGivenName(fName);
        informationType.setMiddleAndSurName(lName);
        informationType.setTitle(TITLE);
        informationType.setMail(fName.concat(".").concat(lName).concat("@landstinget.se"));
        informationType.getPaTitle().add(titleType);
        informationType.getHealthCareProfessionalLicence().addAll(licenses);
        informationType.getSpecialityName().addAll(specialities);

        return informationType;
    }

    private String getUserFullName() {
        return FIRST_NAME + " " + LAST_NAME;
    }

}
