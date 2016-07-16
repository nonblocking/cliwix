/*
 * Copyright (c) 2014-2016
 * nonblocking.at gmbh [http://www.nonblocking.at]
 *
 * This file is part of Cliwix.
 *
 * Cliwix is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('cliwix')

    .controller('ImportController', function ($scope, $log, $modal, $interval, $translate, errorService, backendService, utils) {

        $scope.uploadProgress = -1;
        $scope.progressModal = null;
        $scope.overrideRootImportPolicy = false;

        $scope.importSettings = {
            mode: 'upload',
            directory: null,
            tmpFileName: '',
            atomicTransaction: false,
            simulationMode: false,
            overrideRootImportPolicy: null,
            ignoreNonExistingUsersGroupsOrgs: false,
            ignoreNonExistingResourceActions: false,
            ignoreInvalidDocumentReferences: false,
            ignoreDeletionFailures: false
        };

        $scope.startImport = function() {
            if ($scope.importSettings.mode === 'upload') {
                if ($scope.uploadFile === null) {
                    $scope.$emit('globalMessage', { key: 'error.upload.file.required', severity: 'error'});
                    return;
                }
            } else {
                if ($scope.importSettings.directory === null || $scope.importSettings.directory.length === 0) {
                    $scope.$emit('globalMessage', { key: 'error.directory.required', severity: 'error'});
                    return;
                }
            }

            $scope.$emit('globalMessage', { message: null, severity: null});

            if ($scope.importSettings.mode === 'upload') {
                $scope.importUploadFileAndPostSettings();
            } else {
                $scope.importPostSettings();
            }
        };

        $scope.importUploadFileAndPostSettings = function() {
            $scope.openProgressModal();

            backendService.uploadImportFile($scope.uploadFile).then(
                function (data) {
                    $scope.importSettings.tmpFileName = data.uploadResult.tmpFileName;
                    $scope.importPostSettings();
                }, function(httpError) {
                    $log.error("Failed to upload file. Status: " + httpError.status);
                    errorService.processHttpError('error.upload.failed', httpError);
                    $scope.uploadFile = null;
                    $scope.closeProgressModal();
                }, function (progress) {
                    $scope.uploadProgress = progress.progress;
                    $log.debug('Upload progress: ' + $scope.uploadProgress);
                }
            );
        };

        $scope.importPostSettings = function() {
            $scope.openProgressModal();

            backendService.startImport($scope.importSettings).then(
                function (result) {
                    $scope.$emit('globalMessage', { key: 'import.successfully.started', severity: 'success'});
                    $scope.uploadFile = null;
                    $scope.closeProgressModal();
                    $scope.refreshImportList();
                }, function(httpError) {
                    $log.error("Error starting import. Status: " + httpError.status);
                    errorService.processHttpError('error.starting.import', httpError);
                    $scope.uploadFile = null;
                    $scope.closeProgressModal();
                }
            );
        };

        $scope.openProgressModal = function() {
            if ($scope.progressModal === null) {
                $scope.uploadProgress = - 1;
                $scope.progressModal = $modal.open({
                    templateUrl: 'progressDialog.html',
                    scope: $scope,
                    backdrop: 'static',
                    keyboard: false
                });
            }
        };

        $scope.closeProgressModal = function() {
            if ($scope.progressModal !== null) {
                setTimeout(function () {
                        $scope.progressModal.close();
                        $scope.progressModal = null;
                    },
                    250);
            }
        };

        $scope.uploadFileLabel = function() {
            if ($scope.uploadFile !== null) {
                return $scope.uploadFile.name;
            } else {
                return $translate.instant("no.file.selected");
            }
        };

        $scope.uploadFile = null;

        $scope.onFileSelect = function(files) {
            if (angular.isUndefined(files) || files === null) {
                return;
            }

            var file = files[0];
            if (angular.isUndefined(files)) {
                return;
            }

            var fileName = file.name.toLowerCase();
            if (utils.endsWith(fileName, '.xml') || utils.endsWith(fileName, '.zip')) {
                $scope.uploadFile = file;
            } else {
                $scope.uploadFile = null;
                $scope.$emit('globalMessage', { key: 'error.upload.file.type', severity: 'error'});
            }
        };

        $scope.importListRefreshInterval = null;
        $scope.importListCurrentPage = 1;
        $scope.importListTotal = 0;
        $scope.importList = [];

        $scope.pageChanged = function() {
            //$log.debug("pageChanged");
            $scope.refreshImportList();
        };

        $scope.stateToClass = function(state) {
            switch (state) {
                case 'failed': return 'danger';
                case 'processing': return 'info';
                default: return state;
            }
        };

        $scope.refreshImportList = function() {
            backendService.getImportList(($scope.importListCurrentPage - 1) * 10, 10).then(
                function(data) {
                    $scope.importListTotal = data.imports.total;
                    $scope.importList = data.imports.list;
                }, function(httpError) {
                    $log.error("Failed to fetch import list. Status: " + httpError.status);
                    errorService.processHttpError('error.couldnt.reach.server', httpError);
                    //$interval.cancel($scope.importListRefreshInterval);
                }
            );
        };

        $scope.deleteImport = function(importId) {
            $log.info("Deleting import: " + importId);
            backendService.deleteImport(importId).then(
                function(data) {
                    $scope.refreshImportList();
                }, function(httpError) {
                    $log.error("Failed to delete import. Status: " + httpError.status);
                    errorService.processHttpError('error.deleting.import', httpError);
                }
            );
        };

        $scope.init = function() {
            $scope.importListCurrentPage = 1;
            $scope.importListTotal = 0;
            $scope.importList = [];
            $scope.refreshImportList();
            $scope.importListRefreshInterval = $interval($scope.refreshImportList, 10000);
        };

        $scope.init();

        $scope.$on('$destroy', function() {
           if ($scope.importListRefreshInterval !== null) {
               $interval.cancel($scope.importListRefreshInterval);
           }
        });
    });
