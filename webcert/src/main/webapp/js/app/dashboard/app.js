'use strict';

/* Lakare Dashboard App Module */
angular.module('wcDashBoardApp', [ 'ui.bootstrap', 'modules.messages', 'wc.common.directives', 'dashboard.services', 'wc.utils', 'wc.common.fragasvarmodule' ]);
angular.module('wcDashBoardApp').config([ '$routeProvider', '$httpProvider', function($routeProvider, $httpProvider) {
    $routeProvider.when('/index', {
        templateUrl : '/views/dashboard/searchcreate-step1.html',
        controller : 'CreateCertCtrl'
    }).when('/searchcreate-editpatient', {
      templateUrl : '/views/dashboard/searchcreate-editpatient.html',
      controller : 'CreateCertCtrl'
    }).when('/searchcreate-step2', {
      templateUrl : '/views/dashboard/searchcreate-step2.html',
      controller : 'CreateCertCtrl'
    }).when('/mycert', {
      templateUrl : '/views/dashboard/mycert.html',
      controller : 'WebCertCtrl'
    }).when('/unhandled-qa', {
        templateUrl : '/views/dashboard/unhandled-qa.html',
        controller : 'UnhandledQACtrl'
    }).when('/unsigned', {
      templateUrl : '/views/dashboard/unsigned.html',
      controller : 'UnsignedCertCtrl'
    }).when('/view', {
        templateUrl : '/views/dashboard/view-cert.html',
        controller : 'ViewCertCtrl'
    }).when('/about.support', {
        templateUrl : '/views/dashboard/about.support.html',
        controller : 'AboutWebcertCtrl'
    }).when('/about.webcert', {
        templateUrl : '/views/dashboard/about.webcert.html',
        controller : 'AboutWebcertCtrl'
    }).otherwise({
        redirectTo : '/index'
    });

    // Add cache buster interceptor
    $httpProvider.interceptors.push('httpRequestInterceptorCacheBuster');
} ]);

// Global config of default date picker config (individual attributes can be
// overridden per directive usage)
angular.module('wcDashBoardApp').constant('datepickerPopupConfig', {
    dateFormat : "yyyy-MM-dd",
    closeOnDateSelection : true,
    appendToBody : false,
    showWeeks : true,
    closeText : 'OK',
    currentText : "Idag",
    toggleWeeksText : "Visa Veckor",
    clearText : "Rensa"
});

// Inject language resources
angular.module('wcDashBoardApp').run([ '$rootScope', 'messageService', function($rootScope, messageService) {
    $rootScope.lang = 'sv';
    $rootScope.DEFAULT_LANG = 'sv';
    $rootScope.MODULE_CONFIG = MODULE_CONFIG;
    messageService.addResources(webcertResources);
    messageService.addResources(commonMessageResources);
} ]);
