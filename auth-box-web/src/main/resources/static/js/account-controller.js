app.controller('accountController', function organizationController($scope, $http, toastr){
    $scope.account = null;
    $scope.password = {};

    $scope.loadAccount = function() {
        $http({
            method: 'GET',
            url: '/api/account'
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.account = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.updateAccount = function() {
        buttonLoading($("#updateAccountBtn"));
        $http({
            method: 'POST',
            url: '/api/account',
            data: $scope.account
        }).then(
            function success(response) {
                buttonReset($("#updateAccountBtn"));
                if ($scope.$parent.validate(response)) {
                    $scope.$parent.logSuccess("Account details updated");
                    $scope.account = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.updatePassword = function() {
        buttonLoading($("#updatePasswordBtn"));
        $http({
            method: 'POST',
            url: '/api/account/password',
            data: $scope.password
        }).then(
            function success(response){
                buttonReset($("#updatePasswordBtn"));
                if ($scope.$parent.validate(response)) {
                    $("#modalChangePassword").modal('hide');
                    $scope.password = {};
                    $scope.$parent.logSuccess("Account password updated");
                    $scope.account = response.data;
                    $scope.password = {};
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.changePasswordModal = function() {
        $("#modalChangePassword").modal('show');
    };

    $scope.loadAccount();
});