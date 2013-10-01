/*
 *  UnhandledQACtrl - Controller for logic related to listing questions and answers 
 * 
 */
angular.module('wcDashBoardApp').controller('UnhandledQACtrl',
        [ '$scope', '$window', '$log', '$timeout', '$filter', 'dashBoardService', function UnhandledCertCtrl($scope, $window, $log, $timeout, $filter, dashBoardService) {
            // init state
            $scope.widgetState = {
                doneLoading : false,
                hasError : false
            }

            $scope.qaList = {};
            $scope.activeUnit = "";

            // load all fragasvar for all units in usercontext
            $timeout(function() { // wrap in timeout to simulate latency -
                // remove soon
                dashBoardService.getQA(function(data) {
                    $scope.widgetState.doneLoading = true;
                    if (data != null) {
                        $scope.qaList = data;
                        $log.debug("got Data!");
                    } else {
                        $scope.widgetState.hasError = true;
                    }
                });
            }, 1000);

            $scope.setActiveUnit = function(unit) {
                $log.debug("ActiveUnit is now:" + unit);
                $scope.activeUnit = unit;
            }

            // Calculate how many entities we have for a specific enhetsId
            $scope.getItemCountForUnitId = function(unit) {
                if (!$scope.widgetState.doneLoading) {
                    return "?";
                }
                var count = $filter('QAEnhetsIdFilter')($scope.qaList, unit.id).length;

                return count;
            }

            $scope.openIntyg = function (intygsReferens) {
                $log.debug("open intyg " + intygsReferens.intygsId);
                $window.location.href = "/m/" + intygsReferens.intygsTyp.toLowerCase() + "/webcert/intyg/" + intygsReferens.intygsId;
            }

        } ]);