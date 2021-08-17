var app = angular.module('authBoxWeb', ['ngRoute', 'angularMoment', 'toastr', 'ui.bootstrap']);

app.config(function ($routeProvider) {
 $routeProvider
    .when('/organization', {
        templateUrl: 'organization.html',
        controller: 'organizationController'
    })
    .when('/account', {
        templateUrl: 'account.html',
        controller: 'accountController'
    })
    .when('/oauth2-clients', {
        templateUrl: 'oauth2-clients.html',
        controller: 'oauth2ClientsController'
    })
    .when('/oauth2-scopes', {
         templateUrl: 'oauth2-scopes.html',
         controller: 'oauth2ScopesController'
     })
    .when('/add-oauth2-client', {
        templateUrl: 'add-oauth2-client.html',
        controller: 'addOauth2ClientController'
    })
    .when('/edit-oauth2-client/:clientId', {
        templateUrl: 'edit-oauth2-client.html',
        controller: 'editOauth2ClientController'
    })
    .when('/oauth2-client-curl-examples/:clientId', {
        templateUrl: 'oauth2-client-curl-examples.html',
        controller: 'oauth2ClientCurlExamplesController'
    })
    .when('/oauth2-client-curl-examples', {
        templateUrl: 'oauth2-client-curl-examples.html',
        controller: 'oauth2ClientCurlExamplesController'
    })
    .when('/oauth2-users', {
        templateUrl: 'oauth2-users.html',
        controller: 'oauth2UsersController'
    })
    .when('/edit-oauth2-user/:userId', {
        templateUrl: 'edit-oauth2-user.html',
        controller: 'oauth2UsersController'
    })
    .when('/add-oauth2-user', {
        templateUrl: 'add-oauth2-user.html',
        controller: 'oauth2UsersController'
    })
    .when('/oauth2-tokens/user/:userId', {
        templateUrl: 'oauth2-tokens.html',
        controller: 'oauth2TokensController'
    })
    .when('/oauth2-tokens/client/:clientId', {
        templateUrl: 'oauth2-tokens.html',
        controller: 'oauth2TokensController'
    })
    .when('/oauth2-tokens/hash/:hash', {
        templateUrl: 'oauth2-tokens.html',
        controller: 'oauth2TokensController'
    })
    .when('/oauth2-tokens/token/:token', {
        templateUrl: 'oauth2-tokens.html',
        controller: 'oauth2TokensController'
    })
    .when('/oauth2-tokens/id/:id', {
        templateUrl: 'oauth2-tokens.html',
        controller: 'oauth2TokensController'
    })
    .when('/oauth2-tokens', {
        templateUrl: 'oauth2-tokens.html',
        controller: 'oauth2TokensController'
    })
    .when('/accounts', {
         templateUrl: 'accounts.html',
         controller: 'accountsController'
     })
    .when('/add-account', {
        templateUrl: 'add-account.html',
        controller: 'addAccountController'
    })
    .when('/accounts/:id', {
        templateUrl: 'accounts.html',
        controller: 'accountsController'
    })
    .when('/access-log', {
        templateUrl: 'access-log.html',
        controller: 'accessLogController'
    })
    .when('/access-log/:requestId', {
        templateUrl: 'access-log.html',
        controller: 'accessLogController'
    })
    .otherwise({redirectTo: '/organization'});
});

