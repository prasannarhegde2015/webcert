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
package se.inera.intyg.webcert.web.service.feature;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.base.Joiner;

import se.inera.intyg.common.support.modules.registry.IntygModuleRegistry;
import se.inera.intyg.common.support.modules.support.ModuleEntryPoint;
import se.inera.intyg.common.support.modules.support.feature.ModuleFeature;
import se.inera.intyg.infra.security.common.service.Feature;
import se.inera.intyg.infra.security.common.service.PilotService;

/**
 * Service that keeps track of active features of Webcert and installed modules.
 *
 * @author npet
 *
 */
@Service
public class WebcertFeatureServiceImpl implements WebcertFeatureService, EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(WebcertFeatureService.class);
    private static final String DOT_SEP = ".";
    private final Map<String, Boolean> featuresMap = new HashMap<>();
    @Autowired
    private IntygModuleRegistry moduleRegistry;
    @Autowired
    @Qualifier("webcertFeatures")
    private Properties features;
    @Autowired
    private PilotService pilotService;
    private Environment env;

    /**
     * Performs initialization of the featuresMap.
     */
    @PostConstruct
    public void initFeaturesMap() {
        initWebcertFeatures(featuresMap);
        initModuleFeatures(featuresMap);
        processWebcertAndModuleFeatureProperties(features, featuresMap);

        LOG.info("Active Webcert features is: {}", Joiner.on(", ").join(getActiveFeatures()));
    }

    /**
     * Inits the featuresMap with all Webcert features set to FALSE.
     */
    protected void initWebcertFeatures(Map<String, Boolean> featuresMap) {

        Assert.notNull(featuresMap);

        for (WebcertFeature feature : WebcertFeature.values()) {
            // the env name can be different to the enum name which is used in the gui.
            // as a result we can normalise the env names ... or translate them to the correct enum name.
            // I think we should normalise but this could be a bigger job, so translation will have to do.
            if (feature.getEnvName() != null && env.containsProperty(feature.getEnvName())) {
                features.setProperty(feature.getName(), env.getProperty(feature.getEnvName()));
            }
            featuresMap.put(feature.getName(), Boolean.FALSE);
        }
    }

    /**
     * Inits the featuresMap with module features. The features are collected from the modules
     * using the ModuleEntryPoint of the module. The names of the module feature is then qualified
     * using the id of the module.
     */
    protected void initModuleFeatures(Map<String, Boolean> featuresMap) {

        Assert.notNull(featuresMap);

        String moduleId;
        Map<String, Boolean> moduleMap;

        for (ModuleEntryPoint mep : moduleRegistry.getModuleEntryPoints()) {

            moduleId = mep.getModuleId();
            moduleMap = mep.getModuleFeatures();

            if (moduleMap == null || moduleMap.isEmpty()) {
                LOG.warn("Module {} did not expose any features! All features of this module will be disabled!", moduleId);
                moduleMap = Collections.emptyMap();
            }

            String key;
            String moduleFeatureName;
            String moduleName;
            Boolean moduleFeatureState;

            for (ModuleFeature moduleFeature : ModuleFeature.values()) {
                moduleFeatureName = moduleFeature.getName();
                moduleName = moduleId.toLowerCase();
                moduleFeatureState = (moduleMap.get(moduleFeatureName) != null) ? moduleMap.get(moduleFeatureName) : Boolean.FALSE;
                key = Joiner.on(DOT_SEP).join(moduleFeatureName, moduleName);
                featuresMap.put(key, moduleFeatureState);
            }
        }

    }

    /**
     * Reads the supplied properties and updates the state of the feature in the featuresMap.
     */
    protected void processWebcertAndModuleFeatureProperties(Properties featureProps, Map<String, Boolean> featuresMap) {

        Assert.notNull(featureProps);
        Assert.notEmpty(featuresMap);
        for (Entry<String, Boolean> entry : featuresMap.entrySet()) {
            String envProp = env.getProperty(entry.getKey());
            Boolean featureState = null;

            if (envProp != null) {
                featureState = Boolean.parseBoolean(envProp);
            } else if (featureProps.getProperty(entry.getKey()) != null) {
                featureState = Boolean.parseBoolean(featureProps.getProperty(entry.getKey()));
            }

            if (featureState != null) {
                entry.setValue(featureState);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see se.inera.intyg.webcert.web.service.feature.WebcertFeatureService#isFeatureActive(se.inera.intyg.webcert.web.service.feature.
     * WebcertFeature)
     */
    @Override
    public boolean isFeatureActive(Feature feature) {
        return isFeatureActive(feature.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see se.inera.intyg.webcert.web.service.feature.WebcertFeatureService#isFeatureActive(java.lang.String)
     */
    @Override
    public boolean isFeatureActive(String featureName) {
        Boolean featureState = featuresMap.get(featureName);
        return featureState != null && featureState;
    }

    /*
     * (non-Javadoc)
     *
     * @see se.inera.intyg.webcert.web.service.feature.WebcertFeatureService#isModuleFeatureActive(java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean isModuleFeatureActive(String moduleFeatureName, String moduleName) {
        if (isFeatureActive(moduleFeatureName)) {
            String key = Joiner.on(DOT_SEP).join(moduleFeatureName, moduleName.toLowerCase());
            Boolean moduleFeatureState = featuresMap.get(key);
            return moduleFeatureState != null && moduleFeatureState;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see se.inera.intyg.webcert.web.service.feature.WebcertFeatureService#getActiveFeatures()
     */
    @Override
    public Set<String> getActiveFeatures(String... hsaIds) {
        return merge(featuresMap.entrySet().stream()
                .filter(entry -> entry.getValue())
                .map(Entry::getKey)
                .collect(Collectors.toSet()),
                pilotService.getFeatures(Arrays.asList(hsaIds)));
    }

    public Properties getFeatures() {
        return features;
    }

    public void setFeatures(Properties features) {
        this.features = features;
    }

    @Override
    public void setFeature(String key, String value) {
        this.features.setProperty(key, value);
        this.featuresMap.put(key, Boolean.parseBoolean(value));
    }

    public Map<String, Boolean> getFeaturesMap() {
        return featuresMap;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }

    private Set<String> merge(Set<String> defaultFeatures, Map<String, Boolean> pilotFeatures) {
        // Separates the pilotfeaturesmap into two sets, one with all positives and one with all the negatives
        Map<Boolean, Set<String>> split = pilotFeatures.entrySet().stream()
                .collect(Collectors.partitioningBy(entry -> entry.getValue(),
                        Collectors.mapping(Entry::getKey, Collectors.toSet())));
        defaultFeatures.addAll(split.get(Boolean.TRUE));
        defaultFeatures.removeAll(split.get(Boolean.FALSE));
        return defaultFeatures;
    }
}
