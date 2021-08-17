app.controller('oauth2UsersController', function oauth2UsersController($scope, $http, toastr, $location, $routeParams){
    $scope.oauth2Users = null;
    $scope.oauth2User = {"metadata": "{}"};
    $scope.pageSize = 10;
    $scope.currentPage = 0;
    $scope.location = $location;
    $scope.password = {"newPassword":"", "newPassword2":""};
    $scope.passwordResetUser = null;

    $scope.loadOauth2Users = function(pageSize, currentPage) {
        if (pageSize != undefined) {
            $scope.pageSize = pageSize;
        }
        if (currentPage != undefined) {
            $scope.currentPage = currentPage;
        }
        $http({
            method: 'GET',
            url: '/api/oauth2-user?pageSize=' + $scope.pageSize + '&currentPage=' + $scope.currentPage
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.oauth2Users = response.data;
                    initUiTools();
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.loadOauth2User = function(userId) {
            $http({
                method: 'GET',
                url: '/api/oauth2-user/' + userId
            }).then(
                function success(response) {
                    if ($scope.$parent.validate(response)) {
                        $scope.oauth2User = response.data;
                        initUiTools();
                    }
                },
                $scope.$parent.logFailure,
            );
        };

    $scope.enableDisableUsers = function(value, clientId) {
        for (var i = 0; i < $scope.oauth2Users.content.length; i++) {
            var oauth2User = $scope.oauth2Users.content[i];
            if (oauth2User.id == clientId) {
                oauth2User.enabled = value;
                $http({
                    method: 'POST',
                    url: '/api/oauth2-user/' + oauth2User.id,
                    data: oauth2User
                }).then(
                    function success(response) {
                        if ($scope.$parent.validate(response)) {
                            $scope.$parent.logSuccess("Users details updated");
                            $scope.loadOauth2Users();
                            initTooltip();
                        }
                    },
                    $scope.$parent.logFailure,
                );
            }
        }
    };

    $scope.enableDisableUser = function(userId) {
            $http({
                method: 'POST',
                url: '/api/oauth2-user/' + userId,
                data: $scope.oauth2User
            }).then(
                function success(response) {
                    if ($scope.$parent.validate(response)) {
                        $scope.$parent.logSuccess("User details updated");
                        $scope.loadOauth2User(userId);
                        initTooltip();
                    }
                },
                $scope.$parent.logFailure,
            );
        };

    $scope.deleteSelectedUsers = function() {
        var userIds = [];
        $('input[name="userSelectorCheckbox"]:checked').each(function() {
            userIds.push($(this).attr('data-user-id'));
        })

        const request = {'userIds': userIds};
        $scope.$parent.areYouSure("You are about to delete "+ userIds.length + " user(s). This can not be undone.", function(){
            $http({
                method: 'DELETE',
                url: '/api/oauth2-user',
                data: request,
                headers: {
                    "Content-Type": "application/json"
                }
            }).then(
                function success(response) {
                    if ($scope.$parent.validate(response)) {
                        $scope.loadOauth2Users();
                        $scope.$parent.logSuccess(userIds.length + " user(s) deleted");
                    }
                },
                $scope.$parent.logFailure,
            );
        });
    };

    $scope.updateOauth2User = function() {

        if (isEmpty($scope.oauth2User.username)) {
            return $scope.$parent.logError("User username can not be empty");
        }

        buttonLoading($("#updateOauth2UserBtn"));
        $http({
            method: 'POST',
            url: '/api/oauth2-user/' + $scope.oauth2User.id,
            data: $scope.oauth2User
        }).then(
            function success(response) {
                buttonReset($("#updateOauth2UserBtn"));
                if ($scope.$parent.validate(response)) {
                    $scope.$parent.logSuccess("User updated");
                    $scope.loadOauth2User($scope.oauth2User.id);
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.editSelectedUser = function() {
        const userId = $('input[name="userSelectorCheckbox"]:checked').attr("data-user-id");
        $location.url("/edit-oauth2-user/" + userId);
    };

    $scope.createOauth2User = function() {

        if (isEmpty($scope.oauth2User.username)) {
            return $scope.$parent.logError("Username can not be empty");
        }

        if (isEmpty($scope.oauth2User.metadata.length)) {
            return $scope.$parent.logError("Metadata can not be empty");
        }

        buttonLoading($("#createOauth2UserBtn"));
        $http({
            method: 'POST',
            url: '/api/oauth2-user',
            data: $scope.oauth2User
        }).then(
            function success(response) {
                buttonReset($("#createOauth2UserBtn"));
                if ($scope.$parent.validate(response)) {
                    // redirect here
                    $scope.oauth2User = response.data;
                    $scope.$parent.logSuccess("User created successfully");
                    $location.url('/edit-oauth2-user/' + $scope.oauth2User.id);
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.passwordResetModal = function(oauth2User) {
        $("#passwordResetModal").modal('show');
        $scope.passwordResetUser = oauth2User;
    };

    $scope.twoFactorAuthModal = function() {
        $("#twoFactorAuthModal").modal('show');
    };

    $scope.passwordReset = function() {
        $http({
            method: 'POST',
            url: '/api/oauth2-user/' + $scope.passwordResetUser.id + '/password-reset',
            data: $scope.password
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $("#passwordResetModal").modal('hide');
                    if ($scope.password.newPassword == "") {
                        $scope.$parent.logSuccess("User '" + $scope.passwordResetUser.username + "' password reset");
                    } else {
                        $scope.$parent.logSuccess("User '" + $scope.passwordResetUser.username + "' password updated");
                    }
                    $scope.passwordResetUser = null;
                    $scope.password = {"newPassword":"", "newPassword2":""};
                }
            },
            $scope.$parent.logFailure,
        );
    };

    if (!isEmpty(($routeParams.userId))) {
        $scope.loadOauth2User($routeParams.userId);
    } else {
        $scope.loadOauth2Users($scope.pageSize, $scope.currentPage);
    }
});