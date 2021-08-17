app.controller('oauth2ScopesController', function organizationController($scope, $http, toastr){
    $scope.oauth2Scopes = null;
    $scope.pageSize = 10;
    $scope.currentPage = 0;
    $scope.newScope = {};
    $scope.oauth2Scope = {};

    $scope.loadOauth2Scopes = function(pageSize, currentPage) {
        $scope.pageSize = pageSize;
        $scope.currentPage = currentPage;
        $http({
            method: 'GET',
            url: '/api/oauth2-scope?pageSize=' + $scope.pageSize + '&currentPage=' + $scope.currentPage
        }).then(
            function success(response) {
                $scope.oauth2Scopes = response.data;
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.createOauth2Scopes = function() {
        buttonLoading($("#createOauth2ScopesBtn"));
        $http({
            method: 'POST',
            url: '/api/oauth2-scope',
            data: $scope.newScope
        }).then(
            function success(response) {
                buttonReset($("#createOauth2ScopesBtn"));
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Scopes = response.data;
                    $("#modalAddScope").modal('hide');
                    $scope.loadOauth2Scopes($scope.pageSize, $scope.currentPage);
                    $scope.$parent.logSuccess("Scope '" + $scope.newScope.scope + "' is created");
                    $scope.newScope = {};
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.openModalEditScope = function(oauth2Scope) {
        if (isEmpty(oauth2Scope)) {
            $('input[name="scopeSelectorCheckbox"]:checked').each(function(){
                $scope.oauth2Scope = JSON.parse($(this).attr('data-scope'));
            });
        } else {
            $scope.oauth2Scope = oauth2Scope;
        }

        $("#modalEditScope").modal('show');
    };

    $scope.updateOauth2Scope = function() {
        buttonLoading($("#updateOauth2ScopeBtn"));
        $http({
            method: 'POST',
            url: '/api/oauth2-scope/' + $scope.oauth2Scope.id,
            data: $scope.oauth2Scope
        }).then(
            function success(response) {
                buttonReset($("#updateOauth2ScopeBtn"));
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Scopes = response.data;
                    $("#modalEditScope").modal('hide');
                    $scope.$parent.logSuccess("Scope '" + $scope.oauth2Scope.scope + "' is updated");
                    $scope.oauth2Scope = {};
                    $scope.loadOauth2Scopes($scope.pageSize, $scope.currentPage);
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.deleteSelectedScopes = function() {
        var scopeIdArray = [];
        $('input[name="scopeSelectorCheckbox"]:checked').each(function() {
            scopeIdArray.push($(this).attr('data-client-id'));
        })

        $http({
            method: 'POST',
            url: '/api/oauth2-scope/count-clients',
            data: {'scopeIds': scopeIdArray},
            headers: {
                "Content-Type": "application/json"
            }
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    const countOfClients = response.data;
                    var toDelete = true;
                    if (countOfClients > 0) {
                        $scope.$parent.areYouSure("You are about to delete "+ scopeIdArray.length + " scope(s) that have " +
                            countOfClients + " client connections. This can not be undone.", function(){
                                $http({
                                    method: 'DELETE',
                                    url: '/api/oauth2-scope',
                                    data: {'scopeIds': scopeIdArray},
                                    headers: {
                                        "Content-Type": "application/json"
                                    }
                                }).then(
                                    function success(response) {
                                        $scope.loadOauth2Scopes($scope.pageSize, $scope.currentPage);
                                        $scope.$parent.logSuccess("Scope deleted");
                                    },
                                    $scope.$parent.logFailure,
                                );
                        });
                    } else {
                        $http({
                            method: 'DELETE',
                            url: '/api/oauth2-scope',
                            data: {'scopeIds': scopeIdArray},
                            headers: {
                                "Content-Type": "application/json"
                            }
                        }).then(
                            function success(response) {
                                $scope.loadOauth2Scopes($scope.pageSize, $scope.currentPage);
                                $scope.$parent.logSuccess("Scope deleted");
                            },
                            $scope.$parent.logFailure,
                        );
                    }
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.openModalAddScope = function() {
        $("#modalAddScope").modal('show');
    };

    $scope.loadOauth2Scopes($scope.pageSize, $scope.currentPage);
});