app.controller('mainController', function mainController($scope, $http, toastr, $route){

    $scope.onAreYouSureConfirm = null;
    $scope.loggedInOrganization = null;
    $scope.loggedInAccount = null;
    $scope.appProperties = appProperties; // set in index.html via thymeleaf

    $scope.paginationRange = function(min, max, currentPage) {
        return paginationRange(min, max, currentPage);
    };

    $scope.validate = function(response){
        if (response.status >= 200 && response.status <= 299) {
            if (response.data.indexOf != undefined && response.data.indexOf("<!DOCTYPE html>") == 0) {
                window.location.assign("/logout");
                return false;
            }
            return true;
        } else {
            return false;
        }
    };

    $scope.loadInitialAppData = function() {
        $http({
            method: 'GET',
            url: '/api/organization'
        }).then(
            function success(response){
                if ($scope.validate(response)) {
                    $scope.loggedInOrganization = response.data;
                }
            },
            $scope.logFailure,
        );
        $http({
            method: 'GET',
            url: '/api/account'
        }).then(
            function success(response) {
                if ($scope.validate(response)) {
                    $scope.loggedInAccount = response.data;
                }
            },
            $scope.logFailure,
        );
    };

    $scope.togglePanel = function(panelId, caretId) {
        if ($("#" + panelId).hasClass("d-none")) {
            $("#" + panelId).removeClass("d-none");
            $("#" + caretId).removeClass("fa-caret-right");
            $("#" + caretId).addClass("fa-caret-down");
        } else {
            $("#" + panelId).addClass("d-none");
            $("#" + caretId).addClass("fa-caret-right");
            $("#" + caretId).removeClass("fa-caret-down");
        }
    };

    $scope.getRoute = function() {
        if ($route.current === undefined) {
            return null;
        }
        return $route.current.templateUrl;
    };

    $scope.logFailure = function(response) {
        buttonReset();
        if (response.status == 403) {
            toastr.error("Access denied");
        } else {
            if (response.status == -1 && response.data == null) {
                // logged out, redirect to login
                location.reload();
            } else {
                toastr.error(response.data.message, response.data.error);
                console.log(response.data.timestamp + ": " + response.data.message + " " + response.data.error);
            }
        }
    };

    $scope.logSuccess = function(message) {
        toastr.success(message);
    };

    $scope.logError = function(message) {
        toastr.error(message);
    };

    $scope.showSecret = function(secret) {
        $("#secretDisplayModal .modal-body").html(secret);
        $("#secretDisplayModal").modal("show");
    };

    $scope.areYouSure = function(warningMessage, onConfirm) {
        $scope.onAreYouSureConfirm = onConfirm;
        $("#warningMessage").text(warningMessage);
        $("#areYouSureModal").modal("show");
    };

    $scope.areYouSureApply = function() {
        $("#areYouSureModal").modal("hide");
        $scope.onAreYouSureConfirm();
    };

    $scope.selectAll = function() {
        if ($('#selectAll:checked').length > 0) {
            $('input[name="clientSelectorCheckbox"],input[name="scopeSelectorCheckbox"],input[name="userSelectorCheckbox"],input[name="tokenSelectorCheckbox"],input[name="accountSelectorCheckbox"]').prop('checked', true);
        } else {
            $('input[name="clientSelectorCheckbox"],input[name="scopeSelectorCheckbox"],input[name="userSelectorCheckbox"],input[name="tokenSelectorCheckbox"],input[name="accountSelectorCheckbox"]').prop('checked', false);
        }
        return true;
    };

    $scope.checkIfAnySelected = function() {
        return $('input[name="clientSelectorCheckbox"]:checked,input[name="scopeSelectorCheckbox"]:checked,input[name="userSelectorCheckbox"]:checked,input[name="tokenSelectorCheckbox"]:checked,input[name="accountSelectorCheckbox"]:checked').length > 0;
    };

    $scope.checkIfOneSelected = function() {
        return $('input[name="clientSelectorCheckbox"]:checked,input[name="scopeSelectorCheckbox"]:checked,input[name="userSelectorCheckbox"]:checked,input[name="tokenSelectorCheckbox"]:checked,input[name="accountSelectorCheckbox"]:checked').length == 1;
    };

    $scope.loadInitialAppData();
});

app.filter('secret', function() {
    return function(input) {
        if (input != undefined && input.length < 10) {
            return "*****";
        } else if (input != undefined && input.length >= 10) {
            return input.substring(0,8) + "...";
        }
    };
});

app.filter('console', function() {
    return function(input) {
        console.log(input);
    };
});