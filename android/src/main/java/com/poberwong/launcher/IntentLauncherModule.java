package com.poberwong.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.blankj.utilcode.util.AppUtils;
import com.facebook.react.bridge.*;

import java.io.Console;
import java.io.File;
import java.util.Set;
import java.util.Iterator;

import androidx.core.content.FileProvider;

/**
 * Created by poberwong on 16/6/30.
 */
public class IntentLauncherModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private static final int REQUEST_CODE = 12;
    private static final String ATTR_ACTION = "action";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_CATEGORY = "category";
    private static final String TAG_EXTRA = "extra";
    private static final String ATTR_DATA = "data";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_CLASS_NAME = "className";
    Promise promise;
    boolean hasError = false;
    String errorMessage = "";


    public IntentLauncherModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "IntentLauncher";
    }

    /**
     * 选用方案
     * intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     * getReactApplicationContext().startActivity(intent);
     */
    @ReactMethod
    public void startActivity(ReadableMap params, final Promise promise) {
        this.promise = promise;
        this.errorMessage = "";
        this.hasError = false;
        Intent intent = new Intent();

        if (params.hasKey(ATTR_CLASS_NAME)) {
            ComponentName cn;
            if (params.hasKey(ATTR_PACKAGE_NAME)) {
                cn = new ComponentName(params.getString(ATTR_PACKAGE_NAME), params.getString(ATTR_CLASS_NAME));
            } else {
                cn = new ComponentName(getReactApplicationContext(), params.getString(ATTR_CLASS_NAME));
            }
            intent.setComponent(cn);
        }
        if (params.hasKey(ATTR_ACTION)) {
            intent.setAction(params.getString(ATTR_ACTION));
        }
        //must use setDataAndType
        Uri contentUri = null;
        if (params.hasKey(ATTR_DATA)) {
            String path = params.getString(ATTR_DATA);
            if(path.startsWith("content://")) {
                contentUri = Uri.parse(path);
            } else {
                //判断是否为文件
                boolean isFile = false;
                try {
                    File newFile = new File(path);
                    isFile = newFile.exists();
                    if(isFile) {
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            final String packageName = getCurrentActivity().getPackageName();
                            final String authority = new StringBuilder(packageName).append(".intent_provider").toString();
                            contentUri = FileProvider.getUriForFile(getCurrentActivity(), authority, newFile);
                        } else {
                            contentUri = Uri.fromFile(newFile);
                        }
                    } else {
                        contentUri = Uri.parse(path);
                    }
                } catch (Exception e) {

                }
            }
        }
        if(contentUri!=null && params.hasKey(ATTR_TYPE)) {
            intent.setDataAndType(contentUri, params.getString(ATTR_TYPE));
        } else {
            //just set data or type,another will be set to null
            if(contentUri!=null) {
                intent.setData(contentUri);
            }
            if (params.hasKey(ATTR_TYPE)) {
                intent.setType(params.getString(ATTR_TYPE));
            }
        }
        if (params.hasKey(TAG_EXTRA)) {
            intent.putExtras(Arguments.toBundle(params.getMap(TAG_EXTRA)));
        }
        if (params.hasKey(ATTR_FLAGS)) {
            intent.addFlags(params.getInt(ATTR_FLAGS));
        }
        if (params.hasKey(ATTR_CATEGORY)) {
            intent.addCategory(params.getString(ATTR_CATEGORY));
        }
        try
        {
            getReactApplicationContext().startActivityForResult(intent, REQUEST_CODE, null); // 暂时使用当前应用的任务栈
        }
        catch(Exception e)
        {
            //不能直接reject，否则跟下面的方式会同时执行promise操作，会报错
            this.hasError = true;
            this.errorMessage = e.getMessage();
//            this.promise.reject("-1", e.getMessage());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (requestCode != REQUEST_CODE) {
            return;
        }
        WritableMap params = Arguments.createMap();
        if (intent != null) {
            params.putInt("resultCode", resultCode);

            Uri data = intent.getData();
            if (data != null) {
                params.putString("data", data.toString());
            }

            Bundle extras = intent.getExtras();
            if (extras != null) {
                params.putMap("extra", Arguments.fromBundle(extras));
            }
        }
        if(this.hasError)
        {
            this.promise.reject("-1",this.errorMessage);
        }
        else
        {
            this.promise.resolve(params);
        }
    }

    @ReactMethod
      public void finish(int result, String action, ReadableMap map) {
          Activity activity = getReactApplicationContext().getCurrentActivity();
          Intent intent = new Intent(action);
          intent.putExtras(Arguments.toBundle(map));
          activity.setResult(result, intent);
          activity.finish();
      }
}
