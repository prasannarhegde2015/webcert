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

package se.inera.intyg.webcert.web.web.controller.integrationtest.api;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;

import org.junit.Test;

import se.inera.intyg.webcert.web.auth.fake.FakeCredentials;
import se.inera.intyg.webcert.web.web.controller.integrationtest.BaseRestIntegrationTest;

import com.jayway.restassured.RestAssured;

/**
 * Basic testing of Fragasvar api endpoint. Main purpose is to validate that the endpoint is reachable and
 * responds according to json-schemas.
 */
public class FragaSvarApiControllerIT extends BaseRestIntegrationTest {

    protected static FakeCredentials LAKARE_MED_FRAGASVAR = new FakeCredentials.FakeCredentialsBuilder("eva", "Eva", "Holgersson",
            "centrum-vast").lakare(true).build();

    /**
     * Verify that no results are returned for a query that can not match anything
     */
    @Test
    public void testQueryFragaSvarNoResults() {
        RestAssured.sessionId = getAuthSession(LAKARE_MED_FRAGASVAR);

        given().param("hsaId", "finnsEj").expect().statusCode(200).
                when().
                get("api/fragasvar/sok").then().
                body(matchesJsonSchemaInClasspath("jsonschema/webcert-fragasvar-query-response-schema.json")).
                body("totalCount", equalTo(0));
    }

    /**
     * Verify that at least one of our bootstrapped fragasvar results are returned for a query
     * that should match match every bootstrapped fragasvar.
     */
    @Test
    public void testQueryFragaSvarAllResults() {

        RestAssured.sessionId = getAuthSession(LAKARE_MED_FRAGASVAR);

        given().expect().statusCode(200).
                when().
                get("api/fragasvar/sok").then().
                body(matchesJsonSchemaInClasspath("jsonschema/webcert-fragasvar-query-response-schema.json")).
                body("totalCount", greaterThan(0));
    }

    /**
     * Verify that at least the LAKARE_MED_FRAGASVAR is returned when querying for lakare that has fragasvar items
     * for a given unit (this is used to select valid hsaId parameter for the queryfilter in the fragasvar query gui).
     */
    @Test
    public void testQueryFragaSvarHsaIdsByEnhetsId() {

        RestAssured.sessionId = getAuthSession(LAKARE_MED_FRAGASVAR);

        given().param("enhetsId", LAKARE_MED_FRAGASVAR.getEnhetId()).expect().statusCode(200).
                when().
                get("api/fragasvar/lakare").then().
                body(matchesJsonSchemaInClasspath("jsonschema/webcert-fragasvar-get-lakare-med-fragasvar-response-schema.json")).
                body("", hasSize(greaterThan(0))).
                body("hsaId", hasItem(LAKARE_MED_FRAGASVAR.getHsaId()));
    }

}
