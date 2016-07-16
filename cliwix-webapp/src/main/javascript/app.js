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

angular.module('cliwix', ['ngRoute', 'pascalprecht.translate', "ui.bootstrap", "ngFileUpload", "ngSanitize"])

    .config(function ($routeProvider, appConfig) {
        if (appConfig.liferayReady) {
            $routeProvider
                .when('/export', {
                    templateUrl: 'templates/export.html?v' + appConfig.version,
                    controller: 'ExportController'
                })
                .when('/import', {
                    templateUrl: 'templates/import.html?v' + appConfig.version,
                    controller: 'ImportController'
                })
                .when('/settings', {
                    templateUrl: 'templates/settings.html?v' + appConfig.version,
                    controller: 'SettingsController'
                })
                .when('/help', {
                    templateUrl: 'templates/help.html?v' + appConfig.version
                })
                .when('/login', {
                    templateUrl: 'templates/login.html?v' + appConfig.version,
                    controller: 'LoginController'
                })
                .otherwise({
                    redirectTo: '/export'
                });
        } else {
            //Nothing
            $routeProvider
                .otherwise({
                    redirectTo: '/'
                });
        }
    })

    .config(function ($translateProvider) {
        $translateProvider.useSanitizeValueStrategy(null);
    })

    .run(function($http) {
        $http.defaults.headers.common.CliwixClient = "Web Client";
    });