app.controller('accountsController', function organizationController($scope, $http, toastr, $location, $routeParams){
    $scope.accounts = null;
    $scope.account = null;
    $scope.password = {};
    $scope.pageSize = 10;
    $scope.currentPage = 0;
    $scope.location = $location;
    $scope.routeParams = $routeParams;

    $scope.loadAccounts = function(pageSize, currentPage) {
        $scope.pageSize = pageSize;
        $scope.currentPage = currentPage;
        $http({
            method: 'GET',
            url: '/api/account/list?pageSize=' + $scope.pageSize + '&currentPage=' + $scope.currentPage
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.account = null;
                    $scope.accounts = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.loadAccount = function(id) {
        $http({
            method: 'GET',
            url: '/api/account/' + id
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.accounts = null;
                    $scope.account = response.data;
                    $scope.account.password = "";
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.deleteSelectedAccount = function() {
        var accountIdArray = [];
        $('input[name="accountSelectorCheckbox"]:checked').each(function() {
            accountIdArray.push($(this).attr('data-account-id'));
        });
        $scope.$parent.areYouSure("You are about to delete " + accountIdArray.length + " account(s). This can not be undone.", function(){
            buttonLoading($("#deleteSelectedAccountBtn"));
            $http({
                method: 'DELETE',
                url: '/api/account/',
                data: {'accountIds': accountIdArray},
                headers: {
                    "Content-Type": "application/json"
                }
            }).then(
                function success(response) {
                    buttonReset($("#deleteSelectedAccountBtn"));
                    if ($scope.$parent.validate(response)) {
                        $scope.loadAccounts($scope.pageSize, $scope.currentPage);
                        $scope.$parent.logSuccess("Account(s) deleted");
                    }
                },
                $scope.$parent.logFailure,
            );
        });
    };

    $scope.updateAccount = function(accountId) {
        buttonLoading($("#updateAccountBtn"));
        $http({
            method: 'POST',
            url: '/api/account/' + accountId,
            data: $scope.account,
        }).then(
            function success(response) {
                buttonReset($("#updateAccountBtn"));
                if ($scope.$parent.validate(response)) {
                    $scope.$parent.logSuccess("Account details updated");
                    $scope.loadAccount($scope.account.id);
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.enableDisableAccount = function(account) {
        $http({
            method: 'POST',
            url: '/api/account/' + account.id,
            data: account,
            headers: {
                "Content-Type": "application/json"
            }
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.$parent.logSuccess("Account details updated");
                    if ($scope.accounts != null) {
                        $scope.loadAccounts($scope.pageSize, $scope.currentPage);
                    } else {
                        $scope.loadAccount(account.id);
                    }
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.updatePassword = function() {
        $http({
            method: 'POST',
            url: '/api/account/password',
            data: $scope.password
        }).then(
            function success(response){
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

    if (!isEmpty(($routeParams.id))) {
        $scope.loadAccount($routeParams.id);
    } else {
        $scope.loadAccounts($scope.pageSize, $scope.currentPage);
    }
});