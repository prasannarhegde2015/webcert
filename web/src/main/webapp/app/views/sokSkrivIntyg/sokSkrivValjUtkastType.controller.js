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
angular.module('webcert').controller('webcert.ChooseCertTypeCtrl',
    ['$window', '$filter', '$log', '$scope', '$stateParams', '$state', '$location',
        'webcert.SokSkrivIntygViewstate', 'webcert.IntygTypeSelectorModel', 'common.PatientModel',
        'common.IntygCopyActions', 'common.IntygFornyaRequestModel', 'common.IntygCopyRequestModel',
        'webcert.IntygProxy', 'webcert.UtkastProxy', 'webcert.SokSkrivValjUtkastService', 'common.ObjectHelper',
        'common.messageService',

        function($window, $filter, $log, $scope, $stateParams, $state, $location,
            Viewstate, IntygTypeSelectorModel, PatientModel,
            CommonIntygCopyActions, IntygFornyaRequestModel, IntygCopyRequestModel,
            IntygProxy, UtkastProxy, Service, ObjectHelper, messageService) {
            'use strict';

            /**
             * Page state
             */

            var choosePatientStateName = 'webcert.create-choosepatient-index';

            $scope.intygTypeModel = IntygTypeSelectorModel.build();
            $scope.messageService = messageService;
            $scope.viewState = Viewstate.build();

            // In case callers do not know the patientId they can use 'default' in which case the controller
            // will use what's currently in PatientModel, or, if that's not available, redirect user to enter a
            // new id on the choose patient screen.
            $scope.patientModel = Service.setupPatientModel(PatientModel, $stateParams.patientId);
            onPageLoad();

            /**
             * Private functions
             * @private
             */

            function onPageLoad() {

                if (ObjectHelper.isEmpty(PatientModel.personnummer)) {
                    $state.go('webcert.create-choosepatient-index');
                    return;
                }

                if (PatientModel.isValid()) {
                    // All is well just load the rest
                    loadUtkastTypesAndIntyg();
                } else {
                    // PatientModel is missing name information. Load that first
                    Viewstate.patientLoading = true;
                    Service.lookupPatient(PatientModel.personnummer).then(function(patientResult) {

                        Viewstate.loadErrorMessageKey = null;
                        Viewstate.patientLoading = false;

                        // Redirect to index if pnr and name still isn't specified
                        if (!PatientModel.update(patientResult)) {
                            $state.go(choosePatientStateName);
                            return;
                        }

                        loadUtkastTypesAndIntyg();

                    }, function(errorId) {
                        Viewstate.loadErrorMessageKey = errorId;
                        Viewstate.patientLoading = false;

                        if (errorId === null) {
                            // If the pu-service isn't available the doctor can write any name they want.
                            // redirect to edit patient name
                            $state.go('webcert.create-edit-patientname', {mode: 'errorOccured'});
                        }
                    });
                }
            }

            function loadUtkastTypesAndIntyg() {
                // Load intyg types user can choose from
                UtkastProxy.getUtkastTypes(function(types) {
                    IntygTypeSelectorModel.intygTypes = types;
                });

                // Load intyg for person with specified pnr
                Viewstate.tidigareIntygLoading = true;
                IntygProxy.getIntygForPatient(PatientModel.personnummer, function(data) {
                    Viewstate.intygListUnhandled = data;
                    $scope.updateIntygList();
                    Viewstate.unsigned = Service.hasUnsigned(Viewstate.currentList);
                    Viewstate.tidigareIntygLoading = false;
                }, function(errorData, errorCode) {
                    Viewstate.tidigareIntygLoading = false;
                    $log.debug('Query Error' + errorData);
                    Viewstate.intygListErrorMessageKey = errorCode;
                });
            }


            $scope.isCopyAllowed = function(intyg) {
                return !(intyg.intygsTyp === 'ts-bas' || intyg.intygsTyp ==='ts-diabetes') &&
                    !$scope.patientModel.sekretessmarkering && intyg.status !== 'CANCELLED' &&
                    !(intyg.relations.latestChildRelations.replacedByIntyg ||
                    intyg.relations.latestChildRelations.complementedByIntyg);
            };

            /**
             * Watches
             */
            $scope.$watch('viewState.intygFilter', function() {
                $scope.updateIntygList();
            });

            /*
            $scope.$watch('current.selected', function(newValue, oldValue) {
                if (newValue !== oldValue) {
                    $scope.intygType = newValue;
                }
            });*/

            /**
             * Exposed to scope
             */

            $scope.updateIntygList = function() {
                Viewstate.currentList =
                    $filter('TidigareIntygFilter')(Viewstate.intygListUnhandled, Viewstate.intygFilter);
            };

            $scope.changePatient = function() {
                $state.go(choosePatientStateName);
            };

            //Use loaded module metadata to look up detailed description for a intygsType
            $scope.getDetailedDescription = function(intygsType) {
                var intygTypes = IntygTypeSelectorModel.intygTypes.filter(function(intygType) {
                    return (intygType.id === intygsType);
                });
                if (intygTypes && intygTypes.length > 0) {
                    return intygTypes[0].detailedDescription;
                }
            };

            //Use loaded module metadata to look up name for a intygsType
            $scope.getTypeName = function(intygsType) {
                var intygTypes = IntygTypeSelectorModel.intygTypes.filter(function(intygType) {
                    return (intygType.id === intygsType);
                });
                if (intygTypes && intygTypes.length > 0) {
                    return intygTypes[0].label;
                }
            };

            $scope.createDraft = function() {

                var createDraftRequestPayload = {
                    intygType: IntygTypeSelectorModel.intygType,
                    patientPersonnummer: PatientModel.personnummer,
                    patientFornamn: PatientModel.fornamn,
                    patientMellannamn: PatientModel.mellannamn,
                    patientEfternamn: PatientModel.efternamn,
                    patientPostadress: PatientModel.postadress,
                    patientPostnummer: PatientModel.postnummer,
                    patientPostort: PatientModel.postort
                };
                UtkastProxy.createUtkast(createDraftRequestPayload, function(data) {
                    Viewstate.createErrorMessageKey = undefined;
                    $location.url('/' + createDraftRequestPayload.intygType + '/edit/' + data.intygsId, true);
                }, function(error) {
                    $log.debug('Create draft failed: ' + error.message);
                    Viewstate.createErrorMessageKey = 'error.failedtocreateintyg';
                });
            };

            $scope.openIntyg = function(intyg) {
                if (intyg.status === 'DRAFT_INCOMPLETE' || intyg.status === 'DRAFT_COMPLETE') {
                    $location.path('/' + intyg.intygType + '/edit/' + intyg.intygId);
                } else {
                    $location.path('/intyg/' + intyg.intygType + '/' + intyg.intygId);
                }
            };

            // $scope.copyIntyg = function(intyg) {
            //     Viewstate.createErrorMessageKey = null;
            //
            //     // We don't have the required info about issuing unit in the supplied 'intyg' object, always set to true.
            //     // It only affects a piece of text in the Kopiera-dialog anyway.
            //     var isOtherCareUnit = true;
            //
            //     CommonIntygCopyActions.copy(Viewstate,
            //         IntygCopyRequestModel.build({
            //             intygId: intyg.intygId,
            //             intygType: intyg.intygType,
            //             patientPersonnummer: PatientModel.personnummer,
            //             nyttPatientPersonnummer: null
            //         }),
            //         isOtherCareUnit
            //     );
            // };

            $scope.fornyaIntyg = function(intyg) {
                Viewstate.createErrorMessageKey = null;

                // We don't have the required info about issuing unit in the supplied 'intyg' object, always set to true.
                // It only affects a piece of text in the Kopiera-dialog anyway.
                var isOtherCareUnit = true;

                CommonIntygCopyActions.fornya(Viewstate,
                    IntygFornyaRequestModel.build({
                        intygId: intyg.intygId,
                        intygType: intyg.intygType,
                        patientPersonnummer: PatientModel.personnummer,
                        nyttPatientPersonnummer: null
                    }),
                    isOtherCareUnit
                );
            };

            $scope.resolveTooltipText = function(intyg) {
                return messageService.getProperty(intyg.intygType + '.fornya.tooltip');
            }
        }]);
