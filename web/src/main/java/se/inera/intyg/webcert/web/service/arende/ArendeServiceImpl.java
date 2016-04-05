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

package se.inera.intyg.webcert.web.service.arende;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import se.inera.intyg.common.integration.hsa.services.HsaEmployeeService;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceErrorCodeEnum;
import se.inera.intyg.webcert.common.service.exception.WebCertServiceException;
import se.inera.intyg.webcert.persistence.arende.model.Arende;
import se.inera.intyg.webcert.persistence.arende.repository.ArendeRepository;
import se.inera.intyg.webcert.persistence.model.Filter;
import se.inera.intyg.webcert.persistence.model.Status;
import se.inera.intyg.webcert.persistence.utkast.model.Utkast;
import se.inera.intyg.webcert.persistence.utkast.repository.UtkastRepository;
import se.inera.intyg.webcert.web.converter.ArendeListItemConverter;
import se.inera.intyg.webcert.web.converter.FilterConverter;
import se.inera.intyg.webcert.web.converter.util.TransportToArende;
import se.inera.intyg.webcert.web.service.dto.Lakare;
import se.inera.intyg.webcert.web.service.fragasvar.FragaSvarService;
import se.inera.intyg.webcert.web.service.fragasvar.dto.FrageStallare;
import se.inera.intyg.webcert.web.service.fragasvar.dto.QueryFragaSvarParameter;
import se.inera.intyg.webcert.web.service.fragasvar.dto.QueryFragaSvarResponse;
import se.inera.intyg.webcert.web.service.monitoring.MonitoringLogService;
import se.inera.intyg.webcert.web.service.notification.NotificationEvent;
import se.inera.intyg.webcert.web.service.notification.NotificationService;
import se.inera.intyg.webcert.web.service.user.WebCertUserService;
import se.inera.intyg.webcert.web.service.user.dto.WebCertUser;
import se.inera.intyg.webcert.web.web.controller.api.dto.ArendeConversationView;
import se.inera.intyg.webcert.web.web.controller.api.dto.ArendeListItem;
import se.inera.intyg.webcert.web.web.controller.api.dto.ArendeView;
import se.inera.intyg.webcert.web.web.controller.api.dto.ArendeView.ArendeType;
import se.riv.infrastructure.directory.employee.getemployeeincludingprotectedpersonresponder.v1.GetEmployeeIncludingProtectedPersonResponseType;

@Service
@Transactional("jpaTransactionManager")
public class ArendeServiceImpl implements ArendeService {

    private static final Logger LOG = LoggerFactory.getLogger(ArendeServiceImpl.class);
    @Autowired
    private ArendeRepository repo;

    @Autowired
    private UtkastRepository utkastRepository;

    @Autowired
    private WebCertUserService webcertUserService;

    @Autowired
    private MonitoringLogService monitoringLog;

    @Autowired
    private TransportToArende transportToArende;

    @Autowired
    private HsaEmployeeService hsaEmployeeService;

    @Autowired
    private FragaSvarService fragaSvarService;

    @Autowired
    private NotificationService notificationService;

    private static final ArendeConversationViewTimeStampComparator ARENDE_TIMESTAMP_COMPARATOR = new ArendeConversationViewTimeStampComparator();

    @Override
    public Arende processIncomingMessage(Arende arende) throws WebCertServiceException {
        if (repo.findOneByMeddelandeId(arende.getMeddelandeId()) != null) {
            throw new WebCertServiceException(WebCertServiceErrorCodeEnum.INVALID_STATE, "meddelandeId not unique");
        }

        Utkast utkast = utkastRepository.findOne(arende.getIntygsId());
        if (utkast == null) {
            throw new WebCertServiceException(WebCertServiceErrorCodeEnum.DATA_NOT_FOUND,
                    "Certificate " + arende.getIntygsId() + " not found.");
        } else if (utkast.getSignatur() == null) {
            throw new WebCertServiceException(WebCertServiceErrorCodeEnum.INVALID_STATE,
                    "Certificate " + arende.getIntygsId() + " not signed.");
        }
        LocalDateTime now = LocalDateTime.now();
        decorateArende(arende, utkast, now);

        updateRelated(arende, now);

        monitoringLog.logArendeReceived(arende.getIntygsId(), utkast.getIntygsTyp(), utkast.getEnhetsId(), arende.getRubrik());

        return repo.save(arende);
    }

