

var exec = require('cordova/exec');

module.exports = {
    echo:function(str, callback){
        console.log("echo str");
        cordova.exec(callback, function(err){
            callback("Nothing to echo.");
        },"CamCtrl","echo", [str]);
    },

    launch:function(str, callback){
        console.log("launch:" + str);
        cordova.exec(callback, function(err){
            callback("something wrong.");
        }, "CamCtrl","launch", [str]);
    }
};