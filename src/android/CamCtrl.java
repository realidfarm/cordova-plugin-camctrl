

package com.realid.cordova.plugin;

import android.content.Intent;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.realid.camctrlsdk.CameraActivity;

public class CamCtrl extends CordovaPlugin{
    private CallbackContext context;

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) 
        throws JSONException{
        if(action.equals("echo")){
            String message = args.getString(0);
            this.echo(message, callbackContext);
            return true;
        }else if(action.equals("launch")){
            final String message = args.getString(0);
            context = callbackContext;
            cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Intent intent = new Intent(CamCtrl.this.cordova.getActivity(), CameraActivity.class);
                    intent.putExtra("cameraIp", message);
                    CamCtrl.this.cordova.startActivityForResult(CamCtrl.this, intent, 0);
                }
            });


            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        context.success("bingo!");
    }

    private void echo(String message, CallbackContext callbackContext){
        if(message != null && message.length() > 0){
            callbackContext.success(message);
        }else{
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
}