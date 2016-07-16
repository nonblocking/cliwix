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





