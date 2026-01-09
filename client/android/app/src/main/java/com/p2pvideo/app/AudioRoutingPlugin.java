package com.p2pvideo.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(name = "AudioRouting")
public class AudioRoutingPlugin extends Plugin {
    private static final String TAG = "AudioRoutingPlugin";
    private AudioManager audioManager;
    private BroadcastReceiver audioDeviceReceiver;
    private boolean isBluetoothScoOn = false;

    @Override
    public void load() {
        super.load();
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        setupAudioDeviceListener();
        Log.d(TAG, "AudioRoutingPlugin loaded");
    }

    private void setupAudioDeviceListener() {
        audioDeviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "Audio device event: " + action);
                
                if (AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED.equals(action)) {
                    int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                    Log.d(TAG, "SCO state: " + state);
                    
                    JSObject ret = new JSObject();
                    ret.put("event", "scoStateChanged");
                    ret.put("state", state);
                    notifyListeners("audioDeviceChanged", ret);
                } else if (AudioManager.ACTION_HEADSET_PLUG.equals(action)) {
                    int state = intent.getIntExtra("state", -1);
                    Log.d(TAG, "Headset plug state: " + state);
                    
                    JSObject ret = new JSObject();
                    ret.put("event", "headsetPlugged");
                    ret.put("connected", state == 1);
                    notifyListeners("audioDeviceChanged", ret);
                } else if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, -1);
                    Log.d(TAG, "Bluetooth connection state: " + state);
                    
                    JSObject ret = new JSObject();
                    ret.put("event", "bluetoothConnectionChanged");
                    ret.put("connected", state == BluetoothAdapter.STATE_CONNECTED);
                    notifyListeners("audioDeviceChanged", ret);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().registerReceiver(audioDeviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            getContext().registerReceiver(audioDeviceReceiver, filter);
        }
    }

    @PluginMethod
    public void getAvailableAudioDevices(PluginCall call) {
        JSArray devices = new JSArray();
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioDeviceInfo[] outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                
                for (AudioDeviceInfo device : outputDevices) {
                    JSObject deviceInfo = new JSObject();
                    deviceInfo.put("id", device.getId());
                    deviceInfo.put("type", device.getType());
                    deviceInfo.put("productName", device.getProductName());
                    
                    String typeName = getDeviceTypeName(device.getType());
                    deviceInfo.put("typeName", typeName);
                    
                    // Check if it's a Bluetooth device
                    boolean isBluetooth = (device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                          device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
                    deviceInfo.put("isBluetooth", isBluetooth);
                    
                    // Check if it's a headphone/headset
                    boolean isHeadphone = (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                                          device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                                          device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET);
                    deviceInfo.put("isHeadphone", isHeadphone);
                    
                    devices.put(deviceInfo);
                    Log.d(TAG, "Found device: " + typeName + " - " + device.getProductName());
                }
            }
            
            // Check Bluetooth SCO separately
            boolean hasBluetoothSco = checkBluetoothHeadset();
            
            JSObject ret = new JSObject();
            ret.put("devices", devices);
            ret.put("hasBluetooth", hasBluetoothSco);
            ret.put("hasWiredHeadset", audioManager.isWiredHeadsetOn());
            ret.put("isSpeakerphoneOn", audioManager.isSpeakerphoneOn());
            ret.put("isBluetoothScoOn", audioManager.isBluetoothScoOn());
            ret.put("mode", audioManager.getMode());
            
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "Error getting audio devices", e);
            call.reject("Error: " + e.getMessage());
        }
    }

    @PluginMethod
    public void setAudioRoute(PluginCall call) {
        String route = call.getString("route");
        
        if (route == null) {
            call.reject("Route parameter is required");
            return;
        }
        
        Log.d(TAG, "Setting audio route to: " + route);
        
        try {
            // Set audio mode to IN_COMMUNICATION for proper routing
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            
            switch (route) {
                case "bluetooth":
                    setBluetoothRoute();
                    break;
                case "speaker":
                    setSpeakerRoute();
                    break;
                case "earpiece":
                    setEarpieceRoute();
                    break;
                case "headphones":
                    setHeadphonesRoute();
                    break;
                default:
                    call.reject("Unknown route: " + route);
                    return;
            }
            
            JSObject ret = new JSObject();
            ret.put("success", true);
            ret.put("route", route);
            call.resolve(ret);
        } catch (Exception e) {
            Log.e(TAG, "Error setting audio route", e);
            call.reject("Error: " + e.getMessage());
        }
    }

    private void setBluetoothRoute() {
        Log.d(TAG, "Enabling Bluetooth SCO");
        
        // Stop any existing SCO connection
        if (audioManager.isBluetoothScoOn()) {
            audioManager.stopBluetoothSco();
        }
        
        // Turn off speakerphone
        audioManager.setSpeakerphoneOn(false);
        
        // Start Bluetooth SCO
        audioManager.startBluetoothSco();
        audioManager.setBluetoothScoOn(true);
        isBluetoothScoOn = true;
        
        Log.d(TAG, "Bluetooth SCO enabled: " + audioManager.isBluetoothScoOn());
    }

    private void setSpeakerRoute() {
        Log.d(TAG, "Enabling speakerphone");
        
        // Stop Bluetooth SCO if active
        if (isBluetoothScoOn || audioManager.isBluetoothScoOn()) {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
            isBluetoothScoOn = false;
        }
        
        // Enable speakerphone
        audioManager.setSpeakerphoneOn(true);
        
        Log.d(TAG, "Speakerphone enabled: " + audioManager.isSpeakerphoneOn());
    }

    private void setEarpieceRoute() {
        Log.d(TAG, "Enabling earpiece");
        
        // Stop Bluetooth SCO if active
        if (isBluetoothScoOn || audioManager.isBluetoothScoOn()) {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
            isBluetoothScoOn = false;
        }
        
        // Disable speakerphone (audio will go to earpiece)
        audioManager.setSpeakerphoneOn(false);
        
        Log.d(TAG, "Earpiece enabled");
    }

    private void setHeadphonesRoute() {
        Log.d(TAG, "Enabling wired headphones");
        
        // Stop Bluetooth SCO if active
        if (isBluetoothScoOn || audioManager.isBluetoothScoOn()) {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
            isBluetoothScoOn = false;
        }
        
        // Disable speakerphone (audio will go to headphones)
        audioManager.setSpeakerphoneOn(false);
        
        Log.d(TAG, "Wired headphones enabled");
    }

    private boolean checkBluetoothHeadset() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                int profileState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
                boolean connected = profileState == BluetoothProfile.STATE_CONNECTED;
                Log.d(TAG, "Bluetooth headset connected: " + connected);
                return connected;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking Bluetooth headset", e);
        }
        return false;
    }

    private String getDeviceTypeName(int type) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            switch (type) {
                case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                    return "Earpiece";
                case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                    return "Speaker";
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                    return "Wired Headset";
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                    return "Wired Headphones";
                case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                    return "Bluetooth SCO";
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                    return "Bluetooth A2DP";
                case AudioDeviceInfo.TYPE_USB_HEADSET:
                    return "USB Headset";
                default:
                    return "Unknown (" + type + ")";
            }
        }
        return "Unknown";
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        
        // Clean up audio
        if (isBluetoothScoOn || audioManager.isBluetoothScoOn()) {
            audioManager.stopBluetoothSco();
            audioManager.setBluetoothScoOn(false);
        }
        
        // Unregister receiver
        if (audioDeviceReceiver != null) {
            try {
                getContext().unregisterReceiver(audioDeviceReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
        }
    }
}
