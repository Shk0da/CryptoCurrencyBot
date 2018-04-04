feather.replace();

var chartWrapper = '<canvas id="cryptoChart" class="my-4" width="900" height="380"></canvas>';
var app = angular.module("dashboard", [])
    .controller("mainCtr", function($scope, $interval, $http) {
        $http.get("/api/markets").then(function(response) {
            $scope.markets = response.data;
        });

        $scope.power = "Off"
        $(".power").css({'color': 'red'});
        $scope.powerOnOff = function() {
            $http.get("/api/trading?active=" + ($scope.power != "On")).then(function(response) {
                if (response.data == "on") {
                   $scope.power = "On"
                   $(".power").css({'color': '#99CC18'});
                } else {
                   $scope.power = "Off"
                   $(".power").css({'color': 'red'});
                }
            }, function(error){console.log(error);}) ;
        }

        $scope.showLog = function() {
             $("main").addClass("d-none");
             $("#logArea").removeClass("d-none");
             $("#logArea").html("Loading, wait...");
             $http.get("/api/logging").then(function(response) {
                 $("#logArea").html("<pre>" + response.data.replace("\n", "<br />", "g") + "</pre>");
             });
        }

        $scope.showMarket = function(event) {
            var market = event.target.hash.replace("#", "");
            $("#logArea").addClass("d-none");
            $("main").removeClass("d-none");
            $scope.MarketName = market;
            $scope.AccountInfo = "Loading, wait...";
            $(".chartWrapper").html("");
            $http.get("/api/status?market=" + market).then(function(response) {
                $scope.AccountInfo = JSON.stringify(response.data, null, 4);
            });
            $http.get("/api/symbols?market=" + market).then(function(response) {
                $scope.symbols = response.data;
            });
            $http.get("/api/orders/active?limit=20&market=" + market).then(function(response) {
                $scope.activeOrders = response.data;
            });
            $http.get("/api/orders/history?limit=20&market=" + market).then(function(response) {
                $scope.historyOrders = response.data;
            });
        }

        $scope.showSymbol = function(event) {
            var symbol = event.target.hash.replace("#", "");
            console.log(symbol);

            $(".chartWrapper").html(chartWrapper);
            var ctx = document.getElementById("cryptoChart");
                  var cryptoChart = new Chart(ctx, {
                    type: 'line',
                    data: {
                      labels: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"],
                      datasets: [{
                        data: [15339, 21345, 18483, 24003, 23489, 24092, 12034],
                        lineTension: 0,
                        backgroundColor: 'transparent',
                        borderColor: '#007bff',
                        borderWidth: 4,
                        pointBackgroundColor: '#007bff'
                      }]
                    },
                    options: {
                      scales: {
                        yAxes: [{
                          ticks: {
                            beginAtZero: false
                          }
                        }]
                      },
                      legend: {
                        display: false,
                      }
                    }
                  });
        }
    });