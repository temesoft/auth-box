app.controller('accessLogController', function accessLogController($scope, $http, toastr, $location, $routeParams){
    $scope.location = $location;
    $scope.accessLogList = null;
    $scope.requestId = null;
    $scope.ip = null;
    $scope.ipDetails = null;
    $scope.userAgent = null;
    $scope.timestamp = null;
    $scope.hasErrors = false;
    $scope.errorMessage = null;
    $scope.statusCode = null;

    $scope.accessLogByRequestId = function() {
        $http({
            method: 'GET',
            url: '/api/access-log/' + $scope.requestId
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.accessLogList = response.data;
                    if ($scope.accessLogList.content.length > 0) {
                        // Only initial request contains IP and UserAgent
                        $scope.ip = $scope.accessLogList.content[0].ip;
                        $scope.userAgent = $scope.accessLogList.content[0].userAgent;
                        $scope.timestamp = $scope.accessLogList.content[0].createTime;
                        $scope.statusCode = $scope.accessLogList.content[$scope.accessLogList.content.length - 1].statusCode;
                        initUiTools();
                        $scope.getIpDetails($scope.ip);
                    }
                    for (var i = 0; i < $scope.accessLogList.content.length; i++) {
                        if ($scope.accessLogList.content[i].error != null) {
                            $scope.hasErrors = true;
                            $scope.errorMessage = $scope.accessLogList.content[i].error;
                        }
                    }
                }
            },
            $scope.$parent.logFailure,
        );
    };

    $scope.getIpDetails = function(ip) {
        $http({
            method: 'GET',
            url: '/api/access-log/ip/' + ip
        }).then(
            function success(response) {
                if ($scope.$parent.validate(response)) {
                    $scope.ipDetails = response.data;
                }
            },
            $scope.$parent.logFailure,
        );
    };

    if (!isEmpty(($routeParams.requestId))) {
        $scope.requestId = $routeParams.requestId;
        $scope.accessLogByRequestId();
    }
});