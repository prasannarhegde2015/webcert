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

angular.module('webcert').controller('webcert.AboutWebcertCtrl',
    ['$rootScope', '$scope', '$log', 'common.fragaSvarCommonService', 'webcert.UtkastProxy',
        function($rootScope, $scope, $log, fragaSvarCommonService, UtkastProxy) {
            'use strict';

            var unbindLocationChange = $rootScope.$on('$locationChangeStart', function($event, newUrl, currentUrl) {
                fragaSvarCommonService.checkQAonlyDialog($scope, $event, newUrl, currentUrl, unbindLocationChange);
            });
            $scope.$on('$destroy', unbindLocationChange);

            function loadIntygTypes() {
                $scope.intygTypes = [];
                UtkastProxy.getUtkastTypesCachedUnfiltered(function(types) {
                    $scope.intygTypes = types;
                });
            }
            loadIntygTypes();

            $scope.getDetailedDescription = function(intygsType) {
                return intygsType.detailedDescription;
            };
        }]
);
