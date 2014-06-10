package se.inera.webcert.hsa.services;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import se.inera.ifv.hsawsresponder.v3.AddressType;
import se.inera.ifv.hsawsresponder.v3.AttributeListType;
import se.inera.ifv.hsawsresponder.v3.AttributeValueListType;
import se.inera.ifv.hsawsresponder.v3.AttributeValuePairType;
import se.inera.ifv.hsawsresponder.v3.CareUnitType;
import se.inera.ifv.hsawsresponder.v3.ExactType;
import se.inera.ifv.hsawsresponder.v3.GetCareUnitListResponseType;
import se.inera.ifv.hsawsresponder.v3.GetHsaUnitResponseType;
import se.inera.ifv.hsawsresponder.v3.GetMiuForPersonResponseType;
import se.inera.ifv.hsawsresponder.v3.GetMiuForPersonType;
import se.inera.ifv.hsawsresponder.v3.HsawsSimpleLookupResponseType;
import se.inera.ifv.hsawsresponder.v3.HsawsSimpleLookupType;
import se.inera.ifv.hsawsresponder.v3.LookupHsaObjectType;
import se.inera.ifv.hsawsresponder.v3.MiuInformationType;
import se.inera.ifv.hsawsresponder.v3.SearchOperatorExact;
import se.inera.ifv.webcert.spi.authorization.impl.HSAWebServiceCalls;
import se.inera.webcert.hsa.model.AbstractVardenhet;
import se.inera.webcert.hsa.model.Mottagning;
import se.inera.webcert.hsa.model.Vardenhet;
import se.inera.webcert.hsa.model.Vardgivare;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import se.inera.webcert.hsa.stub.Medarbetaruppdrag;

/**
 * @author andreaskaltenbach
 */
@Service
public class HsaOrganizationsServiceImpl implements HsaOrganizationsService {

    private static final Logger LOG = LoggerFactory.getLogger(HsaOrganizationsServiceImpl.class);

    @Autowired
    private HSAWebServiceCalls client;

    @Override
    public List<Vardgivare> getAuthorizedEnheterForHosPerson(String hosPersonHsaId) {
        List<Vardgivare> vardgivareList = new ArrayList<>();

        // Set hos person hsa ID
        GetMiuForPersonType parameters = new GetMiuForPersonType();
        parameters.setHsaIdentity(hosPersonHsaId);
        GetMiuForPersonResponseType response = client.callMiuRights(parameters);

        List<MiuInformationType> miuInformation = response.getMiuInformation();
        LOG.debug("User with HSA-Id '{}' has a total of {} medarbetaruppdrag", hosPersonHsaId, miuInformation.size());

        // filter by syfte. Only 'Vård och behandling' assignments are relevant for WebCert.
        Iterable<MiuInformationType> filteredMius = Iterables.filter(miuInformation,
                new Predicate<MiuInformationType>() {
                    @Override
                    public boolean apply(MiuInformationType miuInformationType) {
                        return Medarbetaruppdrag.VARD_OCH_BEHANDLING.equalsIgnoreCase(miuInformationType
                                .getMiuPurpose());
                    }
                });

        // group medarbetaruppdrag by vardgivare ID
        ImmutableListMultimap<String, MiuInformationType> vardgivareIdToMiuInformation = Multimaps.index(filteredMius,
                new Function<MiuInformationType, String>() {
                    @Override
                    public String apply(MiuInformationType miuInformation) {
                        return miuInformation.getCareGiver();
                    }
                });

        LOG.debug("User with HSA-Id '{}' has {} VoB medarbetaruppdrag for {} vårdgivare", new Object[]{hosPersonHsaId,
                vardgivareIdToMiuInformation.size(), vardgivareIdToMiuInformation.keySet().size()});

        for (String vardgivareId : vardgivareIdToMiuInformation.keySet()) {
            Vardgivare vardgivare = convert(vardgivareIdToMiuInformation.get(vardgivareId));
            if (vardgivare != null) {
                vardgivareList.add(vardgivare);
            }
        }

        return vardgivareList;
    }

