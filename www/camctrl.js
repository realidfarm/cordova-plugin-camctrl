

var exec = require('cordova/exec');

module.exports = {
    echo:function(str, callback){
        console.log("echo str");
        cordova.exec(callback, function(err){
            callback("Nothing to echo.");
        },"CamCtrl","echo", [str]);
    },

    launch:function(camIp, camPort, uName, pwd, tunel, callback){
        console.log("launch camera activity:" + camIp);
        cordova.exec(callback, function(err){
            callback("something wrong.");
        }, "CamCtrl","launch", [camIp, camPort, uName, pwd, tunel]);
    }
};