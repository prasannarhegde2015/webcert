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

package se.inera.intyg.webcert.logsender;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.stereotype.Component;
import se.inera.intyg.common.logmessages.AbstractLogMessage;
import se.inera.intyg.common.logmessages.Enhet;
import se.inera.intyg.common.logmessages.Patient;
import se.inera.intyg.webcert.logsender.exception.LoggtjanstExecutionException;
import se.riv.ehr.log.store.storelog.rivtabp21.v1.StoreLogResponderInterface;
import se.riv.ehr.log.store.storelogresponder.v1.StoreLogRequestType;
import se.riv.ehr.log.store.storelogresponder.v1.StoreLogResponseType;
import se.riv.ehr.log.v1.ActivityType;
import se.riv.ehr.log.v1.CareProviderType;
import se.riv.ehr.log.v1.CareUnitType;
import se.riv.ehr.log.v1.LogType;
import se.riv.ehr.log.v1.PatientType;
import se.riv.ehr.log.v1.ResourceType;
import se.riv.ehr.log.v1.ResourcesType;
import se.riv.ehr.log.v1.SystemType;
import se.riv.ehr.log.v1.UserType;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.xml.ws.WebServiceException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author andreaskaltenbach
 */
@Component
public class LogSender {

    private static final Logger LOG = LoggerFactory.getLogger(LogSender.class);

    @Value("${loggtjanst.logicalAddress}")
    private String logicalAddress;

    @Value("${logsender.bulkSize}")
    private int bulkSize;

    @Autowired
    private StoreLogResponderInterface loggTjanstResponder;

