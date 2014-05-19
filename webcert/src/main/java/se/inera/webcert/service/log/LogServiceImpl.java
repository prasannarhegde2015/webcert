package se.inera.webcert.service.log;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import se.inera.log.messages.AbstractLogMessage;
import se.inera.log.messages.CreateDraftMessage;
import se.inera.log.messages.DeleteDraftMessage;
import se.inera.log.messages.Enhet;
import se.inera.log.messages.IntygPrintMessage;
import se.inera.log.messages.IntygReadMessage;
import se.inera.log.messages.Patient;
import se.inera.log.messages.UpdateDraftMessage;
import se.inera.webcert.hsa.model.SelectableVardenhet;
import se.inera.webcert.hsa.model.WebCertUser;
import se.inera.webcert.service.log.dto.LogRequest;
import se.inera.webcert.web.service.WebCertUserService;

/**
 * Implementation of service for logging user actions according to PDL requirements.
 *
 * @author nikpet
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogServiceImpl.class);

    @Autowired(required = false)
    private JmsTemplate jmsTemplate;

    @Value("${pdlLogging.systemId}")
    private String systemId;

    @Autowired
    private WebCertUserService webCertUserService;

    @PostConstruct
    public void checkJmsTemplate() {
        if (jmsTemplate == null) {
            LOGGER.error("PDL logging is disabled!");
        }
    }

    @Override
    public void logReadOfIntyg(LogRequest logRequest) {
        send(populateLogMessage(logRequest, new IntygReadMessage(logRequest.getIntygId())));
    }

    @Override
    public void logPrintOfIntyg(LogRequest logRequest) {
        send(populateLogMessage(logRequest, new IntygPrintMessage(logRequest.getIntygId())));
    }

    @Override
    public void logCreateOfDraft(LogRequest logRequest) {
        send(populateLogMessage(logRequest, new CreateDraftMessage(logRequest.getIntygId())));
    }

    @Override
    public void logUpdateOfDraft(LogRequest logRequest) {
        send(populateLogMessage(logRequest, new UpdateDraftMessage(logRequest.getIntygId())));
    }

    @Override
    public void logDeleteOfDraft(LogRequest logRequest) {
        send(populateLogMessage(logRequest, new DeleteDraftMessage(logRequest.getIntygId())));
    }

    private AbstractLogMessage populateLogMessage(LogRequest logRequest, AbstractLogMessage logMsg) {

        populateWithCurrentUserAndCareUnit(logMsg);

        Patient patient = new Patient(logRequest.getPatientId(), logRequest.getPatientName());
        logMsg.setPatient(patient);

        String careUnitId = logRequest.getIntygCareUnitId();
        String careUnitName = logRequest.getIntygCareUnitName();

        String careGiverId = logRequest.getIntygCareGiverId();
        String careGiverName = logRequest.getIntygCareGiverName();

        Enhet resourceOwner = new Enhet(careUnitId, careUnitName, careGiverId, careGiverName);
        logMsg.setResourceOwner(resourceOwner);

        logMsg.setSystemId(systemId);

        return logMsg;
    }

    private void populateWithCurrentUserAndCareUnit(AbstractLogMessage logMsg) {
        WebCertUser user = webCertUserService.getWebCertUser();
        logMsg.setUserId(user.getHsaId());
        logMsg.setUserName(user.getNamn());

        SelectableVardenhet valdVardenhet = user.getValdVardenhet();
        String enhetsId = valdVardenhet.getId();
        String enhetsNamn = valdVardenhet.getNamn();

        SelectableVardenhet valdVardgivare = user.getValdVardgivare();
        String vardgivareId = valdVardgivare.getId();
        String vardgivareNamn = valdVardgivare.getNamn();

        Enhet vardenhet = new Enhet(enhetsId, enhetsNamn, vardgivareId, vardgivareNamn);
        logMsg.setUserCareUnit(vardenhet);
    }

    private void send(AbstractLogMessage logMsg) {

        if (jmsTemplate == null) {
            LOGGER.warn("Can not log {} of Intyg '{}' since PDL logging is disabled!", logMsg.getActivityType(), logMsg.getActivityLevel());
            return;
        }

        LOGGER.debug("Logging {} of Intyg {}", logMsg.getActivityType(), logMsg.getActivityLevel());

        jmsTemplate.send(new MC(logMsg));
    }

    private static final class MC implements MessageCreator {
        private final AbstractLogMessage logMsg;

        public MC(AbstractLogMessage log) {
            this.logMsg = log;
        }

        public Message createMessage(Session session) throws JMSException {
            return session.createObjectMessage(logMsg);
        }
    }
}
