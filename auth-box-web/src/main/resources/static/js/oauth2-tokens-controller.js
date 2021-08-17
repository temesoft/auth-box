app.controller('oauth2TokensController', function organizationController($scope, $http, toastr, $location, $routeParams){
    $scope.oauth2Tokens = null;
    $scope.oauth2Token = null;
    $scope.location = $location;
    $scope.routeParams = $routeParams;
    $scope.pageSize = 10;
    $scope.currentPage = 0;
    $scope.searchQuery = (
        (isEmpty($scope.routeParams.clientId) ? "" : $scope.routeParams.clientId)
        + (isEmpty($scope.routeParams.userId) ? "" : $scope.routeParams.userId)
        + (isEmpty($scope.routeParams.hash) ? "" : $scope.routeParams.hash)
        + (isEmpty($scope.routeParams.token) ? "" : $scope.routeParams.token)
        + (isEmpty($scope.routeParams.id) ? "" : $scope.routeParams.id)
    );

    $scope.loadOauth2Tokens = function(pageSize, currentPage) {
        $scope.pageSize = pageSize;
        $scope.currentPage = currentPage;
        $http({
            method: 'GET',
            url: '/api/oauth2-token/list?pageSize=' + $scope.pageSize + '&currentPage=' + $scope.currentPage
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Token = null;
                    $scope.oauth2Tokens = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.loadOauth2TokensFor = function(type, value, pageSize, currentPage) {
        $scope.pageSize = pageSize;
        $scope.currentPage = currentPage;
        $http({
            method: 'GET',
            url: '/api/oauth2-token/' + type + '/' + value + "?pageSize=" + $scope.pageSize + "&currentPage=" + $scope.currentPage
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Token = null;
                    $scope.oauth2Tokens = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.loadOauth2TokenFor = function(type, value) {
        $http({
            method: 'GET',
            url: '/api/oauth2-token/' + type + '/' + value
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Tokens = null;
                    $scope.oauth2Token = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.isAfterNow = function(date) {
        return moment().diff(date) < 0;
    };

    $scope.search = function(type, searchQuery) {
        if (isEmpty(searchQuery.trim())) {
            $scope.$parent.logError("Please enter a valid " + type);
            return;
        }
        $scope.location.url('/oauth2-tokens/' + type + "/" + searchQuery);
    };

    $scope.deleteSelectedTokens = function() {
        var tokenIdArray = [];
        $('input[name="tokenSelectorCheckbox"]:checked').each(function() {
            tokenIdArray.push($(this).attr('data-token-id'));
        });

        var onSuccess = function() {
            $scope.processRouteParams();
        };
        if (tokenIdArray.length == 0) {
            tokenIdArray.push($scope.oauth2Token.id);
            onSuccess = function() {
                window.history.back();
            }
        }
        $scope.$parent.areYouSure("You are about to delete "+ tokenIdArray.length + " token(s). This can not be undone.", function(){
            $http({
                method: 'DELETE',
                url: '/api/oauth2-token',
                data: {'tokenIds': tokenIdArray},
                headers: {
                    "Content-Type": "application/json"
                }
            }).then(
                function success(response) {
                    if ($scope.$parent.validate(response)) {
                        $scope.$parent.logSuccess(tokenIdArray.length + " token(s) deleted");
                        onSuccess();
                    }
                },
                    $scope.$parent.logFailure,
            );
        });
    };

    $scope.processRouteParams = function() {
        if (isNotEmpty($routeParams.clientId)) {
            $scope.loadOauth2TokensFor('client', $routeParams.clientId, $scope.pageSize, $scope.currentPage);
        } else if (isNotEmpty($routeParams.userId)) {
            $scope.loadOauth2TokensFor('user', $routeParams.userId, $scope.pageSize, $scope.currentPage);
        } else if (isNotEmpty($routeParams.hash)) {
            $scope.loadOauth2TokenFor('hash', $routeParams.hash);
        } else if (isNotEmpty($routeParams.token)) {
            $scope.loadOauth2TokenFor('token', $routeParams.token);
        } else if (isNotEmpty($routeParams.id)) {
            $scope.loadOauth2TokenFor('id', $routeParams.id);
        } else {
            $scope.loadOauth2Tokens($scope.pageSize, $scope.currentPage);
        }
    };

    $scope.processRouteParams();
});