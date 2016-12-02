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

/**
 * Created by stephenwhite on 31/08/15.
 */
angular.module('webcert.pub.login', ['ui.bootstrap'])
    .controller('LoginController', ['$scope', '$sce', '$uibModal', '$window', function($scope, $sce, $uibModal, $window) {
        'use strict';
        var expand = $sce.trustAsHtml('Visa mer om inloggning <span class="glyphicon glyphicon-chevron-down"></span>');
        var collapse = $sce.trustAsHtml('Visa mindre om inloggning <span class="glyphicon glyphicon-chevron-up"></span>');
        $scope.collapseLoginDesc = true;
        $scope.loginDescText = expand;
        $scope.toggleLoginDesc = function(evt){
            evt.preventDefault();
            $scope.collapseLoginDesc = !$scope.collapseLoginDesc;
            if($scope.collapseLoginDesc){
                $scope.loginDescText = expand;
            } else {
                $scope.loginDescText = collapse;
            }
        };

        $scope.showELegWarning = (function () {
            var re = /(?:Chrome\/\d+)|(?:Edge\/\d+)/;
            var userAgent = $window.navigator.userAgent;
            return re.test(userAgent);
        }());

        $scope.open = function (which) {

            $scope.modalInstance = $uibModal.open({
                templateUrl: which,
                scope: $scope,
                size: 'lg'
            });

            $scope.modalInstance.result.then(function (selectedItem) {
                $scope.selected = selectedItem;
            }, function () {
                // closed the modal
            });
        };

        $scope.ok = function () {
            $scope.modalInstance.close();
        };

        $scope.toggleCookie = function(evt) {
            evt.preventDefault();
            $scope.showCookieText = !$scope.showCookieText;
        };

        $scope.afterExpand = function() {
            $window.scrollTo(0,document.body.scrollHeight);
        };

    }]);
