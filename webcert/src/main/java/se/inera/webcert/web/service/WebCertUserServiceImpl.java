/**
 * Copyright (C) 2013 Inera AB (http://www.inera.se)
 *
 * This file is part of Inera Certificate Web (http://code.google.com/p/inera-certificate-web).
 *
 * Inera Certificate Web is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Inera Certificate Web is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.inera.webcert.web.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import se.inera.webcert.hsa.model.WebCertUser;

@Service
public class WebCertUserServiceImpl implements WebCertUserService {

    public WebCertUser getWebCertUser() {
        return (WebCertUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    public boolean isAuthorizedForUnit(String enhetsHsaId) {
        WebCertUser user = getWebCertUser();
        return user != null && user.getIdsOfSelectedVardenhet().contains(enhetsHsaId);
    }
    
    public boolean isAuthorizedForUnits(List<String> enhetsHsaIds) {
        WebCertUser user = getWebCertUser();
        return user != null && user.getIdsOfSelectedVardenhet().containsAll(enhetsHsaIds);   
    }
    
}
