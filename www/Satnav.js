var exec = require('cordova/exec');

var satnavJs = function(){};

satnavJs.prototype.showMap = function(targetP, success, error) {
    exec(success, error, "Satnav", "showMap", [targetP]);
};

module.exports = new satnavJs();