    private List<Vardenhet> fetchAllEnheter(Vardgivare vardgivare, MiuInformationType miuInformationType) {

        List<Vardenhet> vardenheter = new ArrayList<>();

        String enhetHsaId = miuInformationType.getCareUnitHsaIdentity();

        String enhetDn = fetchDistinguishedName(enhetHsaId);
        LOG.debug("DN for enhet '{}' is '{}'", enhetHsaId, enhetDn);

        List<CareUnitType> careUnits = fetchSubEnheter(vardgivare, enhetDn);

        for (CareUnitType careUnit : careUnits) {
            Vardenhet vardenhet = new Vardenhet(careUnit.getHsaIdentity(), careUnit.getCareUnitName(),
                    careUnit.getCareUnitStartDate(), careUnit.getCareUnitEndDate());

            // only add enhet if it is currently active
            if (isActive(vardenhet.getStart(), vardenhet.getEnd())) {
                GetHsaUnitResponseType response = client.callGetHsaunit(careUnit.getHsaIdentity());
                vardenhet.setArbetsplatskod(getWorkplaceCode(vardenhet.getId()));
                updateWithContactInformation(vardenhet, response);
                attachMottagningar(vardenhet);
                vardenheter.add(vardenhet);
            } else {
                LOG.debug("Enhet '{}' is not active right now", enhetHsaId);
            }
        }

        return vardenheter;
    }

    private boolean isActive(LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDateTime now = new LocalDateTime();

        if (fromDate != null && now.isBefore(fromDate)) {
            return false;
        }
        if (toDate != null && now.isAfter(toDate)) {
            return false;
        }
        return true;
    }

    private List<CareUnitType> fetchSubEnheter(Vardgivare vardgivare, String enhetDn) {
        LookupHsaObjectType parameters = new LookupHsaObjectType();
        parameters.setHsaIdentity(vardgivare.getId());
        parameters.setSearchBase(enhetDn);
        GetCareUnitListResponseType response = client.callGetCareUnitList(parameters);
        LOG.debug("Enhet '{}' has {} enheter beneath", enhetDn, response.getCareUnits().getCareUnit().size());
        return response.getCareUnits().getCareUnit();
    }

    private String fetchDistinguishedName(String hsaId) {
        HsawsSimpleLookupType lookupType = new HsawsSimpleLookupType();
        ExactType exactType = new ExactType();
        exactType.setSearchAttribute("hsaIdentity");
        exactType.setSearchOperator(SearchOperatorExact.EXACT);
        exactType.setValue(hsaId);

        AttributeListType attributeList = new AttributeListType();
        attributeList.getAttribute().add("hsaIdentity");
        lookupType.setAttributes(attributeList);
        lookupType.setLookup(exactType);

        HsawsSimpleLookupResponseType lookupResponse = client.callHsawsSimpleLookup(lookupType);

        return lookupResponse.getResponseValues().get(0).getDN();
    }

    private void attachMottagningar(Vardenhet vardenhet) {
        List<String> mottagningsIds = fetchMottagningsHsaId(vardenhet);
        for (String mottagningsId : mottagningsIds) {
            Mottagning mottagning = fetchMottagning(mottagningsId);
            if (mottagning != null && isActive(mottagning.getStart(), mottagning.getEnd())) {
                mottagning.setArbetsplatskod(getWorkplaceCode(mottagning.getId()));
                vardenhet.getMottagningar().add(mottagning);
            } else {
                LOG.debug("Mottagning '{}' is not active right now", mottagningsId);
            }
        }
    }

    private String getWorkplaceCode(String careUnitHsaId) {
        LOG.debug("Calling HSA in getWorkplaceCode");

        HsawsSimpleLookupType parameters = new HsawsSimpleLookupType();

        ExactType lookupValue = new ExactType();
        lookupValue.setSearchAttribute("hsaIdentity");
        lookupValue.setSearchOperator(SearchOperatorExact.EXACT);
        lookupValue.setValue(careUnitHsaId);
        parameters.setLookup(lookupValue);

        AttributeListType attributes = new AttributeListType();
        attributes.getAttribute().add("unitPrescriptionCode");

        parameters.setAttributes(attributes);

        HsawsSimpleLookupResponseType response = client.callHsawsSimpleLookup(parameters);

        // Read response and pick values
        for (AttributeValueListType attValList : response.getResponseValues()) {
            for (AttributeValuePairType attValue : attValList.getResponse()) {
                if (attValue.getAttribute().equalsIgnoreCase("unitPrescriptionCode")) {
                    if (!attValue.getValue().isEmpty()) {
                        return attValue.getValue().get(0);
                    }
                }
            }
        }

        return "";
    }

