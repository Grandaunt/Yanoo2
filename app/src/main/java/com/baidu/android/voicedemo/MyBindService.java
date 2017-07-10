package com.baidu.android.voicedemo;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.VoiceRecognitionService;
import com.baidu.speech.recognizerdemo.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyBindService extends Service  {

    private IBinder myBinder;

    private Random mGenerator;
    private String access_token="24.443988db07896132ec13cac375b621cc.2592000.1501728772.282335-9840500";
    private final String TAG = "MyBindService";
    private EventManager mWpEventManager;

    private static final int REQUEST_UI = 1;

    public static final int STATUS_None = 0;
    public static final int STATUS_WaitingReady = 2;
    public static final int STATUS_Ready = 3;
    public static final int STATUS_Speaking = 4;
    public static final int STATUS_Recognition = 5;
    private SpeechRecognizer speechRecognizer;
    private int status = STATUS_None;
    private long speechEndTime = -1;
    private static final int EVENT_ERROR = 11;
    private String SpeekStr="思颖你好，祝你永远平安喜乐。";
//    private Callback dataCallback;
    public  DataCallback dataCallback = null;
    private String data="可以触发百度语音合成了";
    public class MyBinder extends Binder {
        MyBindService getService() {
            return MyBindService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        myBinder = new MyBinder();
        mGenerator = new Random();

        // 唤醒功能打开步骤
        // 1) 创建唤醒事件管理器
        mWpEventManager = EventManagerFactory.create(this, "wp");

        // 2) 注册唤醒事件监听器
        mWpEventManager.registerListener(new EventListener() {
            @Override
            public void onEvent(String name, String params, byte[] data, int offset, int length) {
                Log.d(TAG, String.format("event: name=%s, params=%s", name, params));
                try {
                    JSONObject json = new JSONObject(params);
                    if ("wp.data".equals(name)) { // 每次唤醒成功, 将会回调name=wp.data的时间, 被激活的唤醒词在params的word字段
                        String word = json.getString("word");
                        Log.i(TAG,"唤醒成功,唤醒词: " + word );
//                        txtLog.append("唤醒成功, 唤醒词: " + word + "\r\n");
//                        downloadTTs();
                        if (dataCallback  != null){
                            dataCallback .dataChanged("可以触发百度语音合成了");
                        }
                    } else if ("wp.exit".equals(name)) {
                        Log.i(TAG,"唤醒已经停止: " + params );
//                        txtLog.append("唤醒已经停止: " + params + "\r\n");
                    }
                } catch (JSONException e) {
                    throw new AndroidRuntimeException(e);
                }
            }
        });

        // 3) 通知唤醒管理器, 启动唤醒功能
        HashMap params = new HashMap();
        params.put("kws-file", "assets:///WakeUp.bin"); // 设置唤醒资源, 唤醒资源请到 http://yuyin.baidu.com/wake#m4 来评估和导出
        mWpEventManager.send("wp.start", new JSONObject(params).toString(), null, 0, 0);
//Log.i(TAG,"DESC_TEXT="+DESC_TEXT);
//        txtLog.setText(DESC_TEXT);
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, new ComponentName(this, VoiceRecognitionService.class));
//
//        speechRecognizer.setRecognitionListener(this);

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        return myBinder;
    }
    public class Binder extends android.os.Binder{
        public void setData(String data){
            MyBindService.this.data = data;
        }
        public MyBindService getMyService(){
            return MyBindService.this;
        }
    }
    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        mWpEventManager.send("wp.stop", null, null, 0, 0);
        speechRecognizer.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        return true;
        //return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.e(TAG, "onRebind");
        super.onRebind(intent);
    }

    public int getRandomNumber() {
        return mGenerator.nextInt(100);
    }



    public DataCallback getDataCallback() {
                return dataCallback;
            }

            public void setDataCallback(DataCallback dataCallback) {
               this.dataCallback = dataCallback;
            }

            // 通过回调机制，将Service内部的变化传递到外部
            public interface DataCallback {
         void dataChanged(String str);
     }


}
