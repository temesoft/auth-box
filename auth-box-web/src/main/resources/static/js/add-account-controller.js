app.controller('addAccountController', function organizationController($scope, $http, toastr, $location){
    $scope.account = {"role":"ROLE_USER"};
    $scope.location = $location;

    $scope.createAccount = function() {
        if (isEmpty($scope.account.username)) {
            return $scope.$parent.logError("Account username can not be empty");
        }

        if (isEmpty($scope.account.role)) {
            return $scope.$parent.logError("Account access role can not be empty");
        }

        buttonLoading($("#createAccountBtn"));

        $http({
            method: 'POST',
            url: '/api/account/create',
            data: $scope.account
        }).then(
            function success(response) {
                buttonReset($("#createAccountBtn"));
                if ($scope.$parent.validate(response)) {
                    // redirect here
                    $scope.$parent.logSuccess("Client created successfully");
                    $location.url('/accounts');
                }
            },
            $scope.$parent.logFailure,
        );
    };

    initUiTools();

});