package com.p2pvideo.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.Log;
import androidx.activity.result.ActivityResult;
import android.widget.Toast;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.ActivityCallback;

@CapacitorPlugin(name = "ScreenCapture")
public class ScreenCapturePlugin extends Plugin {
    private static final String TAG = "ScreenCapturePlugin";
    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private Intent permissionIntent;
    private int permissionResultCode;
    private PluginCall savedCall;

    @Override
    public void load() {
        super.load();
        mediaProjectionManager = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Log.d(TAG, "ScreenCapturePlugin loaded successfully");
    }

    @PluginMethod
    public void requestScreenCapturePermission(PluginCall call) {
        Log.d(TAG, "requestScreenCapturePermission called");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Save the call for later use in callback
                savedCall = call;
                
                // Create the screen capture intent
                Intent intent = mediaProjectionManager.createScreenCaptureIntent();
                Log.d(TAG, "Created screen capture intent, launching permission dialog");
                
                // Show toast to user
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Requesting screen sharing permission...", Toast.LENGTH_LONG).show();
                });
                
                // Start the activity for result - this will show the system permission dialog
                startActivityForResult(call, intent, "handleScreenCaptureResult");
                Log.d(TAG, "startActivityForResult called successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error requesting screen capture permission", e);
                JSObject ret = new JSObject();
                ret.put("granted", false);
                ret.put("error", e.getMessage());
                call.resolve(ret);
            }
        } else {
            JSObject ret = new JSObject();
            ret.put("granted", false);
            ret.put("error", "Screen capture requires Android 5.0 (Lollipop) or higher");
            call.resolve(ret);
        }
    }

    @ActivityCallback
    private void handleScreenCaptureResult(PluginCall call, ActivityResult result) {
        Log.d(TAG, "handleScreenCaptureResult - resultCode: " + result.getResultCode() + ", RESULT_OK: " + Activity.RESULT_OK);
        
        JSObject ret = new JSObject();
        
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent data = result.getData();
            
            if (data != null) {
                // Store the permission data for later use
                permissionIntent = data;
                permissionResultCode = result.getResultCode();
                
                Log.d(TAG, "Screen capture permission granted, data received");
                
                // Immediately start screen capture
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mediaProjection = mediaProjectionManager.getMediaProjection(permissionResultCode, permissionIntent);
                        
                        if (mediaProjection != null) {
                            ret.put("granted", true);
                            ret.put("started", true);
                            ret.put("message", "Screen capture started successfully");
                            Log.d(TAG, "MediaProjection created successfully");
                            
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(), "Screen sharing started!", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            ret.put("granted", true);
                            ret.put("started", false);
                            ret.put("error", "MediaProjection is null");
                            Log.e(TAG, "MediaProjection is null after creation");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error starting media projection", e);
                    ret.put("granted", true);
                    ret.put("started", false);
                    ret.put("error", e.getMessage());
                }
            } else {
                Log.e(TAG, "Result data is null");
                ret.put("granted", false);
                ret.put("error", "Result data is null");
            }
            
            call.resolve(ret);
        } else {
            Log.d(TAG, "Screen capture permission denied or cancelled");
            ret.put("granted", false);
            ret.put("message", "Permission denied or cancelled");
            
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "Screen sharing cancelled", Toast.LENGTH_SHORT).show();
            });
            
            call.resolve(ret);
        }
    }

    @PluginMethod
    public void startScreenCapture(PluginCall call) {
        Log.d(TAG, "startScreenCapture called");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (permissionIntent != null && permissionResultCode == Activity.RESULT_OK) {
                try {
                    if (mediaProjection == null) {
                        mediaProjection = mediaProjectionManager.getMediaProjection(permissionResultCode, permissionIntent);
                    }
                    
                    JSObject ret = new JSObject();
                    ret.put("success", true);
                    ret.put("message", "Screen capture started");
                    call.resolve(ret);
                    Log.d(TAG, "Screen capture started successfully");
                } catch (Exception e) {
                    call.reject("Failed to start screen capture: " + e.getMessage());
                    Log.e(TAG, "Error starting screen capture", e);
                }
            } else {
                Log.w(TAG, "Permission not granted, requesting again");
                // Automatically request permission if not granted
                requestScreenCapturePermission(call);
            }
        } else {
            call.reject("Screen capture not supported on this Android version");
        }
    }

    @PluginMethod
    public void stopScreenCapture(PluginCall call) {
        Log.d(TAG, "stopScreenCapture called");
        
        if (mediaProjection != null) {
            try {
                mediaProjection.stop();
                mediaProjection = null;
                permissionIntent = null;
                permissionResultCode = 0;
                Log.d(TAG, "Screen capture stopped successfully");
                
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Screen sharing stopped", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error stopping screen capture", e);
            }
        }
        
        JSObject ret = new JSObject();
        ret.put("success", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void isScreenCaptureSupported(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("supported", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
        call.resolve(ret);
    }
    
    @PluginMethod
    public void getPermissionStatus(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("hasPermission", permissionIntent != null && permissionResultCode == Activity.RESULT_OK);
        ret.put("isActive", mediaProjection != null);
        call.resolve(ret);
    }
}
