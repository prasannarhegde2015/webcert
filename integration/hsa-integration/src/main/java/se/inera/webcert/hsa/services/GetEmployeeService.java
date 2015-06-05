package se.inera.webcert.hsa.services;

import java.util.List;

import javax.xml.ws.WebServiceException;

import se.riv.infrastructure.directory.employee.getemployeeresponder.v1.GetEmployeeResponseType;
import se.riv.infrastructure.directory.v1.PersonInformationType;

/**
 * Created by Magnus Ekstrand on 27/05/15.
 */
public interface GetEmployeeService {

    /**
     * Returnerar information, som kontaktinformation samt legitimerad yrkesgrupp och specialitet, för sökt person.
     * Exakt ett av fälten personHsaId och personalIdentityNumber ska anges.
     *
     * @param logicalAddress Mottagande systems logiska adress.
     * @param personHsaId Sökt persons HSA-id.
     * @param personalIdentityNumber Sökt persons Person-id (personnummer eller samordningsnummer).
     *
     * @return Information om sökt person. Om personen har flera person-objekt returneras en instans per objekt.
     *
     * @throws WebServiceException
     */
    List<PersonInformationType> getEmployeeInformation(String logicalAddress, String personHsaId, String personalIdentityNumber) throws WebServiceException;

    /**
     * Returnerar information, som kontaktinformation samt legitimerad yrkesgrupp och specialitet, för sökt person.
     * Exakt ett av fälten personHsaId och personalIdentityNumber ska anges.
     *
     * @param logicalAddress Mottagande systems logiska adress.
     * @param personHsaId Sökt persons HSA-id.
     * @param personalIdentityNumber Sökt persons Person-id (personnummer eller samordningsnummer).
     * @param searchBase Sökbas. Om ingen sökbas anges används c=SE som sökbas.
     *
     * @return Information om sökt person. Om personen har flera person-objekt returneras en instans per objekt.
     *
     * @throws WebServiceException
     */
    List<PersonInformationType> getEmployeeInformation(String logicalAddress, String personHsaId, String personalIdentityNumber, String searchBase) throws WebServiceException;

}
