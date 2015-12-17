package se.inera.intyg.webcert.web.web.controller.authtestability;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import se.inera.intyg.webcert.web.auth.authorities.Role;
import se.inera.intyg.webcert.web.service.user.WebCertUserService;
import se.inera.intyg.webcert.web.service.user.dto.WebCertUser;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;

/**
 * Rest interface only used for testing and in dev environments. It seems like it must be in
 * the same Spring context as the rest of the webservices to get access to the security context.
 */
@Api(value = "user service", description = "REST API för testbarhet av användare", produces = MediaType.APPLICATION_JSON)
@Path("/")
public class UserResource {

    private static final Logger LOG = LoggerFactory.getLogger(UserResource.class);

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
    public Response setUserRole(@PathParam("role") Role newRole) {
        webCertUserService.updateUserRole(newRole.getName());
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

}