    private void updateRelated(Arende arende, LocalDateTime now) {
        if (arende.getSvarPaId() != null) {
            Optional.ofNullable(repo.findOneByMeddelandeId(arende.getSvarPaId())).ifPresent(a -> {
                a.setSenasteHandelse(now);
                a.setStatus(Status.ANSWERED);
            });
        } else if (arende.getPaminnelseMeddelandeId() != null) {
            Optional.ofNullable(repo.findOneByMeddelandeId(arende.getPaminnelseMeddelandeId())).ifPresent(a -> a.setSenasteHandelse(now));
        }
    }

    private void decorateArende(Arende arende, Utkast utkast, LocalDateTime now) {
        arende.setTimestamp(now);
        arende.setSenasteHandelse(now);
        arende.setStatus(arende.getSvarPaId() == null ? Status.PENDING_INTERNAL_ACTION : Status.ANSWERED);
        arende.setVidarebefordrad(Boolean.FALSE);

        arende.setIntygTyp(utkast.getIntygsTyp());
        arende.setSigneratAv(utkast.getSignatur().getSigneradAv());
        arende.setSigneratAvName(getSignedByName(utkast));
        arende.setEnhet(utkast.getEnhetsId());
    }

    private String getSignedByName(Utkast utkast) {
        return Optional.ofNullable(hsaEmployeeService.getEmployee(utkast.getSignatur().getSigneradAv(), null))
                .map(GetEmployeeIncludingProtectedPersonResponseType::getPersonInformation)
                .orElseThrow(() -> new WebCertServiceException(WebCertServiceErrorCodeEnum.EXTERNAL_SYSTEM_PROBLEM,
                        "HSA did not respond with information"))
                .stream()
                .filter(pit -> StringUtils.isNotEmpty(pit.getMiddleAndSurName()))
                .map(pit -> StringUtils.isNotEmpty(pit.getGivenName())
                        ? pit.getGivenName() + " " + pit.getMiddleAndSurName()
                        : pit.getMiddleAndSurName())
                .findFirst().orElseThrow(() -> new WebCertServiceException(WebCertServiceErrorCodeEnum.DATA_NOT_FOUND, "No name was found in HSA"));
    }

    @Override
    public List<Lakare> listSignedByForUnits(String enhetsId) throws WebCertServiceException {

        List<String> enhetsIdParams = new ArrayList<>();
        if (enhetsId != null) {
            verifyEnhetsAuth(enhetsId, true);
            enhetsIdParams.add(enhetsId);
        } else {
            enhetsIdParams.addAll(webcertUserService.getUser().getIdsOfSelectedVardenhet());
        }

        List<Lakare> arendeList = repo.findSigneratAvByEnhet(enhetsIdParams).stream()
                .map(arr -> new Lakare((String) arr[0], (String) arr[1]))
                .collect(Collectors.toList());

        // We need to maintain backwards compatibility. When FragaSvar no longer exist remove this part and return above
        // arendeList
        List<Lakare> fragaSvarList = fragaSvarService.getFragaSvarHsaIdByEnhet(enhetsId);
        return Lakare.merge(arendeList, fragaSvarList);
    }

    @Override
    public List<Arende> listArendeForUnits() throws WebCertServiceException {
        WebCertUser user = webcertUserService.getUser();
        List<String> unitIds = user.getIdsOfSelectedVardenhet();

        return repo.findByEnhet(unitIds);
    }

