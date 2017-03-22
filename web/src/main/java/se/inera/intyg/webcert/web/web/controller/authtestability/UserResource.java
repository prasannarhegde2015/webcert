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
package se.inera.intyg.webcert.web.web.controller.authtestability;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.swagger.annotations.Api;
import se.inera.intyg.infra.security.common.model.Role;
import se.inera.intyg.webcert.web.service.user.WebCertUserService;
import se.inera.intyg.webcert.web.service.user.dto.IntegrationParameters;
import se.inera.intyg.webcert.web.service.user.dto.WebCertUser;

/**
 * Rest interface only used for testing and in dev environments. It seems like it must be in
 * the same Spring context as the rest of the webservices to get access to the security context.
 */
@Api(value = "user service", description = "REST API för testbarhet av användare", produces = MediaType.APPLICATION_JSON)
@Path("/")
public class UserResource {

    @Autowired
    private WebCertUserService webCertUserService;

    @GET
    @Path("/role")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPropertyDescription("Get the roles for user in session")
    public Response getUserRoles() {
        final WebCertUser user = webCertUserService.getUser();
        final Map<String, Role> roles = user.getRoles();
        final Set<String> roleStrings = roles.keySet();
        return Response.ok(roleStrings).build();
    }

    /**
     * Set the role for current user. Using a GET to change a state might not be
     * recommended. However, it is a very convenient way to change the user role
     * from the browser and it is also the only way I could figure out to invoke
     * it from the browser session in the Fitnesse tests.
     */
    @GET
    @Path("/role/{role}")
    @Produces(MediaType.APPLICATION_JSON)
    @JsonPropertyDescription("Set the roles for user in session")
    public Response setUserRole(@PathParam("role") String role) {
        webCertUserService.updateUserRole(role);
        return Response.ok().build();
    }

    @GET
    @Path("/origin")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrigin() {
        final WebCertUser user = webCertUserService.getUser();
        final String currentOrigin = user.getOrigin();
        return Response.ok(currentOrigin).build();
    }

    /**
     * Set current user's request origin. Using a GET to change a state might not be
     * recommended. However, it is a very convenient way to change the request origin
     * from the browser and it is also the only way I could figure out to invoke
     * it from the browser session in the Fitnesse tests.
     */
    @GET
    @Path("/origin/{origin}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setOrigin(@PathParam("origin") String origin) {
        webCertUserService.updateOrigin(origin);
        return Response.ok().build();
    }

    @GET
    @Path("/preferences/delete")
    public Response deleteUserPreferences() {
        webCertUserService.deleteUserPreferences();
        return Response.ok().build();
    }

    @GET
    @Path("/parameters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getParameters() {
        return Response.ok(webCertUserService.getUser().getParameters()).build();
    }

    @POST
    @Path("/parameters/sjf")
    public Response setSjf() {
        webCertUserService.getUser()
                .setParameters(new IntegrationParameters(null, null, null, null, null, null, null, null, null, true, false, false));
        return Response.ok().build();
    }
}
