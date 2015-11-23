package se.inera.intyg.webcert.web.integration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import se.inera.webcert.persistence.utkast.model.Utkast;
import se.riv.clinicalprocess.healthcond.certificate.createdraftcertificateresponder.v1.CreateDraftCertificateResponderInterface;
import se.riv.clinicalprocess.healthcond.certificate.createdraftcertificateresponder.v1.CreateDraftCertificateResponseType;
import se.riv.clinicalprocess.healthcond.certificate.createdraftcertificateresponder.v1.CreateDraftCertificateType;
import se.riv.clinicalprocess.healthcond.certificate.createdraftcertificateresponder.v1.Utlatande;
import se.riv.clinicalprocess.healthcond.certificate.types.v1.UtlatandeId;
import se.riv.clinicalprocess.healthcond.certificate.v1.ErrorIdType;
import se.riv.clinicalprocess.healthcond.certificate.v1.ResultType;
import se.inera.ifv.hsawsresponder.v3.MiuInformationType;
import se.inera.intyg.common.schemas.clinicalprocess.healthcond.certificate.utils.ResultTypeUtil;
import se.inera.webcert.hsa.services.HsaPersonService;
import se.inera.intyg.webcert.web.integration.builder.CreateNewDraftRequestBuilder;
import se.inera.intyg.webcert.web.integration.registry.IntegreradeEnheterRegistry;
import se.inera.intyg.webcert.web.integration.registry.dto.IntegreradEnhetEntry;
import se.inera.intyg.webcert.web.integration.validator.CreateDraftCertificateValidator;
import se.inera.intyg.webcert.web.integration.validator.ResultValidator;
import se.inera.intyg.webcert.web.service.dto.Vardenhet;
import se.inera.intyg.webcert.web.service.dto.Vardgivare;
import se.inera.intyg.webcert.web.service.monitoring.MonitoringLogService;
import se.inera.intyg.webcert.web.service.utkast.UtkastService;
import se.inera.intyg.webcert.web.service.utkast.dto.CreateNewDraftRequest;

public class CreateDraftCertificateResponderImpl implements CreateDraftCertificateResponderInterface {

    private static final Logger LOG = LoggerFactory.getLogger(CreateDraftCertificateResponderImpl.class);

    @Autowired
    private UtkastService utkastService;

    @Autowired
    private HsaPersonService hsaPersonService;

    @Autowired
    private CreateNewDraftRequestBuilder draftRequestBuilder;

    @Autowired
    private CreateDraftCertificateValidator validator;

    @Autowired
    private IntegreradeEnheterRegistry integreradeEnheterRegistry;

    @Autowired
    private MonitoringLogService monitoringLogService;

    @Override
    public CreateDraftCertificateResponseType createDraftCertificate(String logicalAddress, CreateDraftCertificateType parameters) {

        Utlatande utkastsParams = parameters.getUtlatande();

        // Validate draft parameters
        ResultValidator resultsValidator = validator.validate(utkastsParams);
        if (resultsValidator.hasErrors()) {
            return createValidationErrorResponse(resultsValidator);
        }

        String invokingUserHsaId = utkastsParams.getSkapadAv().getPersonalId().getExtension();
        String invokingUnitHsaId = utkastsParams.getSkapadAv().getEnhet().getEnhetsId().getExtension();

        LOG.debug("Creating draft for invoker '{}' on unit '{}'", invokingUserHsaId, invokingUnitHsaId);

        // Check if the invoking health personal has MIU rights on care unit
        MiuInformationType unitMIU = checkMIU(utkastsParams);
        if (unitMIU == null) {
            return createMIUErrorResponse(utkastsParams);
        }

        // Create the draft
        Utkast utkast = createNewDraft(utkastsParams, unitMIU);

        return createSuccessResponse(utkast.getIntygsId());
    }

    private Utkast createNewDraft(Utlatande utlatandeRequest, MiuInformationType unitMIU) {

        String invokingUserHsaId = utlatandeRequest.getSkapadAv().getPersonalId().getExtension();
        String invokingUnitHsaId = utlatandeRequest.getSkapadAv().getEnhet().getEnhetsId().getExtension();
        LOG.debug("Creating draft for invoker '{}' on unit '{}'", invokingUserHsaId, invokingUnitHsaId);

        // Create draft request
        CreateNewDraftRequest draftRequest = draftRequestBuilder.buildCreateNewDraftRequest(utlatandeRequest, unitMIU);

        // Add the creating vardenhet to registry
        addVardenhetToRegistry(draftRequest);

        // Create draft and return its id
        return utkastService.createNewDraft(draftRequest);
    }

