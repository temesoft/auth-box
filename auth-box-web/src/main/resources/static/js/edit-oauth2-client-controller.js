app.controller('editOauth2ClientController', function organizationController($scope, $http, toastr, $location, $routeParams){
    $scope.oauth2Client = null;
    $scope.oauth2Scopes = [];
    $scope.location = $location;

    $scope.loadOauth2Client = function(clientId) {
        if (isEmpty(clientId)) {
            return;
        }
        $http({
            method: 'GET',
            url: '/api/oauth2-client/' + clientId
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Client = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.addNewPrivateKeySubmit = function() {
        if ($scope.oauth2Client.privateKey != null && $scope.oauth2Client.privateKey.length > 0) {
            $scope.updateOauth2Client();
        } else {
            $scope.$parent.logError("Private key can not be empty");
        }
    };

    $scope.updateOauth2Client = function() {

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

        buttonLoading($("#addNewPrivateKeyBtn"));
        buttonLoading($("#updateOauth2ClientBtn"));
        buttonLoading($("#modalEnterPrivateKeyBtn"));
        $http({
            method: 'POST',
            url: '/api/oauth2-client/' + $scope.oauth2Client.id,
            data: $scope.oauth2Client
        }).then(
            function success(response) {
                buttonReset($("#addNewPrivateKeyBtn"));
                buttonReset($("#updateOauth2ClientBtn"));
                buttonReset($("#modalEnterPrivateKeyBtn"));
                if ($scope.$parent.validate(response)) {
                    $scope.$parent.logSuccess("Client updated");
                    $scope.oauth2Client = response.data;
                    $("#modalEnterPrivateKey").modal('hide');
                    $scope.oauth2Client.privateKey = null;
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.deleteOauth2Client = function() {
        $scope.$parent.areYouSure("You are about to delete a client. This can not be undone.", function(){
            const ids = [];
            ids.push($scope.oauth2Client.id);
            const dataObject = {'clientIds': ids};
            $http({
                method: 'DELETE',
                url: '/api/oauth2-client',
                data: dataObject,
                headers: {
                    "Content-Type": "application/json"
                }
            }).then(
                function success(response) {
                    if ($scope.$parent.validate(response)) {
                        $scope.$parent.logSuccess("Client deleted");
                        $location.url('/oauth2-clients');
                    }
                },
                $scope.$parent.logFailure,
            );
        });
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

    $scope.generateKeys = function() {
        $scope.$parent.areYouSure("You are about to replace old security keys with new ones. This can not be undone.", function(){
            buttonLoading($("#generateKeysBtn"));
            $http({
                method: 'POST',
                url: '/api/oauth2-client/' + $scope.oauth2Client.id + '/create-new-keys'
            }).then(
                function success(response) {
                    buttonReset($("#generateKeysBtn"));
                    if ($scope.$parent.validate(response)) {
                        $scope.$parent.logSuccess("Client keys generated");
                        $scope.oauth2Client.publicKey = response.data.publicKey;
                    }
                },
                $scope.$parent.logFailure,
            );
        });
    };

    $scope.addNewPrivateKey = function() {
        $("#modalEnterPrivateKey").modal('show');
    };

    $scope.loadOauth2Scopes();
    $scope.loadOauth2Client($routeParams.clientId);
});