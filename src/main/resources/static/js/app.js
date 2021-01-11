feather.replace();

var chartWrapper = '<canvas id="cryptoChart" class="my-4" width="900" height="380"></canvas>';
var app = angular.module("dashboard", [])
    .controller("mainCtr", function($scope, $interval, $http) {

        $scope.locaton = "";

        function sleep(ms) {
            return new Promise(resolve => setTimeout(resolve, ms));
        }

        $http.get($scope.locaton + "/api/markets").then(function(response) {
            $scope.markets = response.data;
        });

        $http.get($scope.locaton + "/api/trading/status").then(function(response) {
            if (response.data == "on") {
                $scope.power = "On"
                $(".power").css({'color': '#99CC18'});
            } else {
                $scope.power = "Off"
                $(".power").css({'color': 'red'});
            }
        });

        $scope.powerOnOff = function() {
            $http.get($scope.locaton + "/api/trading?active=" + ($scope.power != "On")).then(function(response) {
                if (response.data == "on") {
                    $scope.power = "On"
                    $(".power").css({'color': '#99CC18'});
                } else {
                    $scope.power = "Off"
                    $(".power").css({'color': 'red'});
                }
            }, function(error){console.log(error);}) ;
        }

        var logRefresh;
        $scope.showLog = function() {
            $("main").addClass("d-none");
            $("#logArea").removeClass("d-none");
            $("#logArea").html("Loading, wait...");

            logRefresh = $interval(function() {
                $http.get($scope.locaton + "/api/logging").then(function(response) {
                    $("#logArea").html("<pre>" + response.data.replace("\n", "<br />", "g") + "</pre>");
                });
            }, 2000);
        }

        $scope.showMarket = async function (event) {
            if (angular.isDefined(logRefresh)) {
                $interval.cancel(logRefresh);
                logRefresh = undefined;
            }
            var market = event.target.hash.replace("#", "");
            $("main").addClass("d-none");
            $("#logArea").html("Loading, wait...");
            $("#logArea").removeClass("d-none");
            $scope.MarketName = market;
            $scope.AccountInfo = "Loading, wait...";
            $(".chartWrapper").html("");
            $(".accinf").addClass("d-none");
            $http.get($scope.locaton + "/api/status?market=" + market).then(function (response) {
                $scope.AccountInfo = response.data;
                $(".accinf").removeClass("d-none");
            });
            await sleep(1000);
            $http.get($scope.locaton + "/api/symbols?market=" + market).then(function (response) {
                $scope.symbols = response.data;
            });
            await sleep(1000);
            $http.get($scope.locaton + "/api/orders/active?limit=20&market=" + market).then(function (response) {
                $scope.activeOrders = response.data;
            });
            await sleep(1000);
            $http.get($scope.locaton + "/api/orders/history?limit=20&market=" + market).then(function (response) {
                $scope.historyOrders = response.data;
                $("#logArea").addClass("d-none");
                $("main").removeClass("d-none");
            });
        }
    });