    @Override
    public List<ArendeConversationView> getArenden(String intygsId) {
        List<Arende> arendeList = repo.findByIntygsId(intygsId);

        WebCertUser user = webcertUserService.getUser();
        List<String> hsaEnhetIds = user.getIdsOfSelectedVardenhet();

        Iterator<Arende> iterator = arendeList.iterator();
        while (iterator.hasNext()) {
            Arende arende = iterator.next();
            if (arende.getEnhet() != null && !hsaEnhetIds.contains(arende.getEnhet())) {
                arendeList.remove(arende);
            }
        }
        List<ArendeView> arendeViews = new ArrayList<>();
        for (Arende arende : arendeList) {
            ArendeView latestDraft = transportToArende.convert(arende);
            arendeViews.add(latestDraft);
        }
        List<ArendeConversationView> arendeConversations = buildArendeConversations(arendeViews);
        Collections.sort(arendeConversations, ARENDE_TIMESTAMP_COMPARATOR);

        return arendeConversations;
    }

    private List<ArendeConversationView> buildArendeConversations(List<ArendeView> arendeViews) {
        List<ArendeConversationView> arendeConversations = new ArrayList<>();
        Map<String, List<ArendeView>> threads = new HashMap<>();
        String meddelandeId = null;
        for (ArendeView arende : arendeViews) { // divide into threads
            meddelandeId = getMeddelandeId(arende);
            if (threads.get(meddelandeId) == null) {
                threads.put(meddelandeId, new ArrayList<ArendeView>());
            }
            threads.get(meddelandeId).add(arende);
        }

        for (String meddelandeIdd : threads.keySet()) {
            List<ArendeView> arendeConversationContent = threads.get(meddelandeIdd);
            List<ArendeView> paminnelser = new ArrayList<>();
            ArendeView fraga = null, svar = null;
            LocalDateTime senasteHandelse = null;
            for (ArendeView view : arendeConversationContent) {
                if (view.getArendeType() == ArendeType.FRAGA) {
                    fraga = view;
                } else if (view.getArendeType() == ArendeType.SVAR) {
                    svar = view;
                } else {
                    paminnelser.add(view);
                }
                if (senasteHandelse == null || senasteHandelse.isBefore(view.getTimestamp())) {
                    senasteHandelse = view.getTimestamp();
                }
            }

            arendeConversations.add(ArendeConversationView.create(fraga, svar, senasteHandelse, paminnelser));
        }
        return arendeConversations;
    }

    private String getMeddelandeId(ArendeView arende) {
        String referenceId = (arende.getSvarPaId() != null) ? arende.getSvarPaId() : arende.getPaminnelseMeddelandeId();
        String meddelandeId = (referenceId != null) ? referenceId : arende.getInternReferens();
        return meddelandeId;
    }

    @Override
    @Transactional(value = "jpaTransactionManager", readOnly = true)
    public QueryFragaSvarResponse filterArende(QueryFragaSvarParameter filterParameters) {
        Filter filter;
        if (StringUtils.isNotEmpty(filterParameters.getEnhetId())) {
            verifyEnhetsAuth(filterParameters.getEnhetId(), true);
            filter = FilterConverter.convert(filterParameters, Arrays.asList(filterParameters.getEnhetId()));
        } else {
            filter = FilterConverter.convert(filterParameters, webcertUserService.getUser().getIdsOfSelectedVardenhet());
        }

        int originalStartFrom = filter.getStartFrom();
        int originalPageSize = filter.getPageSize();

        // update page size and start from to be able to merge FragaSvar and Arende properly
        filter.setStartFrom(Integer.valueOf(0));
        filter.setPageSize(originalPageSize + originalStartFrom);

        List<ArendeListItem> results = repo.filterArende(filter).stream()
                .map(ArendeListItemConverter::convert)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        QueryFragaSvarResponse fsResults = fragaSvarService.filterFragaSvar(filter);

        int totalResultsCount = repo.filterArendeCount(filter) + fsResults.getTotalCount();

        results.addAll(fsResults.getResults());
        results.sort(Comparator.comparing(ArendeListItem::getReceivedDate));

        QueryFragaSvarResponse response = new QueryFragaSvarResponse();
        if (originalStartFrom >= results.size()) {
            response.setResults(new ArrayList<>());
        } else {
            response.setResults(results.subList(originalStartFrom, Math.min(originalPageSize + originalStartFrom, results.size())));
        }
        response.setTotalCount(totalResultsCount);

        return response;
    }