    @Autowired
    @Qualifier("jmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    @Qualifier("nonTransactedJmsTemplate")
    private JmsTemplate nonTransactedJmsTemplate;

    @Autowired
    private Queue queue;

    @PostConstruct
    public void checkConfiguration() {
        if (bulkSize == 0) {
            throw new IllegalStateException("'bulkSize' has to be greater than zero");
        }
    }

    private int bulkSize() {
        return nonTransactedJmsTemplate.execute(new SessionCallback<Integer>() {
            @Override
            public Integer doInJms(Session session) throws JMSException {
                QueueBrowser queueBrowser = session.createBrowser(queue);
                Enumeration queueMessageEnum = queueBrowser.getEnumeration();
                int count = 0;
                while (queueMessageEnum.hasMoreElements() && count < bulkSize) {
                    queueMessageEnum.nextElement();
                    count++;
                }
                return count;
            }
        }, true);
    }

    public void sendLogEntries() {

        final int chunk = bulkSize();

        if (chunk == 0) {
            LOG.info("Zero messages in logging queue. Nothing will be sent to loggtjänst");
        } else {
            Boolean reExecute = jmsTemplate.execute(new JmsToLogSender(chunk), true);

            // there may be messages left on the queue after the first chunk, so reperform the action
            if (reExecute) {
                sendLogEntries();
            }
        }

    }

    private List<LogType> convert(List<Message> messages) {
        List<LogType> logTypes = new ArrayList<>();
        for (Message message : messages) {
            logTypes.add(convert(message));
        }
        return logTypes;
    }

    /**
     * For WC 4.1 and RHS 1.0, adapted to handle lists of AbstractLogMessage too.
     * @param message
     * @return
     */
    private LogType convert(Message message) {
        try {
            Object element = ((ObjectMessage) message).getObject();

            if (element instanceof AbstractLogMessage) {
                AbstractLogMessage logMessage = (AbstractLogMessage) element;
                return convert(logMessage);

            } else if (element instanceof ArrayList) {

                ArrayList<AbstractLogMessage> logMessages = (ArrayList<AbstractLogMessage>) element;

                // TODO validate: All messages in List must originate from same user and system. Only patient info may differ.
                return convertFromList(logMessages);

            } else {
                throw new RuntimeException("Unrecognized message type " + element.getClass().getCanonicalName());
            }

        } catch (JMSException e) {
            throw new RuntimeException("Failed to read incoming JMS message", e);
        }
    }

    private LogType convertFromList(List<AbstractLogMessage> sources) {
        AbstractLogMessage source = sources.get(0);
        LogType logType = new LogType();

        logType.setLogId(source.getLogId());

        SystemType system = new SystemType();
        system.setSystemId(source.getSystemId());
        system.setSystemName(source.getSystemName());
        logType.setSystem(system);

        ActivityType activity = new ActivityType();
        activity.setActivityType(source.getActivityType().getType());
        activity.setStartDate(source.getTimestamp());
        activity.setPurpose(source.getPurpose().getType());
        activity.setActivityLevel(source.getActivityLevel());

        if (StringUtils.isNotEmpty(source.getActivityArgs())) {
            activity.setActivityArgs(source.getActivityArgs());
        }

        logType.setActivity(activity);

        UserType user = new UserType();
        user.setUserId(source.getUserId());
        user.setName(source.getUserName());
        user.setCareProvider(careProvider(source.getUserCareUnit()));
        user.setCareUnit(careUnit(source.getUserCareUnit()));
        logType.setUser(user);

        logType.setResources(new ResourcesType());
        logType.getResources().getResource().addAll(sources.stream().map(this::buildResource).collect(Collectors.toList()));
        if (LOG.isDebugEnabled()) {
            LOG.debug("LogType built: ActivityType: {}, UserId: {}, SystemId: {} with {} resources: ",
                    logType.getActivity().getActivityType(),
                    logType.getUser().getUserId(),
                    logType.getSystem().getSystemId(),
                    logType.getResources().getResource().size());
        }
        return logType;
    }

    private ResourceType buildResource(AbstractLogMessage source) {
        ResourceType resource = new ResourceType();
        resource.setResourceType(source.getResourceType());
        resource.setCareProvider(careProvider(source.getResourceOwner()));
        resource.setCareUnit(careUnit(source.getResourceOwner()));

        resource.setPatient(patient(source.getPatient()));
        return resource;
    }

    private LogType convert(AbstractLogMessage source) {
        LogType logType = new LogType();

        logType.setLogId(source.getLogId());

        SystemType system = new SystemType();
        system.setSystemId(source.getSystemId());
        system.setSystemName(source.getSystemName());
        logType.setSystem(system);

        ActivityType activity = new ActivityType();
        activity.setActivityType(source.getActivityType().getType());
        activity.setStartDate(source.getTimestamp());
        activity.setPurpose(source.getPurpose().getType());
        activity.setActivityLevel(source.getActivityLevel());

        if (StringUtils.isNotEmpty(source.getActivityArgs())) {
            activity.setActivityArgs(source.getActivityArgs());
        }

        logType.setActivity(activity);

        UserType user = new UserType();
        user.setUserId(source.getUserId());
        user.setName(source.getUserName());
        user.setCareProvider(careProvider(source.getUserCareUnit()));
        user.setCareUnit(careUnit(source.getUserCareUnit()));
        logType.setUser(user);

        logType.setResources(new ResourcesType());

        ResourceType resource = buildResource(source);

        logType.getResources().getResource().add(resource);

        return logType;
    }

    private PatientType patient(Patient source) {
        PatientType patient = new PatientType();
        patient.setPatientId(source.getPatientId().getPersonnummerWithoutDash());
        patient.setPatientName(source.getPatientNamn());
        return patient;
    }

    private CareUnitType careUnit(Enhet source) {
        CareUnitType careUnit = new CareUnitType();
        careUnit.setCareUnitId(source.getEnhetsId());
        careUnit.setCareUnitName(source.getEnhetsNamn());
        return careUnit;
    }

    private CareProviderType careProvider(Enhet source) {
        CareProviderType careProvider = new CareProviderType();
        careProvider.setCareProviderId(source.getVardgivareId());
        careProvider.setCareProviderName(source.getVardgivareNamn());
        return careProvider;
    }

    private void sendLogEntriesToLoggtjanst(List<LogType> logEntries) {

        StoreLogRequestType request = new StoreLogRequestType();
        request.getLog().addAll(logEntries);

        try {
            StoreLogResponseType response = loggTjanstResponder.storeLog(logicalAddress, request);
            switch (response.getResultType().getResultCode()) {
            case OK:
            case INFO:
                break;
            default:
                throw new LoggtjanstExecutionException();
            }
        } catch (WebServiceException e) {
            throw new LoggtjanstExecutionException(e);
        }

    }

    private final class JmsToLogSender implements SessionCallback<Boolean> {
        private final int chunk;

        private JmsToLogSender(int chunk) {
            this.chunk = chunk;
        }

        @Override
        public Boolean doInJms(Session session) throws JMSException {
            LOG.info("Transferring {} log entries to loggtjänst.", chunk);

            MessageConsumer consumer = session.createConsumer(queue);

            List<Message> messages = new ArrayList<>();
            for (int i = 0; i < chunk; i++) {
                messages.add(consumer.receive());
            }

            try {
                sendLogEntriesToLoggtjanst(convert(messages));
                session.commit();
                return true;
            } catch (LoggtjanstExecutionException e) {
                LOG.error("Failed to send log entries to loggtjänst, JMS session will be rolled back.", e);
                session.rollback();
                return false;
            }
        }
    }
}
