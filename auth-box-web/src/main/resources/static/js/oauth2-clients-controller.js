app.controller('oauth2ClientsController', function organizationController($scope, $http, toastr, $location){
    $scope.oauth2Clients = null;
    $scope.pageSize = 10;
    $scope.currentPage = 0;
    $scope.location = $location;

    $scope.loadOauth2Clients = function(pageSize, currentPage) {
        $scope.pageSize = pageSize;
        $scope.currentPage = currentPage;
        $http({
            method: 'GET',
            url: '/api/oauth2-client?pageSize=' + $scope.pageSize + '&currentPage=' + $scope.currentPage
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

    $scope.enableDisableClient = function(value, clientId) {
        for (var i = 0; i < $scope.oauth2Clients.content.length; i++) {
            var oauth2Client = $scope.oauth2Clients.content[i];
            if (oauth2Client.id == clientId) {
                oauth2Client.enabled = value;
                $http({
                    method: 'POST',
                    url: '/api/oauth2-client/' + oauth2Client.id,
                    data: oauth2Client
                }).then(
                    function success(response) {
                        if ($scope.$parent.validate(response)) {
                            $scope.$parent.logSuccess("Client details updated");
                            $scope.loadOauth2Clients($scope.pageSize, $scope.currentPage);
                            initTooltip();
                        }
                    },
                    $scope.$parent.logFailure,
                );
            }
        }
    };

    $scope.deleteSelectedClients = function() {
        var clientIds = [];
        $('input[name="clientSelectorCheckbox"]:checked').each(function() {
            clientIds.push($(this).attr('data-client-id'));
        });

        $scope.$parent.areYouSure("You are about to delete " + clientIds.length + " client(s). This can not be undone.", function(){
            const request = {'clientIds': clientIds};

            buttonLoading($("#deleteSelectedClientsBtn"));
            $http({
                method: 'DELETE',
                url: '/api/oauth2-client',
                data: request,
                headers: {
                    "Content-Type": "application/json"
                }
            }).then(
                function success(response) {
                    buttonReset($("#deleteSelectedClientsBtn"));
                    if ($scope.$parent.validate(response)) {
                        $scope.loadOauth2Clients($scope.pageSize, $scope.currentPage);
                        $scope.$parent.logSuccess(clientIds.length + " clients(s) deleted");
                    }
                },
                $scope.$parent.logFailure,
            );
        });
    };

    $scope.loadOauth2Clients($scope.pageSize, $scope.currentPage);
});