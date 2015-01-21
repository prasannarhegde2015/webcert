package se.inera.webcert.notifications.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import se.inera.certificate.clinicalprocess.healthcond.certificate.certificatestatusupdateforcareresponder.v1.CertificateStatusUpdateForCareType;
import se.inera.certificate.clinicalprocess.healthcond.certificate.certificatestatusupdateforcareresponder.v1.UtlatandeType;
import se.inera.webcert.notifications.TestUtkastProducer;
import se.inera.webcert.persistence.utkast.model.Utkast;

public class EnrichWithIntygDataStrategyTest {

    private EnrichWithIntygDataStrategy strategy;

    private TestUtkastProducer dataProducer = new TestUtkastProducer();

    @Before
    public void setup() {
        this.strategy = new EnrichWithIntygDataStrategy();
    }

    @Test
    public void testEnrichWithIntygData() {

        UtlatandeType orgUtlatande = new UtlatandeType();

        CertificateStatusUpdateForCareType statusUpdateType = new CertificateStatusUpdateForCareType();
        statusUpdateType.setUtlatande(orgUtlatande);

        Utkast intygsUtkast = dataProducer.buildUtkast("intyg/intyg-1.json");

        CertificateStatusUpdateForCareType res = strategy.enrichWithIntygProperties(statusUpdateType, intygsUtkast);
        assertNotNull(res.getUtlatande());
        assertNotNull(res.getUtlatande().getSkapadAv());
        assertNotNull(res.getUtlatande().getSkapadAv().getFullstandigtNamn());
        assertNotNull(res.getUtlatande().getSkapadAv().getPersonalId());
        assertNotNull(res.getUtlatande().getSkapadAv().getEnhet());
        assertNotNull(res.getUtlatande().getSkapadAv().getEnhet().getEnhetsId());
        assertNotNull(res.getUtlatande().getSkapadAv().getEnhet().getEnhetsId().getExtension());
        assertNotNull(res.getUtlatande().getSkapadAv().getEnhet().getEnhetsId().getRoot());
        assertNotNull(res.getUtlatande().getSkapadAv().getEnhet().getEnhetsnamn());
    }

    @Test
    public void testVardenhetAndHosPersonWithCorrectId() {
        UtlatandeType orgUtlatande = new UtlatandeType();

        CertificateStatusUpdateForCareType statusUpdateType = new CertificateStatusUpdateForCareType();
        statusUpdateType.setUtlatande(orgUtlatande);

        Utkast intygsUtkast = dataProducer.buildUtkast("intyg/intyg-1.json");
        String hosPersonId = intygsUtkast.getSkapadAv().getHsaId();
        String enhetsId = intygsUtkast.getEnhetsId();
        
        CertificateStatusUpdateForCareType res = strategy.enrichWithIntygProperties(statusUpdateType, intygsUtkast);

        assertEquals(res.getUtlatande().getSkapadAv().getEnhet().getEnhetsId().getExtension(), enhetsId);
        assertEquals(res.getUtlatande().getSkapadAv().getPersonalId().getExtension(), hosPersonId);
    }

}
