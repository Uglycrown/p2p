package com.p2pvideo.app;

import android.os.Bundle;
import android.view.WindowManager;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.util.Rational;
import android.os.Build;
import android.graphics.drawable.Icon;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;

public class MainActivity extends BridgeActivity {
    
    private static final String ACTION_MUTE = "com.p2pvideo.app.MUTE";
    private static final String ACTION_HANGUP = "com.p2pvideo.app.HANGUP";
    private static final String ACTION_CAMERA = "com.p2pvideo.app.CAMERA";
    private static final String ACTION_EXPAND = "com.p2pvideo.app.EXPAND";
    
    private BroadcastReceiver pipReceiver;
    private BroadcastReceiver callEndedReceiver;
    private boolean isInPipMode = false;
    private boolean isCallActive = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        );
        
        registerPlugin(PipPlugin.class);
        registerPlugin(CallServicePlugin.class);
        registerPlugin(ScreenCapturePlugin.class);
        registerPlugin(AudioRoutingPlugin.class);
        
        setupPipReceiver();
        setupCallEndedReceiver();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        enterPipMode();
    }
    
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        isInPipMode = isInPictureInPictureMode;
        getBridge().triggerWindowJSEvent("pipModeChanged", 
            "{\"isInPipMode\":" + isInPictureInPictureMode + "}");
    }
    
    private void setupPipReceiver() {
        pipReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    String action = intent.getAction();
                    if (ACTION_MUTE.equals(action)) {
                        getBridge().triggerWindowJSEvent("pipAction", "{\"action\":\"mute\"}");
                    } else if (ACTION_HANGUP.equals(action)) {
                        getBridge().triggerWindowJSEvent("pipAction", "{\"action\":\"hangup\"}");
                    } else if (ACTION_CAMERA.equals(action)) {
                        getBridge().triggerWindowJSEvent("pipAction", "{\"action\":\"camera\"}");
                    } else if (ACTION_EXPAND.equals(action)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
                            Intent expandIntent = new Intent(context, MainActivity.class);
                            expandIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                            startActivity(expandIntent);
                        }
                    }
                }
            }
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_MUTE);
            filter.addAction(ACTION_HANGUP);
            filter.addAction(ACTION_CAMERA);
            filter.addAction(ACTION_EXPAND);
            registerReceiver(pipReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_MUTE);
            filter.addAction(ACTION_HANGUP);
            filter.addAction(ACTION_CAMERA);
            filter.addAction(ACTION_EXPAND);
            registerReceiver(pipReceiver, filter);
        }
    }
    
    public void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Rational aspectRatio = new Rational(9, 16);
            ArrayList<RemoteAction> actions = new ArrayList<>();
            
            Intent muteIntent = new Intent(ACTION_MUTE);
            PendingIntent mutePending = PendingIntent.getBroadcast(this, 0, muteIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon muteIcon = Icon.createWithResource(this, android.R.drawable.ic_btn_speak_now);
            RemoteAction muteAction = new RemoteAction(muteIcon, "Mute", "Mute/Unmute", mutePending);
            actions.add(muteAction);
            
            Intent cameraIntent = new Intent(ACTION_CAMERA);
            PendingIntent cameraPending = PendingIntent.getBroadcast(this, 1, cameraIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon cameraIcon = Icon.createWithResource(this, android.R.drawable.ic_menu_camera);
            RemoteAction cameraAction = new RemoteAction(cameraIcon, "Camera", "Switch Camera", cameraPending);
            actions.add(cameraAction);
            
            Intent expandIntent = new Intent(ACTION_EXPAND);
            PendingIntent expandPending = PendingIntent.getBroadcast(this, 2, expandIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon expandIcon = Icon.createWithResource(this, android.R.drawable.ic_menu_zoom);
            RemoteAction expandAction = new RemoteAction(expandIcon, "Expand", "Full Screen", expandPending);
            actions.add(expandAction);
            
            Intent hangupIntent = new Intent(ACTION_HANGUP);
            PendingIntent hangupPending = PendingIntent.getBroadcast(this, 3, hangupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Icon hangupIcon = Icon.createWithResource(this, android.R.drawable.ic_delete);
            RemoteAction hangupAction = new RemoteAction(hangupIcon, "Hang Up", "End Call", hangupPending);
            actions.add(hangupAction);
            
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .setActions(actions)
                    .build();
            enterPictureInPictureMode(params);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            enterPictureInPictureMode();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pipReceiver != null) {
            unregisterReceiver(pipReceiver);
        }
        if (callEndedReceiver != null) {
            unregisterReceiver(callEndedReceiver);
        }
        stopCallService();
    }
    
    private void setupCallEndedReceiver() {
        callEndedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                isCallActive = false;
                getBridge().triggerWindowJSEvent("callServiceEnded", "{}");
            }
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IntentFilter filter = new IntentFilter("com.p2pvideo.app.CALL_ENDED");
            registerReceiver(callEndedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            IntentFilter filter = new IntentFilter("com.p2pvideo.app.CALL_ENDED");
            registerReceiver(callEndedReceiver, filter);
        }
    }
    
    public void startCallService() {
        if (!isCallActive) {
            Intent serviceIntent = new Intent(this, CallService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            isCallActive = true;
        }
    }
    
    public void stopCallService() {
        if (isCallActive) {
            Intent serviceIntent = new Intent(this, CallService.class);
            stopService(serviceIntent);
            isCallActive = false;
        }
    }
}

@CapacitorPlugin(name = "PipPlugin")
class PipPlugin extends Plugin {
    @PluginMethod
    public void enterPipMode(PluginCall call) {
        MainActivity activity = (MainActivity) getActivity();
        activity.enterPipMode();
        call.resolve();
    }
    
    @PluginMethod
    public void isPipSupported(PluginCall call) {
        boolean supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
        call.resolve(new com.getcapacitor.JSObject().put("supported", supported));
    }
}

@CapacitorPlugin(name = "CallServicePlugin")
class CallServicePlugin extends Plugin {
    @PluginMethod
    public void startCall(PluginCall call) {
        MainActivity activity = (MainActivity) getActivity();
        activity.startCallService();
        call.resolve();
    }
    
    @PluginMethod
    public void endCall(PluginCall call) {
        MainActivity activity = (MainActivity) getActivity();
        activity.stopCallService();
        call.resolve();
    }
}
