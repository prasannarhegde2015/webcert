/**
 * Common directives used in both WC main application as well as in a certificate's module app pages. 
 * Since this js will be used/loaded from different contextpaths, all templates are inlined. PLEASE keep source 
 * formatting in this file as-is, otherwise the inline templates will be hard to follow. 
 */
angular.module('wc.common.directives', []);
angular.module('wc.common.directives').directive("wcHeader", ['$rootScope', function($rootScope) {
    return {
        restrict : "A",
        replace : true,
        scope : {
          userName: "@",
          caregiverName: "@",
          isDoctor: "@"
        },
        controller: function($scope, $element, $attrs) {
            //Expose "now" as a model property for the template to render as todays date
            $scope.today = new Date();
        },
        template:
        	'<div>'
        	+'<div class="row-fluid">'
        		+'<div class="span2">'
        			+'<div class="headerbox-logo"><a href="/web/start"><img alt="Till startsidan" src="/img/webcert_logo.png"/></a></div>'
        		+'</div>'
        		+'<div class="span4 headerbox-date">'
        			+'{{today | date:"shortDate"}}'
        		+'</div>'
        		+'<div class="span5 headerbox-user">'
        			+'<div class="span2"><img src="/img/avatar.png"/></div>'
        			+'<div class="span10" ng-show="userName.length">'
                        +'<span ng-switch="isDoctor">'
                        +'<strong ng-switch-when="true">Läkare</strong>'
                        +'<strong ng-switch-default>Admin</strong>'
                        +'</span>'
        				+' - <span class="logged-in">{{userName}}</span><br>'
        				+'<span class="location">{{caregiverName}}</span>'
        			+'</div>'
        		+'</div>'
	        	+'<div class="span1">'
		    			+'<div class="dropdown pull-right">'
		    				+'<a class="dropdown-toggle settings" data-toggle="dropdown" href="#"></a>'
		    				+'<ul class="dropdown-menu dropdown-menu-center" role="menu" aria-labelledby="dLabel">'
		    					+'<li><a tabindex="-1" href="#">Hjälp</a></li>'
		    					+'<li><a tabindex="-1" href="#">Logga ut</a></li>'
		    				+'</ul>'
		    			+'</div>'				
	    			+'</div>'
    			+'</div>'
    		+'</div>'  
    }
} ]);


angular.module('wc.common.directives').directive("wcSpinner", ['$rootScope', function($rootScope) {
    return {
        restrict : "A",
        transclude : true,
        replace : true,
        scope : {
          label: "@",
          showSpinner: "=",
          showContent: "="
        },
        template :
            '<div>'
           +'  <div ng-show="showSpinner" style="text-align: center; padding: 20px;">'
           +'    <img aria-labelledby="loading-message" src="/img/ajax-loader.gif" style="text-align: center;" />'
           +'    <p id="loading-message" style="text-align: center; color: #64ABC0; margin-top: 20px;">'
           +'      <strong><span message key="{{ label }}"></span></strong>'
           +'    </p>'
           +'  </div>'
           +'  <div ng-show="showContent">'
           +'    <div ng-transclude></div>'
           +'  </div>'
           +'</div>'
    }
} ]);