/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

angular.module('dgc.navigation').controller('navigationController', ['$scope', 'navigationResource', '$cacheFactory', 'atlasConfig',
    function($scope, navigationResource, $cacheFactory, atlasConfig) {
        var limitIntialCount = 18;
        $scope.intialCount = limitIntialCount;

        $scope.updateVar = function(event) {
            $scope.$$prevSibling.query = angular.element(event.target).text();

        };

        $scope.dslQueryVal = atlasConfig.SEARCH_TYPE.dsl.value;

        $scope.$on('load_Traits', function() {
            $scope.leftnav = navigationResource.get();
        });

        setTimeout(function() {
            var httpDefaultCache = $cacheFactory.get('$http');
            httpDefaultCache.remove(atlasConfig.API_ENDPOINTS.TRAITS_LIST);
        }, 3600000);

        $scope.refreshTags = function() {
            var httpDefaultCache = $cacheFactory.get('$http');
            httpDefaultCache.remove(atlasConfig.API_ENDPOINTS.TRAITS_LIST);
            $scope.leftnav = navigationResource.get();
            $scope.intialCount = limitIntialCount;
        };

        $scope.showMore = function() {
            $scope.intialCount += limitIntialCount;
        };

        $scope.filterTags = function() {
            $scope.intialCount = limitIntialCount;
        };
    }
]);
