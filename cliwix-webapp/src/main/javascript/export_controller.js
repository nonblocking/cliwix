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

    .controller('ExportController', function ($scope, $log, $interval, $timeout, errorService, backendService) {

        $scope.exportAllCompanies = true;
        $scope.exportAllSites = true;
        $scope.exportOnlyFileDataLastModifiedWithinDaysEnabled = false;

        $scope.exportSettings = {
            companyFilter: null,
            exportPortalInstanceConfiguration: true,
            siteFilter: null,
            exportSiteConfiguration: true,
            exportUsers: true,
            exportUserGroups: true,
            exportRoles: true,
            exportOrganizations: true,
            exportPages: true,
            exportWebContent: true,
            exportDocumentLibrary: true,
            skipCorruptDocuments: false,
            exportOnlyFileDataLastModifiedWithinDays: null
        };

        $scope.exportButtonDisabled = false;

        $scope.startExport = function() {
            $scope.exportButtonDisabled = true;
            $timeout(function() {  $scope.exportButtonDisabled = false; $scope.$digest(); }, 1000);

            backendService.startExport($scope.exportSettings).then(
                function(result) {
                    $scope.$emit('globalMessage', { key: 'export.successfully.started', severity: 'success'});
                    $scope.refreshExportList();
                }, function(httpError) {
                    $log.error("Error starting export. Status: " + httpError.status);
                    errorService.processHttpError('error.starting.export', httpError);
                }
            );
        };

        $scope.exportListRefreshInterval = null;
        $scope.exportListCurrentPage = 1;
        $scope.exportListTotal = 0;
        $scope.exportList = [];

        $scope.pageChanged = function() {
            $scope.refreshExportList();
        };

        $scope.stateToClass = function(state) {
          switch (state) {
              case 'failed': return 'danger';
              case 'processing': return 'info';
              default: return state;
          }
        };

        $scope.refreshExportList = function() {
            backendService.getExportList(($scope.exportListCurrentPage - 1) * 10, 10).then(
                function(data) {
                    $scope.exportListTotal = data.exports.total;
                    $scope.exportList = data.exports.list;
                }, function(httpError) {
                    $log.error("Failed to fetch export list. Status: " + httpError.status);
                    errorService.processHttpError('error.couldnt.reach.server', httpError);
                    //$interval.cancel($scope.exportListRefreshInterval);
                }
            );
        };

        $scope.deleteExport = function(exportId) {
            $log.info("Deleting export: " + exportId);
            backendService.deleteExport(exportId).then(
                function(data) {
                    $scope.refreshExportList();
                }, function(httpError) {
                    $log.error("Failed to delete export. Status: " + httpError.status);
                    errorService.processHttpError('error.deleting.export', httpError);
                }
            );
        };

        $scope.init = function() {
            $scope.exportListCurrentPage = 1;
            $scope.exportListTotal = 0;
            $scope.exportList = [];
            $scope.refreshExportList();
            $scope.exportListRefreshInterval = $interval($scope.refreshExportList, 10000);
        };

        $scope.init();

        $scope.$on('$destroy', function() {
            if ($scope.exportListRefreshInterval !== null) {
                $interval.cancel($scope.exportListRefreshInterval);
            }
        });

    });
