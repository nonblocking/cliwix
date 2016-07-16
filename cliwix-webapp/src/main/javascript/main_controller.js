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

    .controller('MainController', function ($scope, $location, $translate, $interval, backendService, appConfig) {

        $scope.liferayReady = appConfig.liferayFound && appConfig.liferaySupported && appConfig.liferayReady;

        $scope.errorMessageInterval = null;
        $scope.errorMessage = null;
        $scope.errorSeverity = null;

        $scope.username = appConfig.user;

        $scope.cliwixVersion = appConfig.version;
        $scope.cliwixBaseVersion = $scope.cliwixVersion.split('-')[0];

        $scope.isActive = function(route) {
            return route === $location.path();
        };

        $scope.showMenu = function() {
            return $scope.liferayReady && !$scope.isActive('/login');
        };

        $scope.setErrorMessage = function(message, severity) {
            $scope.errorMessage = message;
            $scope.errorSeverity = severity;

            if ($scope.errorMessageInterval !== null) {
                $interval.cancel($scope.errorMessageInterval);
            }
            $scope.errorMessageInterval = $interval($scope.clearMessages, 12000);
        };

        $scope.setErrorMessageKey = function(key, severity) {
            $translate(key)
                .then(function(message) {
                    $scope.setErrorMessage(message, severity);
                }, function() {
                    $scope.setErrorMessage(key, severity);
                });
        };

        $scope.clearMessages = function() {
            $scope.setErrorMessage(null, null);

            if ($scope.errorMessageInterval !== null) {
                $interval.cancel($scope.errorMessageInterval);
                $scope.errorMessageInterval = null;
            }
        };

        $scope.alertClass = function() {
          switch($scope.errorSeverity) {
              case 'success': return 'alert-success';
              case 'info': return 'alert-info';
              case 'warning': return 'alert-warning';
              default:
              case 'error': return 'alert-danger';
          }
        };

        $scope.$on('globalMessage', function(event, data) {
            if (data.hasOwnProperty('key')) {
                $scope.setErrorMessageKey(data.key, data.severity);
            } else {
                $scope.setErrorMessage(data.message, data.severity);
            }
        });

        $scope.$on('login', function(event, data) {
            $scope.username = data;
        });

        $scope.logout = function() {
            backendService.logout().then(
                function() {
                    $scope.username = "anonymous";
                    $location.path("/login");
                }
            );
        };

        $scope.setLanguage = function(lang) {
            $translate.use(lang);
        };

        $scope.copyrightYear = new Date().getFullYear();

        $scope.init = function() {
            if (!appConfig.liferayFound) {
                $scope.setErrorMessageKey('error.no.liferay.found', 'error');
            } else if (!appConfig.liferayReady) {
                $scope.setErrorMessageKey('error.liferay.not.ready', 'error');
            } else if (!appConfig.liferaySupported) {
                $scope.setErrorMessageKey('error.liferay.not.supported', 'error');
            }
        };

        $scope.init();

    });
