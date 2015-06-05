package se.inera.webcert.hsa.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.inera.certificate.common.util.StringUtil;
import se.inera.certificate.logging.LogMarkers;
import se.riv.infrastructure.directory.employee.getemployee.v1.rivtabp21.GetEmployeeResponderInterface;
import se.riv.infrastructure.directory.employee.getemployeeresponder.v1.GetEmployeeResponseType;
import se.riv.infrastructure.directory.employee.getemployeeresponder.v1.GetEmployeeType;
import se.riv.infrastructure.directory.v1.PersonInformationType;
import se.riv.infrastructure.directory.v1.ResultCodeEnum;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;
import java.util.List;

/**
 * Created by Magnus Ekstrand on 28/05/15.
 */
public class GetEmployeeServiceImpl implements GetEmployeeService {

    private static final Logger LOG = LoggerFactory.getLogger(GetEmployeeServiceImpl.class);

    @Autowired
    private GetEmployeeResponderInterface service;

    /*
     * (non-Javadoc)
     *
     * @see se.inera.webcert.hsa.services.GetEmployeeService#getEmployeeInformation(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public List<PersonInformationType> getEmployeeInformation(String logicalAddress, String personHsaId, String personalIdentityNumber) throws WebServiceException {
        return getEmployeeInformation(logicalAddress, personHsaId, personalIdentityNumber, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see se.inera.webcert.hsa.services.GetEmployeeService#getEmployeeInformation(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public List<PersonInformationType> getEmployeeInformation(String logicalAddress, String personHsaId, String personalIdentityNumber, String searchBase) throws WebServiceException {

        // -- Argument-check
        // Exakt ett av fälten personHsaId och personalIdentityNumber ska anges.
        if (StringUtil.isNullOrEmpty(personHsaId) && StringUtil.isNullOrEmpty(personalIdentityNumber)) {
            throw new IllegalArgumentException("None of the arguments personHsaId and personalIdentityNumber is set. One of them must have a value.");
        }
        if (!StringUtil.isNullOrEmpty(personHsaId) && !StringUtil.isNullOrEmpty(personalIdentityNumber)) {
            throw new IllegalArgumentException("Only one of the arguments personHsaId and personalIdentityNumber can be set");
        }

        // -- Log which argument that is used to retrieve information
        if (StringUtil.isNullOrEmpty(personHsaId) && !StringUtil.isNullOrEmpty(personalIdentityNumber)) {
            LOG.debug(LogMarkers.HSA, "Getting employee information from HSA for person having HSA-id '{}'", personHsaId);
        }
        if (!StringUtil.isNullOrEmpty(personHsaId) && StringUtil.isNullOrEmpty(personalIdentityNumber)) {
            LOG.debug(LogMarkers.HSA, "Getting employee information from HSA for person having person-id '{}'", personalIdentityNumber);
        }

        GetEmployeeType employeeType = createEmployeeType(personHsaId, personalIdentityNumber, searchBase);
        GetEmployeeResponseType response = getEmployee(logicalAddress, employeeType);

        return response.getPersonInformation();
    }

    GetEmployeeResponseType getEmployee(String logicalAddress, GetEmployeeType employeeType) throws WebServiceException {

        GetEmployeeResponseType response;

        try {
            response = service.getEmployee(logicalAddress, employeeType);

            // check whether call was successful or not
            if (response.getResultCode() != ResultCodeEnum.OK) {
                LOG.error(LogMarkers.HSA, "Failed getting employee information from HSA");
                throw new WebServiceException(response.getResultText());
            }

            // The service contract declares that a valid response can have 0 (zero)
            // personInformationType object. This means that the request has found the employee
            // in question but there is no information about the employee (contradiction?)
            if (response.getPersonInformation().size() == 0) {
                LOG.warn(LogMarkers.HSA, "The employee exists in HSA but no user information was passed in the response.");
                throw new WebServiceException(response.getResultText());
            }
        } catch (SOAPFaultException e) {
            throw new WebServiceException(e);
        }

        return response;
    }

    private GetEmployeeType createEmployeeType(String personHsaId, String personalIdentityNumber, String searchBase) {
        GetEmployeeType employeeType = new GetEmployeeType();
        employeeType.setPersonHsaId(personHsaId);
        employeeType.setPersonalIdentityNumber(personalIdentityNumber);
        employeeType.setSearchBase(searchBase);

        return employeeType;
    }

}
