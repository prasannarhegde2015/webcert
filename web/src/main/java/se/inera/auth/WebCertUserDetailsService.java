package se.inera.auth;

import static se.inera.webcert.hsa.stub.Medarbetaruppdrag.VARD_OCH_BEHANDLING;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import se.inera.auth.exceptions.HsaServiceException;
import se.inera.auth.exceptions.MissingMedarbetaruppdragException;
import se.inera.webcert.hsa.model.Befattning;
import se.inera.webcert.hsa.model.Vardenhet;
import se.inera.webcert.hsa.model.Vardgivare;
import se.inera.webcert.hsa.model.WebCertUser;
import se.inera.webcert.hsa.services.GetEmployeeService;
import se.inera.webcert.hsa.services.HsaOrganizationsService;
import se.inera.webcert.service.feature.WebcertFeatureService;
import se.inera.webcert.service.monitoring.MonitoringLogService;
import se.riv.infrastructure.directory.v1.PaTitleType;
import se.riv.infrastructure.directory.v1.PersonInformationType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author andreaskaltenbach
 */
public class WebCertUserDetailsService implements SAMLUserDetailsService {

    private static final Logger LOG = LoggerFactory.getLogger(WebCertUserDetailsService.class);

    private static final String COMMA = ", ";
    private static final String SPACE = " ";

    private static final String LAKARE = "Läkare";
    private static final String LAKARE_CODE = "204010";

    @Value("${hsa.ws.service.logicaladdress}")
    private String hsaLogicalAddress;

    @Autowired
    private HsaOrganizationsService hsaOrganizationsService;

/*
    @Autowired
    private HsaPersonService hsaPersonService;
*/

    @Autowired
    private GetEmployeeService getEmployeeService;

    @Autowired
    private WebcertFeatureService webcertFeatureService;

    @Autowired
    private MonitoringLogService monitoringLogService;

    @Override
    public Object loadUserBySAML(SAMLCredential credential) {

        LOG.info("User authentication was successful. SAML credential is: {}", credential);

        SakerhetstjanstAssertion assertion = new SakerhetstjanstAssertion(credential.getAuthenticationAssertion());

        try {
            WebCertUser webCertUser = createWebCertUser(assertion);

            // if user has authenticated with other contract than 'Vård och behandling', we have to reject her
            if (!VARD_OCH_BEHANDLING.equals(assertion.getMedarbetaruppdragType())) {
                throw new MissingMedarbetaruppdragException(webCertUser.getHsaId());
            }

            List<Vardgivare> authorizedVardgivare = hsaOrganizationsService.getAuthorizedEnheterForHosPerson(webCertUser.getHsaId());

            // if user does not have access to any vardgivare, we have to reject authentication
            if (authorizedVardgivare.isEmpty()) {
                throw new MissingMedarbetaruppdragException(webCertUser.getHsaId());
            }

            webCertUser.setVardgivare(authorizedVardgivare);
            setDefaultSelectedVardenhetOnUser(webCertUser, assertion);

            return webCertUser;
        } catch (MissingMedarbetaruppdragException e) {
            monitoringLogService.logMissingMedarbetarUppdrag(assertion.getHsaId());
            throw e;
        } catch (Exception e) {
            LOG.error("Error building user {}, failed with message {}", assertion.getHsaId(), e.getMessage());
            throw new HsaServiceException(assertion.getHsaId(), e);
        }
    }

    private WebCertUser createWebCertUser(SakerhetstjanstAssertion assertion) {

        WebCertUser webcertUser = new WebCertUser();

        webcertUser.setHsaId(assertion.getHsaId());
        webcertUser.setNamn(compileName(assertion));
        webcertUser.setForskrivarkod(assertion.getForskrivarkod());
        webcertUser.setAuthenticationScheme(assertion.getAuthenticationScheme());

        // lakare flag is calculated by checking for lakare profession in title and title code
        webcertUser.setLakare(assertion.getTitel().contains(LAKARE) || assertion.getTitelKod().contains(LAKARE_CODE));

        decorateWebCertUserWithAdditionalInfo(webcertUser);
        decorateWebCertUserWithAvailableFeatures(webcertUser);

        return webcertUser;
    }

    private void decorateWebCertUserWithAdditionalInfo(WebCertUser webcertUser) {

        String hsaId = webcertUser.getHsaId();

        List<PersonInformationType> employeeInfo = getEmployeeService.getEmployeeInformation(hsaLogicalAddress, hsaId, null);

        if (employeeInfo == null || employeeInfo.isEmpty()) {
            LOG.info("getEmployeeInformation did not return any info for employee '{}'", hsaId);
            return;
        }

        String titel = extractTitel(employeeInfo);
        webcertUser.setTitel(titel);

        List<Befattning> befattningar = extractBefattningar(employeeInfo);
        webcertUser.setBefattningar(befattningar);

        List<String> specialiseringar = extractSpecialiseringar(employeeInfo);
        webcertUser.setSpecialiseringar(specialiseringar);

        List<String> legitimeradeYrkesgrupper = extractLegitimeradeYrkesgrupper(employeeInfo);
        webcertUser.setLegitimeradeYrkesgrupper(legitimeradeYrkesgrupper);

    }

