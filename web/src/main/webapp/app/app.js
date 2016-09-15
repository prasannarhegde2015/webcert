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

/* global wcMessages, console */

(function() {
    'use strict';

    // --- define test hooks
    window.doneLoading = false;
    window.rendered = true;
    window.saving = false;
    window.digest = 0;
    window.autoSave = true;

    window.setAutoSave = function(val) {
        window.autoSave = val;
    };

    window.setSaving = function(val) {
        window.saving = val;
    };
    // --- end test hooks

    // Globally configure jquery not to cache ajax requests.
    // Our other angular $http service driven requests have their own solution (using an interceptor)

    $.ajaxSetup({ cache: false });

    // before we do anything.. we need to get the user and moduleConfig
    var user;
    var moduleConfig;

    var app = angular.module('webcert',
        ['ui.bootstrap', 'ui.router', 'ngCookies', 'ngSanitize', 'common', 'ngAnimate', 'smoothScroll', 'formly', 'formlyBootstrap', 'ng.shims.placeholder']);

    app.value('networkConfig', {
        defaultTimeout: 30000 // test: 1000
    });

    function getModuleConfig() {
        return $.get('/api/config').then(function(data) {
            moduleConfig = data;
            app.constant('moduleConfig', moduleConfig);
            return data;
        });
    }

    function getUser() {
        var restPath = '/api/anvandare';
        return $.get(restPath).then(function(data) {
            user = data;
            // set jsMinified
            if (user !== undefined && user.features !== undefined && user.features.length > 0) {
                if (user.features.indexOf('jsMinified') >= 0) {
                    user.jsMinified = true;
                } else {
                    user.jsMinified = false;
                }
            }
            if (window.console) {
                console.log('---- user.jsMinified : ' + user.jsMinified);
            }
            return data;
        });
    }

    app.config(['$httpProvider', 'common.http403ResponseInterceptorProvider', '$logProvider',
        function($httpProvider, http403ResponseInterceptorProvider, $logProvider) {
            // Add cache buster interceptor
            $httpProvider.interceptors.push('common.httpRequestInterceptorCacheBuster');

            // Configure 403 interceptor provider
            http403ResponseInterceptorProvider.setRedirectUrl('/error.jsp');
            $httpProvider.interceptors.push('common.http403ResponseInterceptor');

            // Enable debug logging
            $logProvider.debugEnabled(false);
        }]);

    // Decorators that update form input names and interpolates them. Needed for datepicker directives templates dynamic name attributes
    app.config(function($provide) {

        $provide.decorator('ngModelDirective', function($delegate) {
            var ngModel = $delegate[0], controller = ngModel.controller;
            ngModel.controller =
                ['$scope', '$element', '$attrs', '$injector', function(scope, element, attrs, $injector) {
                    var $interpolate = $injector.get('$interpolate');
                    attrs.$set('name', $interpolate(attrs.name || '')(scope));
                    $injector.invoke(controller, this, {
                        '$scope': scope,
                        '$element': element,
                        '$attrs': attrs
                    });
                }];
            return $delegate;
        });
        $provide.decorator('formDirective', function($delegate) {
            var form = $delegate[0], controller = form.controller;
            form.controller = ['$scope', '$element', '$attrs', '$injector', function(scope, element, attrs, $injector) {
                var $interpolate = $injector.get('$interpolate');
                attrs.$set('name', $interpolate(attrs.name || attrs.ngForm || '')(scope));
                $injector.invoke(controller, this, {
                    '$scope': scope,
                    '$element': element,
                    '$attrs': attrs
                });
            }];
            return $delegate;
        });
    });

    // Global config of default date picker config (individual attributes can be
    // overridden per directive usage)
    app.constant('uibDatepickerPopupConfig', {
        altInputFormats: [],
        appendToBody: true,
        clearText: 'Rensa',
        closeOnDateSelection: true,
        closeText: 'OK',
        currentText: 'Idag',
        datepickerPopup: 'yyyy-MM-dd',
        datepickerPopupTemplateUrl: 'uib/template/datepickerPopup/popup.html',
        datepickerTemplateUrl: 'uib/template/datepicker/datepicker.html',
        html5Types: {
            date: 'yyyy-MM-dd',
            'datetime-local': 'yyyy-MM-ddTHH:mm:ss.sss',
            'month': 'yyyy-MM'
        },
        onOpenFocus: true,
        showButtonBar: true,
        placement: 'auto bottom-left'
    });

    // Inject language resources
    app.run(['$log', '$rootScope', '$window', '$location', '$state', '$q', 'common.messageService', 'common.UserModel', 'formlyConfig',
        function($log, $rootScope, $window, $location, $state, $q, messageService, UserModel, formlyConfig) {

            // Configure formly to use default hide directive.
            // must be ng-if or attic won't work because that works by watching when elements are destroyed and created, which only happens with ng-if.
            // With ng-show they are always in DOM and those phases won't happen.
            formlyConfig.extras.defaultHideDirective = 'ng-if';

            $rootScope.lang = 'sv';
            $rootScope.DEFAULT_LANG = 'sv';
            $rootScope.testModeActive = false;

            UserModel.setUser(user);
            UserModel.termsAccepted = user.privatLakareAvtalGodkand;

            messageService.addResources(wcMessages);

            $rootScope.$on('$stateChangeStart',
                function(event, toState, toParams/*, fromState, fromParams*/) {
                    // if we dont have a user then we need to defer until we do ..
                    var termsCheck = function() {
                        // check terms if not accepted then always redirect
                        if (toState.name !== 'webcert.terms') {
                            UserModel.transitioning = false;
                        }
                        if (UserModel.isPrivatLakare() && !UserModel.termsAccepted && !UserModel.transitioning) {
                            event.preventDefault();
                            UserModel.transitioning = true;
                            $state.transitionTo('webcert.terms');
                        }
                    };
                    if (!UserModel.user) {
                        event.preventDefault();
                        // gets resolved when a user is loaded
                        $state.transitionTo(toState.name, toParams);
                    } else {
                        termsCheck();
                        $window.doneLoading = false;
                    }
                });

            $rootScope.$on('$stateNotFound',
                function(event, unfoundState, fromState, fromParams) {
                    $log.debug('$stateNotFound '+unfoundState.to + '  - fired when a state cannot be found by its name.');
                    $log.debug(unfoundState, fromState, fromParams);
                });

            $rootScope.$on('$stateChangeSuccess',
                function(event, toState/*, toParams, fromState, fromParams*/) {
                    $log.debug('$stateChangeSuccess to ' + toState.name + '- fired once the state transition is complete.');
                    if (!UserModel.termsAccepted && UserModel.transitioning && toState.name === 'webcert.terms') {
                        UserModel.transitioning = false;
                    }
                    $window.doneLoading = true;
                });

            $rootScope.$on('$stateChangeError',
                function(event, toState, toParams, fromState, fromParams, error) {
                    $log.debug('$stateChangeError');
                    $log.debug(toState);
                });

            $rootScope.$on('$viewContentLoading', function(/*event, viewConfig*/) {
                // runs on individual scopes, so putting it in "run" doesn't work.
                $window.rendered = false;
                $log.debug('+++ $viewContentLoading, rendered : ' + $window.rendered);
            });

            $rootScope.$on('$viewContentLoaded', function(event) {
                $log.debug('$viewContentLoaded - fired after dom rendered',event);
                $window.rendered = true;
                $log.debug('--- $viewContentLoaded, rendered : ' + $window.rendered);
            });

        }]);


    // We need to have the moduleConfig available before loading modules
    getModuleConfig().then(function(data) {
        // Get a list of all modules to find all files to load.
        getUser().then(function(data) {
            user = data;
            $.get('/api/modules/map').then(function(modules) {


                var modulesIds = [];
                var modulePromises = [];

                if (user.jsMinified) {
                    modulePromises.push(loadScriptFromUrl('/web/webjars/common/webcert/module.min.js?' +
                                moduleConfig.BUILD_NUMBER));
                    // All dependencies in module-deps.json are included in module.min.js
                    // All dependencies in app-deps.json are included in app.min.js

                } else {
                    modulePromises.push(loadScriptFromUrl('/web/webjars/common/webcert/module.js'));
                    modulePromises.push($.get('/web/webjars/common/webcert/module-deps.json'));
                    modulePromises.push($.get('/app/app-deps.json'));

                    // Prevent jQuery from appending cache buster to the url to make it easier to debug.
                    $.ajaxSetup({
                        cache: true
                    });
                }

                angular.forEach(modules, function(module) {
                    modulesIds.push(module.id);
                    loadCssFromUrl(module.cssPath + '?' + moduleConfig.BUILD_NUMBER);

                    if (user.jsMinified) {

                        if (window.console) {
                            console.log('use mini is true! loading compressed modules');
                        }

                        modulePromises.push(loadScriptFromUrl(module.scriptPath + '.min.js?' + moduleConfig.BUILD_NUMBER));
                        // All dependencies for the modules are included in module.min.js
                    } else {
                        modulePromises.push(loadScriptFromUrl(module.scriptPath + '.js'));
                        modulePromises.push($.get(module.dependencyDefinitionPath));
                    }
                });

                // Wait for all modules and module dependency definitions to load.
                $.when.apply(this, modulePromises).then(function() {
                    var dependencyPromises = [];

                    // Only needed for development since all dependencies are included in other files.
                    if (!user.jsMinified) {
                        if (window.console) {
                            console.log('use mini is false! loading modules');
                        }
                        angular.forEach(arguments, function(data) {
                            if (data !== undefined && data[0] instanceof Array) {
                                angular.forEach(data[0], function(depdendency) {
                                    dependencyPromises.push(loadScriptFromUrl(depdendency));
                                });
                            }
                        });
                    }

                    // Wait for all dependencies to load (for production dependencies are empty which is resolved immediately)
                    $.when.apply(this, dependencyPromises).then(function() {
                        angular.element(document).ready(function() {

                            var allModules = [app.name, 'common'].concat(Array.prototype.slice.call(modulesIds, 0));

                            // Cant use common.featureService to check for this since it needs to be done before angular bootstrap.
                            if (user.jsLoggning) {
                                addExceptionHandler();
                            }

                            // Everything is loaded, bootstrap the application with all dependencies.
                            document.documentElement.setAttribute('ng-app', 'webcert');
                            angular.bootstrap(document, allModules);

                        });
                    }).fail(function(error) {
                        if (window.console) {
                            console.log(error);
                        }
                    });
                }).fail(function(error) {
                    if (window.console) {
                        console.log(error);
                    }
                });
            });
        });
    });

    function loadCssFromUrl(url) {


        var link = document.createElement('link');
        link.type = 'text/css';
        link.rel = 'stylesheet';
        link.href = url;
        document.getElementsByTagName('head')[0].appendChild(link);
    }

    function loadScriptFromUrl(url) {


        var result = $.Deferred();
        var script = document.createElement('script');
        script.async = 'async';
        script.type = 'text/javascript';
        script.src = url;
        script.onload = script.onreadystatechange = function(_, isAbort) {
            if (!script.readyState || /loaded|complete/.test(script.readyState)) {
                if (isAbort) {
                    result.reject();
                } else {
                    result.resolve();
                }
            }
        };
        script.onerror = function() {
            result.reject();
        };
        document.getElementsByTagName('head')[0].appendChild(script);
        return result.promise();
    }

    function addExceptionHandler() {

        // By default, AngularJS will catch errors and log them to
        // the Console. We want to keep that behavior; however, we
        // want to intercept it so that we can also log the errors
        // to the server for later analysis.
        app.provider('$exceptionHandler', function() { // $exceptionHandlerProvider
            this.$get = ['common.errorLogService', function(errorLogService) {
                return errorLogService;
            }];
        });
    }

}());
