app.controller('addOauth2ClientController', function organizationController($scope, $http, toastr, $location){
    $scope.oauth2Client = {"expiration":"1h", "refreshExpiration":"24h", "redirectUrls": [], "grantTypes": [], "description":"", "scopeIds":[]};
    $scope.oauth2Scopes = [];
    $scope.location = $location;

    $scope.createOauth2Client = function() {

        if (isEmpty($scope.oauth2Client.description)) {
            return $scope.$parent.logError("Client description can not be empty");
        }

        if (isEmpty($scope.oauth2Client.grantTypes.length)) {
            return $scope.$parent.logError("Grant types list can not be empty");
        }

        if ($scope.oauth2Client.grantTypes.indexOf("authorization_code") >= 0
                && isEmpty($scope.oauth2Client.redirectUrls.length)) {
            return $scope.$parent.logError("Redirect url list can not be empty when 'authorization_code' is selected");
        }

        if (isEmpty($scope.oauth2Client.expiration)) {
            $scope.$parent.logError("Token expiration can not be empty");
        }

        if ($scope.oauth2Client.grantTypes.indexOf("refresh_token") >= 0
                && isEmpty($scope.oauth2Client.refreshExpiration.length)) {
            $scope.$parent.logError("Refresh token expiration can not be empty");
        }

        buttonLoading($("#createOauth2ClientBtn"));
        $http({
            method: 'POST',
            url: '/api/oauth2-client',
            data: $scope.oauth2Client
        }).then(
            function success(response) {
                buttonReset($("#createOauth2ClientBtn"));
                if ($scope.$parent.validate(response)) {
                    // redirect here
                    $scope.$parent.logSuccess("Client created successfully");
                    $location.url('/oauth2-clients');
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.addRedirectUrl = function() {
        if ($scope.oauth2Client.redirectUrl != null
            && ($scope.oauth2Client.redirectUrl.trim().indexOf("http://") == 0
                || ($scope.oauth2Client.redirectUrl.trim().indexOf("https://") == 0))
        ) {
            if ($scope.oauth2Client.redirectUrls.indexOf($scope.oauth2Client.redirectUrl) != -1) {
                $scope.$parent.logError("Redirect url already in the list");
                return;
            }

            $scope.oauth2Client.redirectUrls.push($scope.oauth2Client.redirectUrl.trim());
            $scope.oauth2Client.redirectUrl = "";
        } else {
            $scope.$parent.logError("Redirect url should not be empty, and should start with 'http://' or 'https://'");
        }
    };

    $scope.loadOauth2Scopes = function() {
        $http({
            method: 'GET',
            url: '/api/oauth2-scope'
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Scopes = response.data.content;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.removeRedirectUrl = function(redirectUrl) {
        const index = $scope.oauth2Client.redirectUrls.indexOf(redirectUrl);
        if (index >= 0) {
            $scope.oauth2Client.redirectUrls.splice(index, 1);
        }
    };

    $scope.loadOauth2Scopes();
});