    private List<Befattning> extractBefattningar(List<PersonInformationType> informationTypes) {

        Set<Befattning> befattningar = new TreeSet<>();

        for (PersonInformationType informationType : informationTypes) {
            if (informationType.getPaTitle() != null) {
                befattningar.addAll(mapPaTitles(informationType.getPaTitle()));
            }
        }

        return new ArrayList<Befattning>(befattningar);
    }

    private List<Befattning> mapPaTitles(List<PaTitleType> types) {
        List<Befattning> befattningar = new ArrayList<Befattning>();

        for (PaTitleType type : types) {
            befattningar.add(createBefattning(type));
        }

        return befattningar;
    }

    private Befattning createBefattning(PaTitleType titleType) {
        Befattning b = new Befattning();
        b.setCode(titleType.getPaTitleCode());
        b.setDescription(titleType.getPaTitleName());

        return b;
    }

    private void decorateWebCertUserWithAvailableFeatures(WebCertUser webcertUser) {

        Set<String> availableFeatures = webcertFeatureService.getActiveFeatures();

        webcertUser.setAktivaFunktioner(availableFeatures);
    }

    private String extractTitel(List<PersonInformationType> informationTypes) {

        List<String> titlar = new ArrayList<String>();

        for (PersonInformationType type : informationTypes) {
            if (StringUtils.isNotBlank(type.getTitle())) {
                titlar.add(type.getTitle());
            }
        }

        return StringUtils.join(titlar, COMMA);
    }

/*
    private String extractTitel(List<GetHsaPersonHsaUserType> hsaUserTypes) {

        List<String> titlar = new ArrayList<String>();

        for (GetHsaPersonHsaUserType userType : hsaUserTypes) {
            if (StringUtils.isNotBlank(userType.getTitle())) {
                titlar.add(userType.getTitle());
            }
        }

        return StringUtils.join(titlar, COMMA);
    }
*/

    private List<String> extractLegitimeradeYrkesgrupper(List<PersonInformationType> informationTypes) {

        Set<String> lygSet = new TreeSet<>();

        for (PersonInformationType type : informationTypes) {
            if (type.getHealthCareProfessionalLicence() != null) {
                lygSet.addAll(type.getHealthCareProfessionalLicence());
            }
        }

        return new ArrayList<String>(lygSet);
    }

/*
    private List<String> extractLegitimeradeYrkesgrupper(List<GetHsaPersonHsaUserType> hsaUserTypes) {

        Set<String> lygSet = new TreeSet<>();

        for (GetHsaPersonHsaUserType userType : hsaUserTypes) {
            if (userType.getHsaTitles() != null) {
                List<String> hsaTitles = userType.getHsaTitles().getHsaTitle();
                lygSet.addAll(hsaTitles);
            }
        }

        List<String> list = new ArrayList<String>(lygSet);

        return list;
    }
*/

    private List<String> extractSpecialiseringar(List<PersonInformationType> informationTypes) {

        Set<String> specSet = new TreeSet<>();

        for (PersonInformationType type : informationTypes) {
            if (type.getSpecialityName() != null) {
                specSet.addAll(type.getSpecialityName());
            }
        }

        return new ArrayList<String>(specSet);
    }

/*
    private List<String> extractSpecialiseringar(List<GetHsaPersonHsaUserType> hsaUserTypes) {

        Set<String> specSet = new TreeSet<>();

        for (GetHsaPersonHsaUserType userType : hsaUserTypes) {
            if (userType.getSpecialityNames() != null) {
                List<String> specialityNames = userType.getSpecialityNames().getSpecialityName();
                specSet.addAll(specialityNames);
            }
        }

        List<String> list = new ArrayList<String>(specSet);

        return list;
    }
*/

    private String compileName(SakerhetstjanstAssertion assertion) {

        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotBlank(assertion.getFornamn())) {
            sb.append(assertion.getFornamn());
        }

        if (StringUtils.isNotBlank(assertion.getMellanOchEfternamn())) {
            if (sb.length() > 0) {
                sb.append(SPACE);
            }
            sb.append(assertion.getMellanOchEfternamn());
        }

        return sb.toString();
    }

    private void setDefaultSelectedVardenhetOnUser(WebCertUser user, SakerhetstjanstAssertion assertion) {

        // Get HSA id for the selected MIU
        String medarbetaruppdragHsaId = assertion.getEnhetHsaId();

        boolean changeSuccess;

        if (StringUtils.isNotBlank(medarbetaruppdragHsaId)) {
            changeSuccess = user.changeValdVardenhet(medarbetaruppdragHsaId);
        } else {
            LOG.error("Assertion did not contain a medarbetaruppdrag, defaulting to use one of the Vardenheter present in the user");
            changeSuccess = setFirstVardenhetOnFirstVardgivareAsDefault(user);
        }

        if (!changeSuccess) {
            LOG.error("When logging in user '{}', unit with HSA-id {} could not be found in users MIUs", user.getHsaId(), medarbetaruppdragHsaId);
            throw new MissingMedarbetaruppdragException(user.getHsaId());
        }

        LOG.debug("Setting care unit '{}' as default unit on user '{}'", user.getValdVardenhet().getId(), user.getHsaId());
    }

    private boolean setFirstVardenhetOnFirstVardgivareAsDefault(WebCertUser user) {

        Vardgivare firstVardgivare = user.getVardgivare().get(0);
        user.setValdVardgivare(firstVardgivare);

        Vardenhet firstVardenhet = firstVardgivare.getVardenheter().get(0);
        user.setValdVardenhet(firstVardenhet);

        return true;
    }

}