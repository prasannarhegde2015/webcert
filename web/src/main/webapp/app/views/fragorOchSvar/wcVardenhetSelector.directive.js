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

angular.module('webcert').directive('wcCareUnitClinicSelector',
    [ '$cookies', '$rootScope', '$timeout', 'common.User', 'common.statService',
        function($cookies, $rootScope, $timeout, User, statService) {
            'use strict';

            return {
                restrict: 'A',
                transclude: false,
                replace: true,
                templateUrl: '/app/views/fragorOchSvar/wcVardenhetSelector.directive.html',
                controller: function($scope) {

                    $scope.units = User.getVardenhetFilterList(User.getValdVardenhet());
                    $scope.units = $scope.units.slice(0, 1)
                        .concat($scope.units.slice(1, $scope.units.length).sort(
                                    function(a, b) {
                                        return (a.namn > b.namn) - (a.namn < b.namn);
                    }));
                    $scope.units.unshift({id: 'wc-all', namn: 'Alla frågor och svar'});
                    $scope.selectedUnit = null;

                    /**
                     * Toggles if the enheter without an active question should
                     * be shown
                     */
                    $scope.toggleShowInactive = function() {
                        $scope.showInactive = !$scope.showInactive;
                    };

                    function _updateStats(event, message) {
                        // Get the latest stats
                        var unitStats = message;

                        // Get the chosen vardgivare
                        var valdVardgivare = User.getValdVardgivare();

                        // Find stats for the chosen vardenhets units below the chosen vardgivare
                        var valdVardenheterStats = {};
                        angular.forEach(unitStats.vardgivare, function(vardgivareStats) {
                            if (vardgivareStats.id === valdVardgivare.id) {
                                valdVardenheterStats = vardgivareStats.vardenheter;
                            }
                        });

                        // Set stats for each unit available for the filter
                        angular.forEach($scope.units, function(unit) {

                            // If it's the all choice, we know we want the total of everything
                            if (unit.id === 'wc-all') {
                                unit.fragaSvar = unitStats.fragaSvarValdEnhet;
                                unit.tooltip =
                                    'Totalt antal ej hanterade frågor och svar för den vårdenhet där du är inloggad. ' +
                                    'Här visas samtliga frågor och svar på vårdenhetsnivå och på mottagningsnivå.';
                            } else {
                                // Otherwise find the stats for the unit
                                angular.forEach(valdVardenheterStats, function(unitStat) {
                                    if (unit.id === unitStat.id) {
                                        unit.fragaSvar = unitStat.fragaSvar;
                                        unit.tooltip =
                                            'Det totala antalet ej hanterade frågor och svar som finns registrerade på ' +
                                            'vårdenheten. Det kan finnas frågor och svar som gäller denna vårdenhet men ' +
                                                'som inte visas här. För säkerhets skull bör du även kontrollera frågor ' +
                                                'och svar för övriga vårdenheter och mottagningar.';
                                    }
                                });
                            }
                        });
                    }

                    if (statService.getLatestData()) {
                        _updateStats(null, statService.getLatestData());
                    }
                    $scope.$on('wc-stat-update', _updateStats);

                    $scope.selectUnit = function(unit) {
                        $scope.selectedUnit = unit;
                        $rootScope.$broadcast('qa-filter-select-care-unit', $scope.selectedUnit);
                    };

                    // Local function getting the first care unit's hsa id in the data struct.
                    function selectFirstUnit(units) {
                        if (typeof units === 'undefined' || units.length === 0) {
                            return null;
                        } else {
                            return units[0];
                        }
                    }

                    function selectUnitById(units, unitName) {
                        for (var count = 0; count < units.length; count++) {
                            if (units[count].id === unitName) {
                                return units[count];
                            }
                        }
                        return selectFirstUnit(units);
                    }

                    //initial selection, now handles cases when no enhetsId cookie has been set.
                    if ($scope.units.length > 2 && $cookies.getObject('enhetsId')) {
                        $scope.selectUnit(selectUnitById($scope.units, $cookies.getObject('enhetsId')));
                    } else {
                        $scope.selectUnit(selectFirstUnit($scope.units));
                    }
                }
            };
        }]);
