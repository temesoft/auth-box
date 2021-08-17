app.controller('oauth2ClientCurlExamplesController', function organizationController($scope, $http, toastr, $location, $routeParams){
    $scope.oauth2Client = null;
    $scope.oauth2Clients = null;
    $scope.organization = null;
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

    $scope.loadOauth2Clients = function() {
        $http({
            method: 'GET',
            url: '/api/oauth2-client?pageSize=1000&currentPage=0'
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Clients = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.loadOrganization = function() {
        $http({
            method: 'GET',
            url: '/api/organization'
        }).then(
            function success(response){
                if ($scope.$parent.validate(response)) {
                    $scope.organization = response.data;
                }
            },
            $scope.$parent.logFailure,
        );
    };

    if (isEmpty($routeParams.clientId)) {
        $scope.loadOauth2Clients();
    } else {
        $scope.loadOauth2Client($routeParams.clientId);
    }


    $scope.loadOrganization();
});