    private Mottagning fetchMottagning(String mottagningsHsaId) {
        GetHsaUnitResponseType response = client.callGetHsaunit(mottagningsHsaId);
        LOG.debug("Fetching details for mottagning '{}'", mottagningsHsaId);
        if (response == null) {
            LOG.error("Mottagning '{}' hittades ej i HSA. Inkonsistent data i HSA", mottagningsHsaId);
            return null;
        } else {
            Mottagning mottagning = new Mottagning(response.getHsaIdentity(), response.getName(), response.getStartDate(), response.getEndDate());
            updateWithContactInformation(mottagning, response);
            return mottagning;
        }
    }

    private void updateWithContactInformation(AbstractVardenhet vardenhet, GetHsaUnitResponseType response) {
        vardenhet.setEpost(response.getEmail());
        if (response.getTelephoneNumbers() != null && !response.getTelephoneNumbers().getTelephoneNumber().isEmpty()) {
            vardenhet.setTelefonnummer(response.getTelephoneNumbers().getTelephoneNumber().get(0));
        }
        AddressType address = response.getPostalAddress();
        if (address == null) {
            return;
        }
        StringBuilder postaAddress = new StringBuilder();
        List<String> lines = address.getAddressLine();
        for (int i = 0; i < lines.size() - 1; i++) {
            postaAddress.append(lines.get(i).trim());
        }
        vardenhet.setPostadress(postaAddress.toString());
        String lastLine = lines.get(lines.size() - 1);
        if (lastLine.length() > 7 && Character.isDigit(lastLine.charAt(0))) {
            vardenhet.setPostnummer(lastLine.substring(0, 6).trim());
            vardenhet.setPostort(lastLine.substring(6).trim());
        } else {
            vardenhet.setPostnummer("XXXXX");
            vardenhet.setPostort(lastLine.trim());
        }
    }

    private List<String> fetchMottagningsHsaId(Vardenhet vardenhet) {
        List<String> ids = new ArrayList<>();

        HsawsSimpleLookupType lookupType = new HsawsSimpleLookupType();

        ExactType exactType = new ExactType();
        exactType.setSearchAttribute("hsaIdentity");
        exactType.setSearchOperator(SearchOperatorExact.EXACT);
        exactType.setValue(vardenhet.getId());

        AttributeListType attributeList = new AttributeListType();
        attributeList.getAttribute().add("hsaHealthCareUnitMember");
        lookupType.setAttributes(attributeList);
        lookupType.setSearchBase("c=SE");
        lookupType.setLookup(exactType);

        HsawsSimpleLookupResponseType lookupResponse = client.callHsawsSimpleLookup(lookupType);

        if (lookupResponse.getResponseValues().isEmpty()) {
            LOG.debug("Enhet '{}' has 0 mottagningar", vardenhet.getId());
            return ids;
        }

        List<AttributeValuePairType> attributes = lookupResponse.getResponseValues().get(0).getResponse();

        for (AttributeValuePairType attribute : attributes) {
            if ("hsaHealthCareUnitMember".equals(attribute.getAttribute())) {
                LOG.debug("Enhet '{}' has {} mottagningar", vardenhet.getId(), attribute.getValue().size());
                ids.addAll(attribute.getValue());
            }
        }

        return ids;
    }

    private Vardgivare convert(List<MiuInformationType> miuInformationTypes) {

        Vardgivare vardgivare = new Vardgivare(miuInformationTypes.get(0).getCareGiver(), miuInformationTypes.get(0).getCareGiverName());

        for (MiuInformationType miuInformationType : miuInformationTypes) {
            vardgivare.getVardenheter().addAll(fetchAllEnheter(vardgivare, miuInformationType));
        }

        if (vardgivare.getVardenheter().isEmpty()) {
            return null;
        }

        return vardgivare;
    }
}
