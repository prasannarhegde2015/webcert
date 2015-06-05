package se.inera.webcert.hsa.services;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.riv.infrastructure.directory.employee.getemployee.v1.rivtabp21.GetEmployeeResponderInterface;
import se.riv.infrastructure.directory.employee.getemployeeresponder.v1.GetEmployeeResponseType;
import se.riv.infrastructure.directory.employee.getemployeeresponder.v1.GetEmployeeType;
import se.riv.infrastructure.directory.v1.PaTitleType;
import se.riv.infrastructure.directory.v1.PersonInformationType;
import se.riv.infrastructure.directory.v1.ResultCodeEnum;

import javax.xml.ws.WebServiceException;
import java.util.List;

/**
 * Created by Magnus Ekstrand on 01/06/15.
 */
@RunWith(MockitoJUnitRunner.class)
public class GetEmployeeServiceTest {

    private static final String LOGICAL_ADDRESS = "1234567890";

    private static final String VALID_HSA_ID = "SE11837399";
    private static final String INVALID_HSA_ID = "SE88888888";

    private static final String VALID_PERSONAL_ID = "19980101-9801";
    private static final String INVALID_PERSONAL_ID = "19990101-9901";

    private static final String BEFATTNING = "Överläkare";
    private static final String BEFATTNING_KOD = "201010";

    @Mock
    GetEmployeeResponderInterface responderService;

    @InjectMocks
    GetEmployeeServiceImpl employeeService;


    @Test
    public void whenValidHsaIdThenOk() {
        // When
        when(responderService.getEmployee(anyString(), any(GetEmployeeType.class))).thenReturn(buildOkResponse());
        // -- Make the call
        List<PersonInformationType> response = employeeService.getEmployeeInformation(LOGICAL_ADDRESS, VALID_HSA_ID, null);

        // Then
        assertTrue(response.size() > 0);
    }

    @Test(expected = WebServiceException.class)
    public void whenInvalidHsaIdThenExceptionThrown() {
        // When
        when(responderService.getEmployee(anyString(), any(GetEmployeeType.class))).thenReturn(buildInvalidHsaIdResponse());
        // -- Make the call
        employeeService.getEmployeeInformation(LOGICAL_ADDRESS, INVALID_HSA_ID, null);
    }

    @Test
    public void whenValidPersonalIdThenOk() {
        // When
        when(responderService.getEmployee(anyString(), any(GetEmployeeType.class))).thenReturn(buildOkResponse());
        // -- Make the call
        List<PersonInformationType> response = employeeService.getEmployeeInformation(LOGICAL_ADDRESS, null, VALID_PERSONAL_ID);

        // Then
        assertTrue(response.size() > 0);
    }

    @Test(expected = WebServiceException.class)
    public void whenInvalidPersonalIdThenExceptionThrown() {
        // When
        when(responderService.getEmployee(anyString(), any(GetEmployeeType.class))).thenReturn(buildInvalidPersonalIdResponse());
        // -- Make the call
        employeeService.getEmployeeInformation(LOGICAL_ADDRESS, null, INVALID_PERSONAL_ID);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenHsaIdAndPersonalIdIsNullThenExceptionThrown() {
        // -- Make the call
        employeeService.getEmployeeInformation(LOGICAL_ADDRESS, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenHsaIdAndPersonalIdIsSetThenExceptionThrown() {
        // -- Make the call
        employeeService.getEmployeeInformation(LOGICAL_ADDRESS, VALID_HSA_ID, VALID_PERSONAL_ID);
    }

    private GetEmployeeResponseType buildOkResponse() {
        GetEmployeeResponseType response = new GetEmployeeResponseType();
        response.getPersonInformation().add(buildPersonInformationType(VALID_HSA_ID, "Test", "Testorsson"));
        response.setResultCode(ResultCodeEnum.OK);

        return response;
    }

    private GetEmployeeResponseType buildInvalidHsaIdResponse() {
        GetEmployeeResponseType response = new GetEmployeeResponseType();
        response.setResultCode(ResultCodeEnum.ERROR);
        response.setResultText("Det går inte att hitta något personobjekt med angivet HSA-id: " + INVALID_HSA_ID);

        return response;
    }

    private GetEmployeeResponseType buildInvalidPersonalIdResponse() {
        GetEmployeeResponseType response = new GetEmployeeResponseType();
        response.setResultCode(ResultCodeEnum.ERROR);
        response.setResultText("Det går inte att hitta något personobjekt med angivet person-id: " + INVALID_PERSONAL_ID);

        return response;
    }

    private PersonInformationType buildPersonInformationType(String hsaId, String fName, String lName) {

        PaTitleType titleType = new PaTitleType();
        titleType.setPaTitleCode(BEFATTNING_KOD);
        titleType.setPaTitleName(BEFATTNING);

        PersonInformationType informationType = new PersonInformationType();
        informationType.setPersonHsaId(hsaId);
        informationType.setGivenName(fName);
        informationType.setMiddleAndSurName(lName);
        informationType.setTitle("En titel");
        informationType.setMail(fName.concat(".").concat(lName).concat("@landstinget.se"));
        informationType.getPaTitle().add(titleType);

        return informationType;
    }

}