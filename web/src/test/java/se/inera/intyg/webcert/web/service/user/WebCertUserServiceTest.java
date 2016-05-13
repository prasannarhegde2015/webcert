/*
 * Copyright (C) 2016 Inera AB (http://www.inera.se)
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

package se.inera.intyg.webcert.web.service.user;

import se.inera.intyg.webcert.web.auth.bootstrap.AuthoritiesConfigurationTestSetup;


//@RunWith(MockitoJUnitRunner.class)
public class WebCertUserServiceTest extends AuthoritiesConfigurationTestSetup {
   /*
    public static final String VARDGIVARE_1 = "VG1";
    public static final String VARDGIVARE_2 = "VG2";

    public static final String VARDENHET_1 = "VG1VE1";
    public static final String VARDENHET_2 = "VG1VE2";
    public static final String VARDENHET_3 = "VG2VE1";
    public static final String VARDENHET_4 = "VG2VE2";


    @InjectMocks
    public WebCertUserServiceImpl webcertUserService = new WebCertUserServiceImpl();

    @Test
    public void testCheckIfAuthorizedForUnit() {
        // anv inloggad på VE1 på VG1
        WebCertUser user = createWebCertUser(false);

        assertTrue("ska kunna titta på ett intyg inom VE1", webcertUserService.checkIfAuthorizedForUnit(user, VARDGIVARE_1, VARDENHET_1, true));
        assertFalse("ska INTE kunna titta på ett intyg inom VE2", webcertUserService.checkIfAuthorizedForUnit(user, VARDGIVARE_1, VARDENHET_2, true));
        assertTrue("ska kunna redigera ett intyg inom VE1", webcertUserService.checkIfAuthorizedForUnit(user, VARDGIVARE_1, VARDENHET_1, false));
        assertFalse("ska INTE kunna redigera ett intyg inom VE2",
                webcertUserService.checkIfAuthorizedForUnit(user, VARDGIVARE_1, VARDENHET_2, false));
    }

    @Test
    public void testCheckIfAuthorizedForUnitWhenIntegrated() {
        // anv i JS-läge inloggad på VE1 på VG1
        WebCertUser user = createWebCertUser(true);

        assertTrue("ska kunna titta på ett intyg inom VE1", webcertUserService.checkIfAuthorizedForUnit(user, VARDGIVARE_1, VARDENHET_1, true));
        assertTrue("ska kunna titta på ett intyg inom VE2", webcertUserService.checkIfAuthorizedForUnit(user, VARDGIVARE_1, VARDENHET_2, true));
        assertTrue("ska kunna redigera ett intyg inom VE1", webcertUserService.checkIfAuthorizedForUnit(user, VARDGIVARE_1, VARDENHET_1, false));
        assertFalse("ska INTE kunna redigera ett intyg inom VE2",
                webcertUserService.checkIfAuthorizedForUnit(user, VARDGIVARE_1, VARDENHET_2, false));
    }

    @Test
    public void testEnableFeatures() {

        WebCertUser user = createWebCertUser(false);

        assertEquals(0, user.getFeatures().size());

        webcertUserService.enableFeatures(user, WebcertFeature.HANTERA_FRAGOR, WebcertFeature.HANTERA_INTYGSUTKAST);

        assertEquals(2, user.getFeatures().size());
    }

    @Test
    public void testEnableModuleFeatures() {

        WebCertUser user = createWebCertUser(false);

        assertEquals(0, user.getFeatures().size());

        // base features must be enabled first
        webcertUserService.enableFeatures(user, WebcertFeature.HANTERA_FRAGOR, WebcertFeature.HANTERA_INTYGSUTKAST);

        assertEquals(2, user.getFeatures().size());

        webcertUserService.enableModuleFeatures(user, "fk7263", ModuleFeature.HANTERA_FRAGOR, ModuleFeature.HANTERA_INTYGSUTKAST);

        assertEquals(4, user.getFeatures().size());
    }

    private WebCertUser createWebCertUser(boolean fromJS) {

        WebCertUser user = createUser();

        user.setNamn("A Name");
        user.setHsaId("HSA-id");
        user.setForskrivarkod("Forskrivarkod");
        user.setAuthenticationScheme("AuthScheme");
        user.setSpecialiseringar(Arrays.asList("Kirurgi", "Ortopedi"));
        user.setBefattningar(Arrays.asList("Specialistläkare"));

        List<Vardgivare> vardgivare = new ArrayList<>();

        Vardgivare vg1 = new Vardgivare(VARDGIVARE_1, "Vardgivare 1");

        Vardenhet vg1ve1 = new Vardenhet(VARDENHET_1, "Vardenhet 1");
        vg1.getVardenheter().add(vg1ve1);
        vg1.getVardenheter().add(new Vardenhet(VARDENHET_2, "Vardenhet 2"));

        Vardgivare vg2 = new Vardgivare(VARDGIVARE_2, "Vardgivare 2");

        vg2.getVardenheter().add(new Vardenhet(VARDENHET_3, "Vardenhet 3"));
        vg2.getVardenheter().add(new Vardenhet(VARDENHET_4, "Vardenhet 4"));

        vardgivare.add(vg1);
        vardgivare.add(vg2);

        user.setVardgivare(vardgivare);

        user.setValdVardenhet(vg1ve1);
        user.setValdVardgivare(vg1);

        if (fromJS) {
            user.setOrigin(WebCertUserOriginType.DJUPINTEGRATION.name());
        } else {
            user.setOrigin(WebCertUserOriginType.NORMAL.name());
        }

        return user;
    }

    private WebCertUser createUser() {
        Role role = AUTHORITIES_RESOLVER.getRole(AuthoritiesConstants.ROLE_LAKARE);

        WebCertUser user = new WebCertUser();
        user.setRoles(AuthoritiesResolverUtil.toMap(role));
        user.setAuthorities(AuthoritiesResolverUtil.toMap(role.getPrivileges()));

        return user;
    }
        */
}
