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

package se.inera.intyg.webcert.persistence.fragasvar.repository;

import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;

import se.inera.intyg.webcert.persistence.fragasvar.model.Amne;
import se.inera.intyg.webcert.persistence.fragasvar.model.FragaSvar;
import se.inera.intyg.webcert.persistence.model.Filter;
import se.inera.intyg.webcert.persistence.model.Status;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.List;

/**
 * Created by pehr on 10/21/13.
 */
public class FragaSvarRepositoryImpl implements FragaSvarFilteredRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private Predicate createPredicate(Filter filter, CriteriaBuilder builder, Root<FragaSvar> root) {
        Predicate pred = builder.conjunction();

        pred = builder.and(pred, root.get("vardperson").get("enhetsId").in(filter.getEnhetsIds()));

        if (filter.isQuestionFromFK()) {
            pred = builder.and(pred, builder.equal(root.get("frageStallare"), "FK"));
        }

        if (filter.isQuestionFromWC()) {
            pred = builder.and(pred, builder.equal(root.get("frageStallare"), "WC"));
        }

        if (StringUtils.isNotEmpty(filter.getHsaId())) {
            pred = builder.and(pred, builder.equal(root.get("vardperson").get("hsaId"), filter.getHsaId()));
        }

        if (filter.getVidarebefordrad() != null) {
            pred = builder.and(pred, builder.equal(root.<Boolean>get("vidarebefordrad"), filter.getVidarebefordrad()));
        }

        if (filter.getChangedFrom() != null) {
            pred = builder.and(pred, builder.greaterThanOrEqualTo(root.<LocalDate>get("senasteHandelse"), filter.getChangedFrom()));
        }

        if (filter.getChangedTo() != null) {
            pred = builder.and(pred, builder.lessThan(root.<LocalDate>get("senasteHandelse"), filter.getChangedTo()));
        }

        if (filter.getReplyLatest() != null) {
            pred = builder.and(pred, builder.lessThanOrEqualTo(root.<LocalDate>get("sistaDatumForSvar"), filter.getReplyLatest()));
        }

        switch (filter.getVantarPa()) {
            case ALLA_OHANTERADE:
                pred = builder.and(pred, builder.notEqual(root.<Status>get("status"), Status.CLOSED));
                break;
            case HANTERAD:
                pred = builder.and(pred, builder.equal(root.<Status>get("status"), Status.CLOSED));
                break;
            case KOMPLETTERING_FRAN_VARDEN:
                pred = builder.and(pred, builder.equal(root.<Status>get("status"), Status.PENDING_INTERNAL_ACTION), builder.equal(root.<Amne>get("amne"), Amne.KOMPLETTERING_AV_LAKARINTYG));
                break;
            case SVAR_FRAN_VARDEN:
                Predicate careReplyAmnePred = builder.or(builder.equal(root.<Amne>get("amne"), Amne.OVRIGT), builder.equal(root.<Amne>get("amne"), Amne.ARBETSTIDSFORLAGGNING), builder.equal(root.<Amne>get("amne"), Amne.AVSTAMNINGSMOTE), builder.equal(root.<Amne>get("amne"), Amne.KONTAKT));
                pred = builder.and(pred, builder.equal(root.<Status>get("status"), Status.PENDING_INTERNAL_ACTION), careReplyAmnePred);
                break;
            case SVAR_FRAN_FK:
                pred = builder.and(pred, builder.equal(root.<Status>get("status"), Status.PENDING_EXTERNAL_ACTION), builder.notEqual(root.<Amne>get("amne"), Amne.MAKULERING_AV_LAKARINTYG));
                break;
            case MARKERA_SOM_HANTERAD:
                Predicate amnePred1;
                amnePred1 = builder.and(builder.equal(root.<Status>get("status"), Status.PENDING_INTERNAL_ACTION), builder.equal(root.<Amne>get("amne"), Amne.MAKULERING_AV_LAKARINTYG));

                Predicate amnePred2;
                amnePred2 = builder.and(builder.equal(root.<Status>get("status"), Status.PENDING_INTERNAL_ACTION), builder.equal(root.<Amne>get("amne"), Amne.PAMINNELSE));

                pred = builder.and(pred, builder.or(amnePred1, amnePred2, builder.equal(root.<Status>get("status"), Status.ANSWERED)));
                break;
            case ALLA:
            default:
                break;
        }
        return pred;
    }

    @Override
    public List<FragaSvar> filterFragaSvar(Filter filter) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<FragaSvar> cq = builder.createQuery(FragaSvar.class);

        Root<FragaSvar> root = cq.from(FragaSvar.class);

        cq.where(createPredicate(filter, builder, root));
        cq.orderBy(builder.desc(root.get("senasteHandelse")));

        TypedQuery<FragaSvar> query = entityManager.createQuery(cq);

        if (filter.hasPageSizeAndStartFrom()) {
            query.setMaxResults(filter.getPageSize());
            query.setFirstResult(filter.getStartFrom());
        }

        return query.getResultList();
    }

    @Override
    public int filterCountFragaSvar(Filter filter) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<FragaSvar> root = cq.from(FragaSvar.class);
        cq.select(cb.count(root));

        cq.where(createPredicate(filter, cb, root));

        Query query = entityManager.createQuery(cq);

        return ((Long) query.getSingleResult()).intValue();
    }

}
