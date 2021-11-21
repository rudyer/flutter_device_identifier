package com.drox.flutter_device_identifier;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/** DeviceIdentifierPlugin */
public class FlutterDeviceIdentifierPlugin implements MethodCallHandler, PluginRegistry.RequestPermissionsResultListener,  FlutterPlugin, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel methodChannel;
  private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
  private Result result;
  private Activity activity;
  private Context activeContext;
  private static final String TAG = "FlutterDeviceIdentifierPlugin";

  public FlutterDeviceIdentifierPlugin() {
  }


  @SuppressLint("LongLogTag")
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    Log.v(TAG, "Attached to engine");

    methodChannel = new MethodChannel(binding.getBinaryMessenger(), "flutter_device_identifier");
    methodChannel.setMethodCallHandler(this);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (methodChannel != null) methodChannel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.activity = binding.getActivity();
    binding.addRequestPermissionsResultListener(this);
    activity = binding.getActivity();
    activeContext = binding.getActivity().getApplicationContext();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {

  }



  @SuppressLint({"HardwareIds", "LongLogTag"})
  private String getIMEI(Context c) {
    Log.i(TAG, "ATTEMPTING TO getIMEI: ");
    TelephonyManager telephonyManager;
    telephonyManager = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);

    String deviceId;
    if (telephonyManager.getDeviceId() == null) {
      deviceId = "returned null";
    } else {
      deviceId = telephonyManager.getDeviceId();
    }
    return deviceId;
  }

  @SuppressLint({"LongLogTag", "HardwareIds"})
  private String getSerial() {
    Log.i(TAG, "ATTEMPTING TO getSerial: ");
    String serial;

    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      serial = Build.getSerial();
    } else {
      serial = Build.SERIAL;
    }
    if (serial == null) {
      serial = "returned null";
    }

    return serial;
  }

  @SuppressLint({"HardwareIds", "LongLogTag"})
  private String getAndroidID(Context c) {
    Log.i(TAG, "ATTEMPTING TO getAndroidID: ");
    String androidId;
    androidId = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
    if (androidId == null) {
      androidId = "returned null";
    }
    return androidId;
  }

  private Map<String, String> getIdMap(Context c) {
    String imei = getIMEI(c);
    String serial = getSerial();
    String androidId = getAndroidID(c);
    Map<String, String> idMap = new HashMap<>();
    idMap.put("imei", imei);
    idMap.put("serial", serial);
    idMap.put("androidId", androidId);
    return idMap;
  }

  private boolean checkPermission(Activity thisActivity) {
    boolean res = false;
    if (ContextCompat.checkSelfPermission(thisActivity,
            Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
      res = true;
    }
    return res;
  }

  private boolean checkPermissionRationale(Activity thisActivity) {
    boolean res = false;
    if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity, Manifest.permission.READ_PHONE_STATE)) {
      res = true;
    }
    return res;
  }

  private Map<String, Boolean> checkPermissionMap(Activity activity) {
    Map<String, Boolean> resultMap = new HashMap<>();
    resultMap.put("isGranted", checkPermission(activity));
    resultMap.put("isRejected", checkPermissionRationale(activity));
    return resultMap;
  }

  @SuppressLint("LongLogTag")
  private void requestPermission(Activity thisActivity) {

    Log.i(TAG, "requestPermission: REQUESTING");
    ActivityCompat.requestPermissions(thisActivity, new String[] { Manifest.permission.READ_PHONE_STATE },
            MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);

  }

  @SuppressLint("AnnotateVersionCheck")
  private boolean isAPI23Up() {
    return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
  }

  private void openSettings() {
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + activity.getPackageName()));
    intent.addCategory(Intent.CATEGORY_DEFAULT);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activity.startActivity(intent);
  }

  @Override
  public void onMethodCall(MethodCall call, @NonNull Result res) {
    if (call.method.equals("getPlatformVersion")) {
      res.success("Android " + android.os.Build.VERSION.RELEASE);
      return;
    }
    if (call.method.equals("getIMEI")) {
      String imei = getIMEI(activity.getBaseContext());

      res.success(imei);
      return;
    }

    if (call.method.equals("getSerial")) {
      String serial = getSerial();

      res.success(serial);
      return;
    }
    if (call.method.equals("getAndroidID")) {
      String androidID = getAndroidID(activity.getBaseContext());

      res.success(androidID);
      return;
    }
    if (call.method.equals("getIdMap")) {
      Map idMap = getIdMap(activity.getBaseContext());
      res.success(idMap);
      return;
    }
    if (call.method.equals("checkPermissionMap")) {
      Map<String, Boolean> response = new HashMap<>();
      if (isAPI23Up()) {
        response = checkPermissionMap(activity);
      } else {
        response.put("isGranted", true);
        response.put("isRejected", false);
      }
      res.success(response);
      return;
    }
    if (call.method.equals("checkPermission")) {

      boolean response = !isAPI23Up() || checkPermission(activity);
      res.success(response);
      return;
    }
    if (call.method.equals("checkPermissionRationale")) {
      boolean response = isAPI23Up() && checkPermissionRationale(activity);
      res.success(response);
      return;
    }
    if (call.method.equals("requestPermission")) {
      this.result = res;
      if (isAPI23Up()) {
        requestPermission(activity);
      } else {
        Map<String, Boolean> oldAPIStatusMap = new HashMap<>();
        oldAPIStatusMap.put("status", true);
        oldAPIStatusMap.put("neverAskAgain", false);
        res.success(oldAPIStatusMap);
      }
      return;

    }
    if (call.method.equals("openSettings")) {

      // result.success(true);
      openSettings();
      return;
    }

    res.notImplemented();

  }

  @SuppressLint("LongLogTag")
  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    Map<String, Boolean> statusMap = new HashMap<>();
    statusMap.put("status", false);
    statusMap.put("neverAskAgain", false);
    String permission = permissions[0];
    Log.i(TAG, "requestResponse: INITIALIZED");
    if (requestCode == 0 && grantResults.length > 0) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
        Log.e("ResquestResponse", "DENIED: " + permission);// allowed//denied
        statusMap.put("status", false);
      } else {
        if (ActivityCompat.checkSelfPermission(activeContext, permission) == PackageManager.PERMISSION_GRANTED) {
          Log.e("ResquestResponse", "ALLOWED: " + permission);// allowed
          statusMap.put("status", true);
        } else {
          // set to never ask again
          Log.e("ResquestResponse", "set to never ask again" + permission);
          statusMap.put("neverAskAgain", true);
        }
      }
    }

    Result res = this.result;
    this.result = null;
    if (res != null) {
      try {
        Log.i(TAG, "onRequestPermissionsResult: Returning result");
        res.success(statusMap);
      } catch (IllegalStateException e) {
        Log.i(TAG, "onRequestPermissionsResult: Illegal state, NOT Returning result");
        return false;
      }
    } else {
      Log.i(TAG, "onRequestPermissionsResult: NOT Returning result");
      return false;
    }
    return true;
  }
}
