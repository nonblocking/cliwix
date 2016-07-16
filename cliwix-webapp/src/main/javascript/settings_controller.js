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

    .controller('SettingsController', function ($window, $scope, $log, errorService, backendService, appConfig) {

        $scope.info =  {
            cliwixVersion: '',
            cliwixRelease: '',
            cliwixWorkspaceDirectory: ''
        };

        $scope.init = function() {
            backendService.getInfo().then(
                function (data) {
                    $scope.info = data.info;
                }, function (httpError) {
                    $log.error("Failed to fetch info. Status: " + httpError.status);
                    errorService.processHttpError('error.couldnt.reach.server', httpError);
                }
            );
        };

        $scope.init();

    });