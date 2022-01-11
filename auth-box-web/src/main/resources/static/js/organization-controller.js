app.controller('organizationController', function organizationController($scope, $http){
    $scope.organization = null;

    $scope.loadOrganization = function() {
        $http({
            method: 'GET',
            url: '/api/organization'
        }).then(
            function success(response){
                if ($scope.$parent.validate(response)) {
                    $scope.organization = response.data;
                    console.log("loaded org", $scope.organization);
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.updateOrganization = function() {
        buttonLoading($("#updateOrganizationBtn"));
        $http({
            method: 'POST',
            url: '/api/organization',
            data: $scope.organization
        }).then(
            function success(response) {
                buttonReset($("#updateOrganizationBtn"));
                if ($scope.$parent.validate(response)) {
                    $scope.$parent.logSuccess("Organization details updated");
                    $scope.organization = response.data;
                    console.log("updated org", $scope.organization);
                    $("#modalEnterPrivateKey").modal('hide');
                    $scope.$parent.loadInitialAppData();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.updateOrganizationName = function() {
        $scope.organization.domainPrefix = $scope.organization.name.replace(/[^a-z0-9]/gi,'').toLowerCase();
    };

    $scope.validateDomainPrefix = function() {
        var letterNumber = /^[0-9a-z]+$/;
        if ($scope.organization.domainPrefix.match(letterNumber)) {
            $scope.organization.domainPrefix = $scope.organization.domainPrefix.trim();
            return;
        } else {
            $scope.organization.domainPrefix = $scope.organization.domainPrefix.replace(/[^a-z0-9]/gi,'').toLowerCase().trim();
        }
    };

    $scope.loadOrganization();
});