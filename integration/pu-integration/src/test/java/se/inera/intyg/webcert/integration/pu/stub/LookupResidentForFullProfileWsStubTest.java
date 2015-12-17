/*
 * Copyright (C) 2015 Inera AB (http://www.inera.se)
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

package se.inera.intyg.webcert.integration.pu.stub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import se.riv.population.residentmaster.lookupresidentforfullprofileresponder.v1.LookUpSpecificationType;
import se.riv.population.residentmaster.lookupresidentforfullprofileresponder.v1.LookupResidentForFullProfileResponseType;
import se.riv.population.residentmaster.lookupresidentforfullprofileresponder.v1.LookupResidentForFullProfileType;
import se.riv.population.residentmaster.lookupresidentforfullprofileresponder.v11.LookupResidentForFullProfileResponderInterface;
import se.riv.population.residentmaster.types.v1.PersonpostTYPE;
import se.riv.population.residentmaster.types.v1.ResidentType;

@RunWith(MockitoJUnitRunner.class)
public class LookupResidentForFullProfileWsStubTest {

    @Mock
    private ResidentStore residentStore;

    @InjectMocks
    private LookupResidentForFullProfileResponderInterface ws = new LookupResidentForFullProfileWsStub();

    @Test (expected = IllegalArgumentException.class)
    public void nullAddressThrowsException() {
        ws.lookupResidentForFullProfile(null, defaultRequest());
        fail();
    }

    @Test (expected = IllegalArgumentException.class)
    public void emptyAddressThrowsException() {
        ws.lookupResidentForFullProfile("", defaultRequest());
        fail();
    }

    @Test (expected = IllegalArgumentException.class)
    public void nullParametersThrowsException() {
        ws.lookupResidentForFullProfile("address", null);
        fail();
    }

    @Test (expected = IllegalArgumentException.class)
    public void noPersonIdThrowsException() {
        LookupResidentForFullProfileType parameters = defaultRequest();
        parameters.getPersonId().clear();
        ws.lookupResidentForFullProfile("address", parameters);
        fail();
    }

    @Test (expected = IllegalArgumentException.class)
    public void noLookupSpecificationThrowsException() {
        LookupResidentForFullProfileType parameters = defaultRequest();
        parameters.setLookUpSpecification(null);
        ws.lookupResidentForFullProfile("address", parameters);
        fail();
    }

    @Test
    public void personIdParametersReturned() {
        PersonpostTYPE person = new PersonpostTYPE();
        person.setPersonId("191212121212");
        ResidentType resident = new ResidentType();
        resident.setPersonpost(person);
        when(residentStore.get("191212121212")).thenReturn(resident);
        LookupResidentForFullProfileType parameters = defaultRequest();
        LookupResidentForFullProfileResponseType address = ws.lookupResidentForFullProfile("address", parameters);
        assertEquals(1, address.getResident().size());
        assertEquals("191212121212", address.getResident().get(0).getPersonpost().getPersonId());
    }

    private LookupResidentForFullProfileType defaultRequest() {
        LookupResidentForFullProfileType parameters = new LookupResidentForFullProfileType();
        parameters.getPersonId().add("191212121212");
        LookUpSpecificationType specification = new LookUpSpecificationType();
        parameters.setLookUpSpecification(specification);
        return parameters;
    }

}