    protected void verifyEnhetsAuth(String enhetsId, boolean isReadOnlyOperation) {
        if (!webcertUserService.isAuthorizedForUnit(enhetsId, isReadOnlyOperation)) {
            throw new WebCertServiceException(WebCertServiceErrorCodeEnum.AUTHORIZATION_PROBLEM,
                    "User not authorized for for enhet " + enhetsId);
        }
    }

    @Override
    @Transactional
    public Arende closeArendeAsHandled(String meddelandeId) {
        Arende arendeToClose = lookupArende(meddelandeId);
        NotificationEvent notificationEvent = determineNotificationEvent(arendeToClose);
        Arende closedArende = closeArendeAsHandled(arendeToClose);

        if (notificationEvent != null) {
            sendNotification(closedArende, notificationEvent);
        }
        return closedArende;
    }

    private NotificationEvent determineNotificationEvent(Arende arende) {
        // TODO make sure this is populated with the correct information from Arende
        FrageStallare frageStallare = FrageStallare.getByKod(arende.getSkickatAv());
        Status arendeSvarStatus = arende.getStatus();

        if (FrageStallare.FORSAKRINGSKASSAN.equals(frageStallare)) {
            if (Status.PENDING_INTERNAL_ACTION.equals(arendeSvarStatus)) {
                return NotificationEvent.QUESTION_FROM_FK_HANDLED;
            } else if (Status.CLOSED.equals(arendeSvarStatus)) {
                return NotificationEvent.QUESTION_FROM_FK_UNHANDLED;
            }
        }

        if (FrageStallare.WEBCERT.equals(frageStallare)) {
            if (Status.ANSWERED.equals(arendeSvarStatus)) {
                return NotificationEvent.ANSWER_FROM_FK_HANDLED;
            } else if (Status.CLOSED.equals(arendeSvarStatus) && StringUtils.isNotEmpty(arende.getMeddelande())) {
                return NotificationEvent.ANSWER_FROM_FK_UNHANDLED;
            }
        }

        return null;
    }

    private void sendNotification(Arende arende, NotificationEvent event) {
        switch (event) {
        case ANSWER_FROM_FK_HANDLED:
            notificationService.sendNotificationForAnswerHandled(arende);
            break;
        case ANSWER_FROM_FK_UNHANDLED:
            notificationService.sendNotificationForAnswerRecieved(arende);
            break;
        case ANSWER_SENT_TO_FK:
            notificationService.sendNotificationForQuestionHandled(arende);
            break;
        case QUESTION_FROM_FK_HANDLED:
            notificationService.sendNotificationForQuestionHandled(arende);
            break;
        case QUESTION_FROM_FK_UNHANDLED:
            notificationService.sendNotificationForQuestionReceived(arende);
            break;
        case QUESTION_SENT_TO_FK:
            notificationService.sendNotificationForQuestionSent(arende);
            break;
        default:
            LOG.warn("ArendeServiceImpl.sendNotification(Arende, NotificationEvent) - cannot send notification. Incoming event not handled!");
        }
    }

    private Arende closeArendeAsHandled(Arende arende) {
        arende.setStatus(Status.CLOSED);
        return repo.save(arende);
    }

    private Arende lookupArende(String meddelandeId) {
        Arende arende = repo.findOneByMeddelandeId(meddelandeId);
        if (arende == null) {
            throw new WebCertServiceException(WebCertServiceErrorCodeEnum.INTERNAL_PROBLEM,
                    "Could not find Arende with id:" + meddelandeId);
        }
        return arende;
    }

    public static class ArendeConversationViewTimeStampComparator implements Comparator<ArendeConversationView> {

        @Override
        public int compare(ArendeConversationView f1, ArendeConversationView f2) {
            if (f1.getSenasteHandelse() == null && f2.getSenasteHandelse() == null) {
                return 0;
            } else if (f1.getSenasteHandelse() == null) {
                return -1;
            } else if (f2.getSenasteHandelse() == null) {
                return 1;
            } else {
                return f2.getSenasteHandelse().compareTo(f1.getSenasteHandelse());
            }
        }
    }
}
