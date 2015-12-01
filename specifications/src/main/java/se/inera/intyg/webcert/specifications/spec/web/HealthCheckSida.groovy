package se.inera.intyg.webcert.specifications.spec.web

import se.inera.intyg.common.specifications.spec.Browser
import se.inera.intyg.webcert.specifications.pages.HealthCheckPage
import se.inera.intyg.webcert.specifications.spec.util.screenshot.ExceptionHandlingFixture

class HealthCheckSida extends ExceptionHandlingFixture {

    private String databasTid
    private String databasStatus
    private String jmsTid
    private String jmsStatus
    private String hsaTid
    private String hsaStatus
    private String intygstjanstTid
    private String intygstjanstStatus
    private String signeringsKo
    private String upptid

    void execute() {
        Browser.drive {
            to HealthCheckPage
            assert at(HealthCheckPage)
            databasTid = page.dbMeasurement
            databasStatus = page.dbStatus
            jmsTid = page.jmsMeasurement
            jmsStatus = page.jmsStatus
            hsaTid = page.hsaMeasurement
            hsaStatus = page.hsaStatus
            intygstjanstTid = page.intygstjanstMeasurement
            intygstjanstStatus = page.intygstjanstStatus
            signeringsKo = page.signatureQueueMeasurement
            upptid = page.uptime
        }
    }

    String databasTid() {
        databasTid
    }
    String databasStatus() {
        databasStatus
    }
    String jmsTid() {
        jmsTid
    }
    String jmsStatus() {
        jmsStatus
    }
    String hsaTid() {
        hsaTid
    }
    String hsaStatus() {
        hsaStatus
    }
    String intygstjanstTid() {
        intygstjanstTid
    }
    String intygstjanstStatus() {
        intygstjanstStatus
    }
    String signeringsKo() {
        signeringsKo
    }
    String upptid() {
        upptid
    }

}
