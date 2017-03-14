package com.realid.cordova.plugin;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.hikvision.netsdk.ExceptionCallBack;
import com.hikvision.netsdk.HCNetSDK;
import com.hikvision.netsdk.NET_DVR_DEVICEINFO_V30;
import com.hikvision.netsdk.NET_DVR_PREVIEWINFO;
import com.hikvision.netsdk.PTZCommand;
import com.hikvision.netsdk.RealPlayCallBack;
import com.realid.camctrl.CrashUtil;

import org.MediaPlayer.PlayM4.Player;

//生成的android app 包名
//import io.cordova.hellocordova.R;
import com.realidfarm.realidfarm.R;

public class CameraActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "CameraActivity";
    private String cameraIp = "58.214.29.198";
    private String userName = "admin";
    private String password = "gy123456";
    private String cameraPort = "8000";
    private boolean disableControl = false;
    private int channelId = 0;

    private ImageView btnIncrease;
    private ImageView btnReduce;
    private ImageView btnLeft;
    private ImageView btnRight;
    private ImageView btnUp;
    private ImageView btnDown;

    private SurfaceView m_osurfaceView = null;

    private NET_DVR_DEVICEINFO_V30 m_oNetDvrDeviceInfoV30 = null;

    private int m_iLogID = -1; // return by NET_DVR_Login_v30
    private int m_iPlayID = -1; // return by NET_DVR_RealPlay_V30
    private int m_iPlaybackID = -1; // return by NET_DVR_PlayBackByTime

    private int m_iPort = -1; // play port
    private int m_iStartChan = 0; // start channel no
    private int m_iChanNum = 0; // channel number

    private boolean m_bSurfaceCreated = false;
    private boolean isFullScreen = false;


    private static final int PERMISSION_REQ_STORAGE=1000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        cameraIp = intent.getStringExtra("cameraIp");
        cameraPort = intent.getStringExtra("cameraPort");
        userName = intent.getStringExtra("userName");
        password = intent.getStringExtra("password");
        String tunnel = intent.getStringExtra("tunnel");
        channelId = Integer.parseInt(tunnel);
        if (channelId>0){
            channelId = channelId - 1;
        }

        String disableCtrl = intent.getStringExtra("disableCtrl");
        if("yes" == disableCtrl){
            disableControl = true;
        }

        setContentView(R.layout.activity_camera);

        startRequestPermission();
    }

    public int getScreenWidth() {
       DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    private void fullScreen(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ViewGroup.LayoutParams lp = m_osurfaceView.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;

        ViewGroup.MarginLayoutParams mlp =(ViewGroup.MarginLayoutParams)lp;

        mlp.setMargins(0,0,0,0);

        m_osurfaceView.setLayoutParams(lp);
        isFullScreen = true;
    }

    @Override
    public void onBackPressed() {
        if(isFullScreen){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ViewGroup.LayoutParams lp = m_osurfaceView.getLayoutParams();
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;

            m_osurfaceView.getHolder().setFixedSize(getScreenWidth(),dip2px(this, 210f));

            ViewGroup.MarginLayoutParams mlp =(ViewGroup.MarginLayoutParams)lp;
            mlp.setMargins(0,dip2px(this, 44f),0,0);

            m_osurfaceView.setLayoutParams(lp);
            isFullScreen = false;

        }else{
            super.onBackPressed();
        }

    }

    private void initView(){

        m_osurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

        btnDown = (ImageView) findViewById(R.id.btn_down);
        btnIncrease = (ImageView) findViewById(R.id.btn_increase);
        btnUp = (ImageView) findViewById(R.id.btn_up);
        btnLeft = (ImageView) findViewById(R.id.btn_left);
        btnReduce = (ImageView) findViewById(R.id.btn_reduce);
        btnRight = (ImageView) findViewById(R.id.btn_right);

        ImageView btnBack = (ImageView) findViewById(R.id.btn_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CameraActivity.this.finish();
            }
        });

        ImageView btnFullScreen = (ImageView) findViewById(R.id.btn_fullscreen);
        btnFullScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fullScreen();
            }
        });


        btnIncrease.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(disableControl){
                    Toast.makeText(CameraActivity.this, "您没有控制摄像头的权限", Toast.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    if (m_iLogID < 0) {
                        Log.e(TAG, "please login on a device first");
                        return false;
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.ZOOM_IN, 0)) {
                            Log.e(TAG,
                                    "start ZOOM_IN failed with error code: "
                                            + HCNetSDK.getInstance()
                                            .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "start ZOOM_IN success");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.ZOOM_IN, 1)) {
                            Log.e(TAG, "stop ZOOM_IN failed with error code: "
                                    + HCNetSDK.getInstance()
                                    .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "stop ZOOM_IN success");
                        }
                    }
                    return true;
                } catch (Exception err) {
                    Log.e(TAG, "error: " + err.toString());
                    return false;
                }
            }
        });

        btnReduce.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(disableControl){
                    Toast.makeText(CameraActivity.this, "您没有控制摄像头的权限", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    if (m_iLogID < 0) {
                        Log.e(TAG, "please login on a device first");
                        return false;
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.ZOOM_OUT, 0)) {
                            Log.e(TAG,
                                    "start ZOOM_OUT failed with error code: "
                                            + HCNetSDK.getInstance()
                                            .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "start ZOOM_OUT success");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.ZOOM_OUT, 1)) {
                            Log.e(TAG, "stop ZOOM_OUT failed with error code: "
                                    + HCNetSDK.getInstance()
                                    .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "stop ZOOM_OUT success");
                        }
                    }
                    return true;
                } catch (Exception err) {
                    Log.e(TAG, "error: " + err.toString());
                    return false;
                }
            }
        });

        btnUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(disableControl){
                    Toast.makeText(CameraActivity.this, "您没有控制摄像头的权限", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    if (m_iLogID < 0) {
                        Log.e(TAG, "please login on a device first");
                        return false;
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.TILT_UP, 0)) {
                            Log.e(TAG,
                                    "start TILT_UP failed with error code: "
                                            + HCNetSDK.getInstance()
                                            .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "start TILT_UP success");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.TILT_UP, 1)) {
                            Log.e(TAG, "stop TILT_UP failed with error code: "
                                    + HCNetSDK.getInstance()
                                    .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "stop TILT_UP success");
                        }
                    }
                    return true;
                } catch (Exception err) {
                    Log.e(TAG, "error: " + err.toString());
                    return false;
                }
            }
        });

        btnDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(disableControl){
                    Toast.makeText(CameraActivity.this, "您没有控制摄像头的权限", Toast.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    if (m_iLogID < 0) {
                        Log.e(TAG, "please login on a device first");
                        return false;
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.TILT_DOWN, 0)) {
                            Log.e(TAG,
                                    "start TILT_DOWN failed with error code: "
                                            + HCNetSDK.getInstance()
                                            .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "start TILT_DOWN success");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.TILT_DOWN, 1)) {
                            Log.e(TAG, "stop TILT_DOWN failed with error code: "
                                    + HCNetSDK.getInstance()
                                    .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "stop TILT_DOWN success");
                        }
                    }
                    return true;
                } catch (Exception err) {
                    Log.e(TAG, "error: " + err.toString());
                    return false;
                }
            }
        });

        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(disableControl){
                    Toast.makeText(CameraActivity.this, "您没有控制摄像头的权限", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    if (m_iLogID < 0) {
                        Log.e(TAG, "please login on a device first");
                        return false;
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.PAN_LEFT, 0)) {
                            Log.e(TAG,
                                    "start PAN_LEFT failed with error code: "
                                            + HCNetSDK.getInstance()
                                            .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "start PAN_LEFT success");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.PAN_LEFT, 1)) {
                            Log.e(TAG, "stop PAN_LEFT failed with error code: "
                                    + HCNetSDK.getInstance()
                                    .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "stop PAN_LEFT success");
                        }
                    }
                    return true;
                } catch (Exception err) {
                    Log.e(TAG, "error: " + err.toString());
                    return false;
                }
            }
        });

        btnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(disableControl){
                    Toast.makeText(CameraActivity.this, "您没有控制摄像头的权限", Toast.LENGTH_SHORT).show();
                    return false;
                }
                try {
                    if (m_iLogID < 0) {
                        Log.e(TAG, "please login on a device first");
                        return false;
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.PAN_RIGHT, 0)) {
                            Log.e(TAG,
                                    "start PAN_RIGHT failed with error code: "
                                            + HCNetSDK.getInstance()
                                            .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "start PAN_RIGHT success");
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (!HCNetSDK.getInstance().NET_DVR_PTZControl_Other(
                                m_iLogID, m_iStartChan + channelId, PTZCommand.PAN_RIGHT, 1)) {
                            Log.e(TAG, "stop PAN_RIGHT failed with error code: "
                                    + HCNetSDK.getInstance()
                                    .NET_DVR_GetLastError());
                        } else {
                            Log.i(TAG, "stop PAN_RIGHT success");
                        }
                    }
                    return true;
                } catch (Exception err) {
                    Log.e(TAG, "error: " + err.toString());
                    return false;
                }
            }
        });

        
    }

    private void realInit(){
        Log.e(TAG,"realInit");
        CrashUtil crashUtil = CrashUtil.getInstance();
        crashUtil.init(this);

        if (!initeSdk()) {
            this.finish();
            return;
        }

        if (!initeActivity()) {
            this.finish();
            return;
        }
    }

    private void startRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQ_STORAGE);
        }else{
            realInit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        Log.e(TAG, "request permission result : permissions:"+permissions[0]+","+permissions[1]+", results:"+grantResults[0]+","+grantResults[1]);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_REQ_STORAGE){
            if(grantResults.length>1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                realInit();
            } else{
                new AlertDialog.Builder(this).setTitle("权限不足").setMessage("未授予执行此操作所必须的权限")
                        .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                CameraActivity.this.finish();
                            }
                        }).show();
            }
        }
    }

    // GUI init
    private boolean initeActivity() {
        initView();
        m_osurfaceView.getHolder().addCallback(this);

        if(loginCamera()){
            startPreview();
        }else {
            new AlertDialog.Builder(this)
                    .setTitle("连接失败")
                    .setMessage("暂时无法连接到监控设备，请稍后重试。")
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            CameraActivity.this.finish();
                        }
                    }).show();

        }


        return true;
    }

    private boolean loginCamera() {
        try {
            // login on the device
            m_iLogID = loginDevice();
            if (m_iLogID < 0) {
                Log.e(TAG, "This device logins failed!");
                return false;
            } else {
                Log.i(TAG, "loginDevice success. m_iLogID=" + m_iLogID);
            }

            // get instance of exception callback and set
            ExceptionCallBack oexceptionCbf = getExceptiongCbf();
            if (oexceptionCbf == null) {
                Log.e(TAG, "ExceptionCallBack object is failed!");
                return false;
            }

            if (!HCNetSDK.getInstance().NET_DVR_SetExceptionCallBack(
                    oexceptionCbf)) {
                Log.e(TAG, "NET_DVR_SetExceptionCallBack is failed!");
                return false;
            }

            Log.i(TAG, "Login sucess ****************************1***************************");
            return true;

        } catch (Exception err) {
            Log.e(TAG, "error: " + err.toString());
            return false;
        }

    }

    private void startPreview(){
        try {
           startSinglePreview();
        } catch (Exception err) {
            Log.e(TAG, "error: " + err.toString());
        }
    }

    private void stopPreview(){
        stopSinglePreview();
    }

    private void logOutCamera(){
        // whether we have logout
        if (!HCNetSDK.getInstance().NET_DVR_Logout_V30(m_iLogID)) {
            Log.e(TAG, " NET_DVR_Logout is failed!");
            return;
        }
        m_iLogID = -1;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("m_iPort", m_iPort);
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        m_iPort = savedInstanceState.getInt("m_iPort");
        super.onRestoreInstanceState(savedInstanceState);
        Log.i(TAG, "onRestoreInstanceState");
    }

    /**
     * @fn initeSdk
     * @author zhuzhenlei
     * @brief SDK init
     * @return true - success;false - fail
     */
    private boolean initeSdk() {
        // init net sdk
        if (!HCNetSDK.getInstance().NET_DVR_Init()) {
            Log.e(TAG, "HCNetSDK init is failed!");
            return false;
        }
        HCNetSDK.getInstance().NET_DVR_SetLogToFile(3, "/mnt/sdcard/sdklog/",
                true);
        return true;
    }


    private void startSinglePreview() {
        if (m_iPlaybackID >= 0) {
            Log.i(TAG, "Please stop palyback first");
            return;
        }
        RealPlayCallBack fRealDataCallBack = getRealPlayerCbf();
        if (fRealDataCallBack == null) {
            Log.e(TAG, "fRealDataCallBack object is failed!");
            return;
        }
        Log.i(TAG, "m_iStartChan:" + m_iStartChan);

        NET_DVR_PREVIEWINFO previewInfo = new NET_DVR_PREVIEWINFO();
        previewInfo.lChannel = m_iStartChan + channelId;
        previewInfo.dwStreamType = 0; // substream
        previewInfo.bBlocked = 1;
//         NET_DVR_CLIENTINFO struClienInfo = new NET_DVR_CLIENTINFO();
//         struClienInfo.lChannel = m_iStartChan;
//         struClienInfo.lLinkMode = 0;
        // HCNetSDK start preview
        m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V40(m_iLogID,
                previewInfo, fRealDataCallBack);
//         m_iPlayID = HCNetSDK.getInstance().NET_DVR_RealPlay_V30(m_iLogID,
//         struClienInfo, fRealDataCallBack, false);
        if (m_iPlayID < 0) {
            Log.e(TAG, "NET_DVR_RealPlay is failed!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return;
        }

        Log.i(TAG, "NetSdk Play sucess ***********************3***************************");

    }



    /**
     * @fn stopSinglePreview
     * @author zhuzhenlei
     * @brief stop preview
     * @return NULL
     */
    private void stopSinglePreview() {
        Player.getInstance().stopSound();
        // player stop play
        if (!Player.getInstance().stop(m_iPort)) {
            Log.e(TAG, "stop is failed!");
            return;
        }

        if (!Player.getInstance().closeStream(m_iPort)) {
            Log.e(TAG, "closeStream is failed!");
            return;
        }
        if (!Player.getInstance().freePort(m_iPort)) {
            Log.e(TAG, "freePort is failed!" + m_iPort);
            return;
        }
        m_iPort = -1;
    }

    /**
     * @fn loginNormalDevice
     * @author zhuzhenlei
     * @brief login on device
     * @return login ID
     */
    private int loginNormalDevice() {
        // get instance
        m_oNetDvrDeviceInfoV30 = new NET_DVR_DEVICEINFO_V30();
        if (null == m_oNetDvrDeviceInfoV30) {
            Log.e(TAG, "HKNetDvrDeviceInfoV30 new is failed!");
            return -1;
        }
        String strIP = cameraIp;
        int nPort = Integer.parseInt(cameraPort);
        String strUser = userName;
        String strPsd = password;
        // call NET_DVR_Login_v30 to login on, port 8000 as default
        int iLogID = HCNetSDK.getInstance().NET_DVR_Login_V30(strIP, nPort,
                strUser, strPsd, m_oNetDvrDeviceInfoV30);
        if (iLogID < 0) {
            Log.e(TAG, "NET_DVR_Login is failed!Err:"
                    + HCNetSDK.getInstance().NET_DVR_GetLastError());
            return -1;
        }
        if (m_oNetDvrDeviceInfoV30.byChanNum > 0) {
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byChanNum;
        } else if (m_oNetDvrDeviceInfoV30.byIPChanNum > 0) {
            m_iStartChan = m_oNetDvrDeviceInfoV30.byStartDChan;
            m_iChanNum = m_oNetDvrDeviceInfoV30.byIPChanNum
                    + m_oNetDvrDeviceInfoV30.byHighDChanNum * 256;
        }
        Log.i(TAG, "NET_DVR_Login is Successful!");

        return iLogID;
    }





    // @Override
    public void surfaceCreated(SurfaceHolder holder) {
        m_osurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        Log.i(TAG, "surface is created" + m_iPort);
        m_bSurfaceCreated = true;
        if (-1 == m_iPort) {
            return;
        }
        Surface surface = holder.getSurface();
        if (true == surface.isValid()) {
            if (false == Player.getInstance()
                    .setVideoWindow(m_iPort, 0, holder)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }

    // @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    // @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Player setVideoWindow release!" + m_iPort);
        if (-1 == m_iPort) {
            return;
        }
        if (true == holder.getSurface().isValid()) {
            if (false == Player.getInstance().setVideoWindow(m_iPort, 0, null)) {
                Log.e(TAG, "Player setVideoWindow failed!");
            }
        }
    }


    /**
     * @fn getRealPlayerCbf
     * @author zhuzhenlei
     * @brief get realplay callback instance
     *            [out]
     * @return callback instance
     */
    private RealPlayCallBack getRealPlayerCbf() {
        RealPlayCallBack cbf = new RealPlayCallBack() {
            public void fRealDataCallBack(int iRealHandle, int iDataType,
                                          byte[] pDataBuffer, int iDataSize) {
                // player channel 1
                CameraActivity.this.processRealData(1, iDataType, pDataBuffer,
                        iDataSize, Player.STREAM_REALTIME);
            }
        };
        return cbf;
    }

    /**
     * @fn loginDevice
     * @author zhangqing
     * @brief login on device
     * @return login ID
     */
    private int loginDevice() {
        int iLogID = -1;

        iLogID = loginNormalDevice();


        return iLogID;
    }



    /**
     * @fn getExceptiongCbf
     * @author zhuzhenlei
     * @brief process exception
     * @return exception instance
     */
    private ExceptionCallBack getExceptiongCbf() {
        ExceptionCallBack oExceptionCbf = new ExceptionCallBack() {
            public void fExceptionCallBack(int iType, int iUserID, int iHandle) {
                System.out.println("recv exception, type:" + iType);
            }
        };
        return oExceptionCbf;
    }

    /**
     * @fn processRealData
     * @author zhuzhenlei
     * @brief process real data
     * @param iPlayViewNo
     *            - player channel [in]
     * @param iDataType
     *            - data type [in]
     * @param pDataBuffer
     *            - data buffer [in]
     * @param iDataSize
     *            - data size [in]
     * @param iStreamMode
     *            - stream mode [in]
     * @return NULL
     */
    public void processRealData(int iPlayViewNo, int iDataType,
                                byte[] pDataBuffer, int iDataSize, int iStreamMode) {
        //   Log.i(TAG, "iPlayViewNo:" + iPlayViewNo + ",iDataType:" + iDataType + ",iDataSize:" + iDataSize);
        if(HCNetSDK.NET_DVR_SYSHEAD == iDataType)
        {
            if(m_iPort >= 0)
            {
                return;
            }
            m_iPort = Player.getInstance().getPort();
            if(m_iPort == -1)
            {
                Log.e(TAG, "getPort is failed with: " + Player.getInstance().getLastError(m_iPort));
                return;
            }
            Log.i(TAG, "getPort succ with: " + m_iPort);
            if (iDataSize > 0)
            {
                if (!Player.getInstance().setStreamOpenMode(m_iPort, iStreamMode))  //set stream mode
                {
                    Log.e(TAG, "setStreamOpenMode failed");
                    return;
                }
                if (!Player.getInstance().openStream(m_iPort, pDataBuffer, iDataSize, 2*1024*1024)) //open stream
                {
                    Log.e(TAG, "openStream failed");
                    return;
                }
                while(!m_bSurfaceCreated)
                {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    Log.i(TAG, "wait 100 for surface, handle:" + iPlayViewNo);
                }

                if (!Player.getInstance().play(m_iPort, m_osurfaceView.getHolder()))
                {
                    Log.e(TAG, "play failed,error:" + Player.getInstance().getLastError(m_iPort));
                    return;
                }
                if(!Player.getInstance().playSound(m_iPort))
                {
                    Log.e(TAG, "playSound failed with error code:" + Player.getInstance().getLastError(m_iPort));
                    return;
                }
            }
        }
        else
        {
            if (!Player.getInstance().inputData(m_iPort, pDataBuffer, iDataSize))
            {
                Log.e(TAG, "inputData failed with: " + Player.getInstance().getLastError(m_iPort));
            }
        }

    }



    @Override
    protected void onStart() {
        Log.i(TAG,"onStart");
        super.onStart();

    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();

        stopPreview();
        logOutCamera();
//        cleanup();

        this.finish();
    }

    /**
     * @fn cleanup
     * @author zhuzhenlei
     * @brief cleanup
     * @return NULL
     */
    public void cleanup() {
        // release player resource

        Player.getInstance().freePort(m_iPort);
        m_iPort = -1;

        // release net SDK resource
        HCNetSDK.getInstance().NET_DVR_Cleanup();
    }
}
