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

    .directive('onEnter', function () {
        return function (scope, element, attrs) {
            element.bind("keydown keypress", function (event) {
                if(event.which === 13) {
                    scope.$apply(function (){
                        scope.$eval(attrs.onEnter);
                    });

                    event.preventDefault();
                }
            });
        };
    });




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

    .factory('utils', function() {
        return {
            endsWith: function (string, suffix) {
                return string.indexOf(suffix, string.length - suffix.length) != -1;
            }
        };
    })

    .filter('minutesAndSeconds', function($translate) {
       return function(input) {
            if (typeof input === 'number' && input > 0) {
                var sec = Math.floor(input / 1000);
                var min = Math.floor(sec / 60);
                sec = sec - min * 60;

                return $translate.instant('time.pattern', { min: min, sec: sec });
            } else {
                return '';
            }
       };
    })

    .filter('i18nDate', function($translate, $filter) {
        return function(input) {
            if (typeof input === 'number') {
                var pattern = $translate.instant('date.pattern');
                return $filter('date')(input, pattern);
            } else {
                return '';
            }
        };
    });






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

    .config(function ($translateProvider) {
        $translateProvider.translations('en', {
            'date.pattern': 'MM/dd/yy hh:mm a',
            'time.pattern': '{{min}} min, {{sec}} sec',

            'download.cli.client': 'Download the command line client',
            'download.api': 'Download the Java API',
            'export': 'Export',
            'import': 'Import',
            'settings': 'Settings',
            'help': 'Help',
            'server.information': 'Server Information',
            'liferay.release': 'Liferay Release',
            'cliwix.version': 'Cliwix Version',
            'cliwix.workspace.directory': 'Cliwix Worspace Directory',
            'export.exportAllCompanies': 'All Companies',
            'export.companyFilter': 'Filter',
            'export.companyFilter.tooltip': 'Comma separated list of Company webIds, may contain wildcards (*)',
            'export.exportAllSites': 'All Sites',
            'export.siteFilter': 'Filter',
            'export.siteFilter.tooltip': 'Comma separated list of Sites names, may contain wildcards (*)',
            'export.roles': 'Roles',
            'export.userGroups': 'User Groups',
            'export.organizations': 'Organizations',
            'export.users': 'Users',
            'export.exportPortalInstanceConfiguration': 'Portal Instance Configuration',
            'export.exportPortalInstanceConfiguration.tooltip': 'General portal instance settings such as the virtual host and the LDAP configuration',
            'export.exportSiteConfiguration': 'Site Configuration',
            'export.exportSiteConfiguration.tooltip': 'General site setting such as the friendly URL',
            'export.pages': 'Pages',
            'export.webContent': 'Web Content',
            'export.documentLibrary': 'Document Library',
            'export.skipCorruptDocuments': 'Skip corrupt documents',
            'export.skipCorruptDocuments.tooltip': 'Ignore documents in the library which cannot be exported',
            'export.exportOnlyFileDataLastModifiedWithinDays1': 'Export only data modified within the last',
            'export.exportOnlyFileDataLastModifiedWithinDays2': 'days',
            'export.exportOnlyFileDataLastModifiedWithinDays.tooltip': 'Export only file data of files that have been modified within the given day range. ONLY for periodical imports.',
            'export.start': 'Start Export',
            'export.successfully.started': 'Export successfully started.',
            'export.list': 'Exports',
            'close': 'Close',
            'import.server.directory': 'Import from a server directory',
            'import.server.directory.tooltip': 'The directory must contain a valid liferay-config.xml file',
            'import.upload.file': 'Upload a single XML file or a ZIP archive',
            'import.upload.file.tooltip': 'The ZIP archive must contain a valid liferay-config.xml file',
            'import.atomicTransaction': 'Atomic transaction',
            'import.atomicTransaction.tooltip': 'Execute the import within a single atomic transaction',
            'import.simulationMode': 'Simulate Import (no DB commit)',
            'import.overrideRootImportPolicy': 'Override root import policy in XML:',
            'import.ignoreNonExistingUsersGroupsOrgs': 'Ignore non existing Users/Groups/Organizations',
            'import.ignoreNonExistingResourceActions': 'Skip non existing resource actions',
            'import.ignoreNonExistingResourceActions.tooltip': 'Useful if some portlets are configured but not deployed yet',
            'import.ignoreInvalidDocumentReferences': 'Ignore non existing document references',
            'import.ignoreInvalidDocumentReferences.tooltip': 'Ignore references in articles and other content that cannot be resolved',
            'import.ignoreDeletionFailures': 'Ignore deletion failures',
            'import.ignoreDeletionFailures.tooltip': 'Useful if some configuration entities are still referred by ones not managed by Cliwix',
            'import.start': 'Start Import',
            'import.starting': "Starting import...",
            'import.upload': "Upload",
            'import.successfully.started': 'Import successfully started.',
            'import.list': 'Imports',
            'info.cliwixWorkspaceDirectory.tooltip': 'Can be changed by setting the environment variable CLIWIX_WORKSPACE',
            'browse.file': "Select",
            'no.file.selected': "No file selected",
            'refreshing': 'Refreshing...',
            'timestamp': 'Start Time',
            'duration': 'Duration',
            'user': 'User',
            'client': 'Client',
            'client.ip': 'Client IP Address',
            'state': 'State',
            'success': 'Success',
            'failed': 'Failed',
            'processing': 'Processing',
            'download.report': 'Report',
            'download.export.data': 'Exported Data',
            'page.previous': 'Previous Page',
            'page.next': 'Next Page',
            'job.duration': '{{min}} min %i {{sec}}',
            'login.title': 'Login (as Liferay OmniAdmin)',
            'login.username': 'Username',
            'login.password': 'Password',
            'login.login': 'Login',
            'logout': 'Logout',
            'help.manual': 'Online Manual for Cliwix',
            'help.online': 'Online Resources',
            'delete': 'Delete',

            'error.message': 'Error Message',
            'error.couldnt.reach.server': 'Couldn\'t reach server!',
            'error.no.liferay.found': 'No Liferay on this server found!',
            'error.liferay.not.ready': 'Liferay is not ready yet. Please refresh in a few minutes.',
            'error.liferay.not.supported': 'Liferay version not supported!',
            'error.starting.export': 'Failed to start export!',
            'error.upload.file.type': 'Only XML files and ZIP archives can be imported!',
            'error.upload.file.required': 'Please select a file with import data!',
            'error.directory.required': 'Please enter a server directory with import data!',
            'error.upload.failed': 'File upload failed!',
            'error.starting.import': 'Failed to start import!',
            'error.login': 'Login failed!',
            'error.deleting.export': 'Failed to delete export!',
            'error.deleting.import': 'Failed to delete import!'
        });
        $translateProvider.translations('de', {
            'date.pattern': 'dd.MM.yy HH:mm',
            'time.pattern': '{{min}} Min, {{sec}} Sek',

            'download.cli.client': 'Kommandozeilen Client herunterladen',
            'download.api': 'Java API herunterladen',
            'export': 'Export',
            'import': 'Import',
            'settings': 'Einstellungen',
            'help': 'Hilfe',
            'server.information': 'Server Informationen',
            'liferay.release': 'Liferay Release',
            'cliwix.version': 'Cliwix Version',
            'cliwix.workspace.directory': 'Cliwix Arbeitsverzeichnis',
            'export.exportAllCompanies': 'Alle Companies',
            'export.companyFilter': 'Filter',
            'export.companyFilter.tooltip': 'Mit Beistrich getrennte Liste von Company webIds, dürfen Platzhalter enthalten (*)',
            'export.exportAllSites': 'Alle Sites',
            'export.siteFilter': 'Filter',
            'export.siteFilter.tooltip': 'Mit Beistrich getrennte Liste von Site Namen, dürfen Platzhalter enthalten (*)',
            'export.roles': 'Rollen',
            'export.userGroups': 'Benutzergruppen',
            'export.organizations': 'Organisationen',
            'export.users': 'Benutzer',
            'export.exportPortalInstanceConfiguration': 'Portal Instanz Konfiguration',
            'export.exportPortalInstanceConfiguration.tooltip': 'Allgemeine Portal Instanz Konfigurationen wie Virtual Host oder LDAP Einstellungen',
            'export.exportSiteConfiguration': 'Site Konfiguration',
            'export.exportSiteConfiguration.tooltip': 'Allgemeine Site Konfigurationen wie Friendly URL',
            'export.pages': 'Seiten',
            'export.webContent': 'Artikel',
            'export.documentLibrary': 'Dokumentenbibliothek',
            'export.skipCorruptDocuments': 'Korrupte Dokumente überspringen',
            'export.skipCorruptDocuments.tooltip': 'Dokumente in der Bibliothek ignorieren die nicht exportiert werden können',
            'export.exportOnlyFileDataLastModifiedWithinDays1': 'Nur Daten die innerhalb der letzten',
            'export.exportOnlyFileDataLastModifiedWithinDays2': 'Tage geändert wurden exportieren',
            'export.exportOnlyFileDataLastModifiedWithinDays.tooltip': 'Nur die Daten von Dateien exportieren die innerhalb der gegebenen Zeitspanne verändert wurden. NUR für periodische Imports.',
            'export.start': 'Start Export',
            'export.successfully.started': 'Export erfolgreich gestartet.',
            'export.list': 'Exporte',
            'close': 'Schließen',
            'import.server.directory': 'Von Server-Verzeichnis importieren',
            'import.server.directory.tooltip': 'Das Verzeichnis muss eine gültige liferay-config.xml Datei enthalten',
            'import.upload.file': 'Einzelne XML Datei oder ZIP Archiv hochladen',
            'import.upload.file.tooltip': 'Das ZIP Archiv muss eine gültige liferay-config.xml Datei enthalten',
            'import.atomicTransaction': 'Atomare Transaktion',
            'import.atomicTransaction.tooltip': 'Den Import innerhalb einer einzelnen, atomaren Transaktion ausführen',
            'import.simulationMode': 'Import simulieren (kein DB commit)',
            'import.overrideRootImportPolicy': 'Root Import Policy im XML überschreiben:',
            'import.ignoreNonExistingUsersGroupsOrgs': 'Nicht existierende User/Gruppen/Organisationen ignorieren',
            'import.ignoreNonExistingResourceActions': 'Nicht existierende Resource Actions ignorieren',
            'import.ignoreNonExistingResourceActions.tooltip': 'Nützlich falls Portlets zwar konfiguriert sind, aber noch nicht deployed',
            'import.ignoreInvalidDocumentReferences': 'Nicht existierende Dokumenten Referenzen ignorieren',
            'import.ignoreInvalidDocumentReferences.tooltip': 'Referenzen in Artikeln und anderem Content auf nicht existierende Dokumente ignorieren',
            'import.ignoreDeletionFailures': 'Fehler beim Löschen ignorieren',
            'import.ignoreDeletionFailures.tooltip': 'Nützlich falls Konfigurationselemente von anderen referenziert werden, die nicht von Cliwix gemanaged werden',
            'import.start': 'Start Import',
            'import.starting': "Starte Import...",
            'import.upload': "Upload",
            'import.successfully.started': 'Import erfolgreich gestartet.',
            'import.list': 'Importe',
            'info.cliwixWorkspaceDirectory.tooltip': 'Kann geändert werden durch die Umgebungsvariable CLIWIX_WORKSPACE',
            'browse.file': "Auswählen",
            'no.file.selected': "Keine Datei ausgewählt",
            'refreshing': 'Aktualisieren...',
            'timestamp': 'Start Zeit',
            'duration': 'Dauer',
            'user': 'Benutzer',
            'client': 'Client',
            'client.ip': 'Client IP Adresse',
            'state': 'Status',
            'success': 'Erfolgreich',
            'failed': 'Fehlgeschlagen',
            'processing': 'In Bearbeitung...',
            'download.report': 'Report',
            'download.export.data': 'Exportierte Daten',
            'page.previous': 'Vorige Seite',
            'page.next': 'Nächste Seite',
            'job.duration': '{{min}} Min {{sec}} Sek',
            'login.title': 'Login (als Liferay OmniAdmin)',
            'login.username': 'Benutzername',
            'login.password': 'Passwort',
            'login.login': 'Login',
            'logout': 'Abmelden',
            'help.manual': 'Online Handbuch für Cliwix',
            'help.online': 'Online Ressourcen',
            'delete': 'Löschen',

            'error.message': 'Fehlermeldung',
            'error.couldnt.reach.server': 'Server ist nicht erreichbar!',
            'error.no.liferay.found': 'Kein Liferay auf dem Server gefunden!',
            'error.liferay.not.ready': 'Liferay ist noch nicht bereit. Bitte laden sie die Seite in einigen Minuten neu.',
            'error.liferay.not.supported': 'Die Liferay Version wird nicht unterstützt!',
            'error.starting.export': 'Export konnte nicht gestartet werden!',
            'error.upload.file.type': 'Nur XML Dateien und ZIP Archive können importiert werden!',
            'error.upload.file.required': 'Bitte geben Sie eine Datei mit Import Daten an!',
            'error.directory.required': 'Bitte geben Sie ein Server Verzeichnis mit Import Daten an!',
            'error.upload.failed': 'File upload fehlgeschlagen!',
            'error.starting.import': 'Import konnte nicht gestartet werden!',
            'error.login': 'Login fehlgeschlagen!',
            'error.deleting.export': 'Fehler beim Löschen des Exports!',
            'error.deleting.import': 'Fehler beim Löschen des Imports!'
        });

        $translateProvider.determinePreferredLanguage();
        var lang = $translateProvider.preferredLanguage();
        if (lang.indexOf('de') === 0) {
            $translateProvider.preferredLanguage('de');
        } else {
            $translateProvider.preferredLanguage('en');
        }

    });
