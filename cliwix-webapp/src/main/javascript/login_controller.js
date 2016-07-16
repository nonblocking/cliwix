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

    .controller('LoginController', function ($scope, $log, $location, loginService, errorService, backendService) {

        $scope.username = '';
        $scope.password = '';

        $scope.login = function() {
            if (!$scope.username || !$scope.password) return;

            $log.info('Login as ' + $scope.username);

            var data = {
              username: $scope.username,
              password: $scope.password
            };

            backendService.login(data).then(
                function(data) {
                    var result = data.loginResult;
                    if (result.succeeded) {
                        $scope.$emit('login', $scope.username);
                        loginService.gotoLastLocation();
                    } else {
                        $scope.$emit('globalMessage', { message: result.errorMessage, severity: 'error'});
                    }
                }, function(httpError) {
                    $log.error("Error login. Status: " + httpError.status);
                    errorService.processHttpError('error.login', httpError);
                }
            );
        };

    });