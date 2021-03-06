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
package se.inera.intyg.webcert.web.service.signatur.asn1;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 * Created by eriklupander on 2015-10-15.
 */
public class ASN1UtilTest {

    private static final String PERSON_ID = "197309069289";
    private static final String HSA_ID = "TSTNMT2321000156-1028";
    private static final String HSA_ID_2 = "TSTNMT2321000156-1025";

    private final ASN1UtilImpl asn1Util = new ASN1UtilImpl();

    @Test
    public void decodePersonIdFromASN1SigData() throws IOException {
        InputStream is = new ClassPathResource("netid-sig.txt").getInputStream();
        String value = asn1Util.getValue("2.5.4.5", is);
        is.close();
        assertEquals(PERSON_ID, value);
    }

    @Test
    public void decodeHsaIdFromASN1SigData() throws IOException {
        InputStream is = new ClassPathResource("netid-siths-sig.txt").getInputStream();
        String value = asn1Util.getValue("2.5.4.5", is);
        is.close();
        assertEquals(HSA_ID, value);
    }

    @Test
    public void decodeHsaIdFromASN1SigData2() throws IOException {
        InputStream is = new ClassPathResource("netid-siths-sig2.txt").getInputStream();
        String value = asn1Util.getValue("2.5.4.5", is);
        is.close();
        assertEquals(HSA_ID_2, value);
    }
}
