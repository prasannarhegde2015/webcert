package se.inera.auth.eleg;

import static se.inera.auth.SakerhetstjanstAssertion.*;

import java.util.ArrayList;

import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.NameID;
import org.opensaml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml2.core.impl.AttributeStatementBuilder;
import org.opensaml.saml2.core.impl.NameIDBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.security.saml.SAMLCredential;

import se.inera.auth.BaseFakeAuthenticationProvider;

/**
 * Created by eriklupander on 2015-06-16.
 */
public class FakeElegAuthenticationProvider extends BaseFakeAuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        FakeElegAuthenticationToken token = (FakeElegAuthenticationToken) authentication;

        SAMLCredential credential = createSamlCredential(token);
        Object details = userDetails.loadUserBySAML(credential);

        ExpiringUsernameAuthenticationToken result = new ExpiringUsernameAuthenticationToken(null, details, credential,
                new ArrayList<GrantedAuthority>());
        result.setDetails(details);

        return result;
    }

    @Override
    public boolean supports(Class authentication) {
        return FakeElegAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private SAMLCredential createSamlCredential(FakeElegAuthenticationToken token) {
        FakeElegCredentials fakeCredentials = (FakeElegCredentials) token.getCredentials();

        Assertion assertion = new AssertionBuilder().buildObject();

        attachAuthenticationContext(assertion);

        AttributeStatement attributeStatement = new AttributeStatementBuilder().buildObject();
        assertion.getAttributeStatements().add(attributeStatement);

        attributeStatement.getAttributes().add(createAttribute(HSA_ID_ATTRIBUTE, fakeCredentials.getPersonId()));
        attributeStatement.getAttributes().add(createAttribute(FORNAMN_ATTRIBUTE, fakeCredentials.getFirstName()));
        attributeStatement.getAttributes().add(
                createAttribute(MELLAN_OCH_EFTERNAMN_ATTRIBUTE, fakeCredentials.getLastName()));

        NameID nameId = new NameIDBuilder().buildObject();
        nameId.setValue(token.getCredentials().toString());
        return new SAMLCredential(nameId, assertion, "fake-idp", "webcert");
    }
}