    /**
     * Method checks if invoking person, i.e the health care personal,
     * is entitled to look at the information.
     */
    private MiuInformationType checkMIU(Utlatande utlatandeType) {

        String invokingUserHsaId = utlatandeType.getSkapadAv().getPersonalId().getExtension();
        String invokingUnitHsaId = utlatandeType.getSkapadAv().getEnhet().getEnhetsId().getExtension();

        List<MiuInformationType> miusOnUnit = hsaPersonService.checkIfPersonHasMIUsOnUnit(invokingUserHsaId, invokingUnitHsaId);

        switch (miusOnUnit.size()) {
        case 0:
            return null;
        case 1:
            return miusOnUnit.get(0);
        default:
            LOG.warn("Found more than one MIU for user '{}' on unit '{}', returning the first one", invokingUserHsaId, invokingUnitHsaId);
            return miusOnUnit.get(0);
        }
    }

    /**
     * The response sent back to caller when an error is raised.
     */
    private CreateDraftCertificateResponseType createErrorResponse(String errorMsg, ErrorIdType errorType) {
        ResultType result = ResultTypeUtil.errorResult(errorType, errorMsg);

        CreateDraftCertificateResponseType response = new CreateDraftCertificateResponseType();
        response.setResult(result);
        return response;
    }

    /**
     * Builds a specific MIU error response.
     */
    private CreateDraftCertificateResponseType createMIUErrorResponse(Utlatande utlatandeType) {

        String invokingUserHsaId = utlatandeType.getSkapadAv().getPersonalId().getExtension();
        String invokingUnitHsaId = utlatandeType.getSkapadAv().getEnhet().getEnhetsId().getExtension();

        monitoringLogService.logMissingMedarbetarUppdrag(invokingUserHsaId, invokingUnitHsaId);

        String errMsg = String.format("No valid MIU was found for person %s on unit %s, can not create draft!", invokingUserHsaId, invokingUnitHsaId);
        return createErrorResponse(errMsg, ErrorIdType.VALIDATION_ERROR);
    }

    /**
     * Builds a specific validation error response.
     */
    private CreateDraftCertificateResponseType createValidationErrorResponse(ResultValidator resultsValidator) {
        String errMsgs = resultsValidator.getErrorMessagesAsString();
        LOG.warn("Utlatande did not validate correctly: {}", errMsgs);
        return createErrorResponse(errMsgs, ErrorIdType.VALIDATION_ERROR);
    }

    /**
     * The response sent back to caller when creating a certificate draft succeeded.
     */
    private CreateDraftCertificateResponseType createSuccessResponse(String nyttUtkastsId) {
        ResultType result = ResultTypeUtil.okResult();

        UtlatandeId utlId = new UtlatandeId();
        utlId.setRoot("utlatandeId");
        utlId.setExtension(nyttUtkastsId);

        CreateDraftCertificateResponseType response = new CreateDraftCertificateResponseType();
        response.setResult(result);
        response.setUtlatandeId(utlId);
        return response;
    }

    private void addVardenhetToRegistry(CreateNewDraftRequest utkastsRequest) {

        Vardenhet vardenhet = utkastsRequest.getVardenhet();
        Vardgivare vardgivare = vardenhet.getVardgivare();

        IntegreradEnhetEntry integreradEnhet = new IntegreradEnhetEntry(vardenhet.getHsaId(),
                vardenhet.getNamn(), vardgivare.getHsaId(), vardgivare.getNamn());

        boolean result = integreradeEnheterRegistry.addIfNotExistsIntegreradEnhet(integreradEnhet);

        if (result) {
            LOG.info("Added unit '{}' to registry of integrated units", vardenhet.getHsaId());
        } else {
            LOG.debug("Unit '{}' was alredy present in the registry of integrated units", vardenhet.getHsaId());
        }
    }

}
