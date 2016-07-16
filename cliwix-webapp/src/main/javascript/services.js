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

    .factory('backendService', function($http, cliwixBackend, dummyBackend, appConfig) {
        if (appConfig.standalone) {
            return dummyBackend;
        }
        return cliwixBackend;
    })

    .service('cliwixBackend', function($log, $q, $http, Upload) {

        this.startExport = function(exportSettings) {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/exports',
                    method: 'POST',
                    data: exportSettings
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
                });
        };

        this.getExportList = function(start, limit) {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/exports?limit=' + limit + '&start=' + start,
                    method: 'GET'
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
                });
        };

        this.deleteExport = function(exportId) {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/exports/' + exportId,
                    method: 'DELETE'
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
            });
        };

        this.uploadImportFile = function(file) {
            var deferred = $q.defer();

            Upload.upload({
                url: 'services/uploads',
                method: 'POST',
                file: file
            }).then(
                function(resp) {
                    deferred.resolve(resp.data);
                }, function(resp) {
                    deferred.reject(resp);
                }, function(evt) {
                    var percent = parseInt(100.0 * evt.loaded / evt.total);
                    deferred.notify({
                        progress: percent
                    });
                });

            return deferred.promise;
        };

        this.startImport = function(importSettings) {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/imports',
                    method: 'POST',
                    data: importSettings
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
                });
        };

        this.getImportList = function(start, limit) {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/imports?limit=' + limit + '10&start=' + start,
                    method: 'GET'
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
                });
        };

        this.deleteImport = function(importId) {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/imports/' + importId,
                    method: 'DELETE'
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
            });
        };

        this.login = function(userData) {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/login',
                    method: 'POST',
                    data: userData
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
            });
        };

        this.logout = function() {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/logout',
                    method: 'GET'
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
            });
        };

        this.getInfo = function() {
            return $q(function (resolve, reject) {
                $http({
                    url: 'services/info',
                    method: 'GET',
                    cache: true
                }).then(
                    function(resp) {
                        resolve(resp.data);
                    }, function(resp) {
                        reject(resp);
                    });
            });
        };
    })

    .service('dummyBackend', function($log, $q, $timeout) {

        this.startExport = function(exportSettings) {
            return $q(function (resolve, reject) {
               resolve({
                   exportId: "ABCDEFGHIJKLM"
               });
            });
        };

        this.getExportList = function(start, limit) {
            return $q(function (resolve, reject) {
                var exports = {
                    total: 55,
                    start: start,
                    list: []
                };
                for (var i = 0; i < limit; i++) {
                    exports.list.push({
                        id: 121322221231231 + i * 500000,
                        user: 'user@user.at',
                        client: 'Web Client',
                        clientIP: '127.0.0.1',
                        state: Math.random() > 0.5 ? 'success' : 'failed',
                        errorMessage: 'Some error',
                        reportExists: true,
                        durationMs: 1231231
                    });
                }

                resolve({
                    exports: exports
                });
            });
        };

        this.deleteExport = function(exportId) {
            return $q(function (resolve, reject) {
                resolve();
            });
        };

        this.uploadImportFile = function(file) {
            return $q(function (resolve, reject) {
                $timeout(function() {
                    resolve({
                        uploadResult: {
                            tmpFileName: 'test.tmp'
                        }
                    });
                }, 4000);
            });
        };

        this.startImport = function(importSettings) {
            return $q(function (resolve, reject) {
                resolve({
                    importId: "ABCDEFGHIJKLM"
                });
            });
        };

        this.getImportList = function(start, limit) {
            return $q(function (resolve, reject) {
                var imports = {
                    total: 55,
                    start: start,
                    list: []
                };
                for (var i = 0; i < limit; i++) {
                    imports.list.push({
                        id: 121322221231231 + i * 500000,
                        user: 'user@user.at',
                        client: 'Web Client',
                        clientIP: '127.0.0.1',
                        state: Math.random() > 0.5 ? 'success' : 'failed',
                        errorMessage: 'Some error',
                        reportExists: true,
                        durationMs: 1231231
                    });
                }

                resolve({
                    imports: imports
                });
            });
        };

        this.deleteImport = function(importId) {
            return $q(function (resolve, reject) {
                resolve();
            });
        };

        this.login = function(userData) {
            return $q(function (resolve, reject) {
                resolve({
                    loginResult: {
                        succeeded: true
                    }
                });
            });
        };

        this.logout = function() {
            return $q(function (resolve, reject) {
                resolve();
            });
        };

        this.getInfo = function() {
            return $q(function (resolve, reject) {
                resolve({
                    info: {
                        cliwixVersion: '1.2.2',
                        liferayRelease: 'TEST',
                        cliwixWorkspaceDirectory: '/test'
                    }
                });
            });
        };
    })

    .service('errorService', function($rootScope, $translate, loginService) {

        this.processHttpError = function(messageKey, httpError) {
            $translate(messageKey).then(function (msg) {

                //Login required
                if (httpError.status === 401)  {
                    loginService.gotoLogin();
                    return;
                }

                var details = '';

                if (typeof httpError.data === 'string' && httpError.data.length > 0) {
                    details = ' (' + httpError.data + ')';
                } else if (httpError.data.hasOwnProperty('error')) {
                    details = ' (' + httpError.data.error.message + ')';
                }

                $rootScope.$broadcast('globalMessage', { message: msg + details, severity: 'error'});
            });
        };
    })

    .service('loginService', function($rootScope, $location, $log) {
        this.lastLocation = '/export';

        this.gotoLogin = function() {
            $rootScope.$broadcast('globalMessage', { message: null, severity: null });
            if ($location.path() !== '/login') {
                this.lastLocation = $location.path();
            } else {
                this.lastLocation = '/export';
            }

            $log.info('Authentication required. Redirect to /login');
            $location.path('/login');
        };

        this.gotoLastLocation = function() {
            $log.info('Login successful. Redirect to ' + this.lastLocation);
            $location.path(this.lastLocation);
        };

    });




