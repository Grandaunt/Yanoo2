package com.baidu.android.voicedemo;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.VoiceRecognitionService;
import com.baidu.speech.recognizerdemo.R;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import  com.baidu.android.voicedemo.ContentBean;
import  com.baidu.android.voicedemo.responseBean;
import com.google.gson.Gson;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.google.gson.reflect.TypeToken;

public class ActivityWakeUp extends Activity implements RecognitionListener{
    private static final String TAG = "ActivityWakeUp";
    private static final int REQUEST_UI = 1;
    private TextView txtLog;
//    private Button btn;
    private Button setting;
    private MediaPlayer mediaPlayer;
    /**
     * 申请的turing的apikey（测试使用）
     * d975f8141aa550cea27b7f48dd50c48d
     * **/
    private final String TURING_APIKEY = "21352615ac1e47119fdc3ab84d7a0927";
    public static final int STATUS_None = 0;
    public static final int STATUS_WaitingReady = 2;
    public static final int STATUS_Ready = 3;
    public static final int STATUS_Speaking = 4;
    public static final int STATUS_Recognition = 5;
    private SpeechRecognizer speechRecognizer;
    private int status = STATUS_None;
    private TextView txtResult;
    private long speechEndTime = -1;
    private static final int EVENT_ERROR = 11;
    private Gson gson;
       MyServiceConn myServiceConn;
       MyBindService.MyBinder binder = null;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
//    小言你好，你好小言，小言说话， 茄子，拍照，打开电灯，下一首，增大音量，暂停，停止，播放，上一首，减小音量
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sdk2_api);
        gson=new Gson();
//        if(isServiceRunning(ActivityWakeUp.this,"MyBindService")){
//
//        }
//        else{
            //绑定service
            myServiceConn = new MyServiceConn();
            Intent intent = new Intent(this, MyBindService.class);
            startService(intent);
            bindService(intent, myServiceConn, Context.BIND_AUTO_CREATE);
//        }


        txtResult = (TextView) findViewById(R.id.txtResult);
        txtLog = (TextView) findViewById(R.id.txtLog);
//        btn = (Button) findViewById(R.id.btn);
        setting = (Button) findViewById(R.id.setting);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this, new ComponentName(this, VoiceRecognitionService.class));

        speechRecognizer.setRecognitionListener(this);
        setting.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.baidu.speech.asr.demo.setting");
                startActivity(intent);
            }
        });

    }



    /***
     * 绑定Service
     */
    class MyServiceConn implements ServiceConnection {
        // 服务被绑定成功之后执行
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // IBinder service为onBind方法返回的Service实例
            binder = (MyBindService.MyBinder) service;
            binder.getService().setDataCallback(new MyBindService.DataCallback() {
                //执行回调函数
                @Override
                public void dataChanged(String str) {
                    Log.i(TAG,"service可以调用信息"+str);
                    start();
//                    Message msg = new Message();
//                    Bundle bundle = new Bundle();
//                    bundle.putString("str", str);
//                    msg.setData(bundle);
//                    //发送通知
//                    handler.sendMessage(msg);
                }
            });
        }
//
//        @SuppressLint("HandlerLeak")
//        Handler handler = new Handler() {
//            public void handleMessage(android.os.Message msg) {
//                //在handler中更新UI
//                tv_out.setText(msg.getData().getString("str"));
//            }
//
//            ;
//        };

        // 服务奔溃或者被杀掉执行
        @Override
        public void onServiceDisconnected(ComponentName name) {
            binder = null;
        }
    }
    @Override
    protected void onDestroy() {
        Log.i(TAG,"onDestroy");
        binder = null;
        speechRecognizer.destroy();
        if (null != mediaPlayer) {
            mediaPlayer.release();
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        Log.i(TAG,"onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.i(TAG,"onRestart");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG,"onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG,"onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(TAG,"onStop");
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            onResults(data.getExtras());
        }
    }

    public void bindParams(Intent intent) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("tips_sound", true)) {
            intent.putExtra(Constant.EXTRA_SOUND_START, R.raw.bdspeech_recognition_start);
            intent.putExtra(Constant.EXTRA_SOUND_END, R.raw.bdspeech_speech_end);
            intent.putExtra(Constant.EXTRA_SOUND_SUCCESS, R.raw.bdspeech_recognition_success);
            intent.putExtra(Constant.EXTRA_SOUND_ERROR, R.raw.bdspeech_recognition_error);
            intent.putExtra(Constant.EXTRA_SOUND_CANCEL, R.raw.bdspeech_recognition_cancel);
        }
        if (sp.contains(Constant.EXTRA_INFILE)) {
            String tmp = sp.getString(Constant.EXTRA_INFILE, "").replaceAll(",.*", "").trim();
            intent.putExtra(Constant.EXTRA_INFILE, tmp);
        }
        if (sp.getBoolean(Constant.EXTRA_OUTFILE, false)) {
            intent.putExtra(Constant.EXTRA_OUTFILE, "sdcard/outfile.pcm");
        }
        if (sp.getBoolean(Constant.EXTRA_GRAMMAR, false)) {
            intent.putExtra(Constant.EXTRA_GRAMMAR, "assets:///baidu_speech_grammar.bsg");
        }
        if (sp.contains(Constant.EXTRA_SAMPLE)) {
            String tmp = sp.getString(Constant.EXTRA_SAMPLE, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_SAMPLE, Integer.parseInt(tmp));
            }
        }
        if (sp.contains(Constant.EXTRA_LANGUAGE)) {
            String tmp = sp.getString(Constant.EXTRA_LANGUAGE, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_LANGUAGE, tmp);
            }
        }
        if (sp.contains(Constant.EXTRA_NLU)) {
            String tmp = sp.getString(Constant.EXTRA_NLU, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_NLU, tmp);
            }
        }

        if (sp.contains(Constant.EXTRA_VAD)) {
            String tmp = sp.getString(Constant.EXTRA_VAD, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_VAD, tmp);
            }
        }
        String prop = null;
        if (sp.contains(Constant.EXTRA_PROP)) {
            String tmp = sp.getString(Constant.EXTRA_PROP, "").replaceAll(",.*", "").trim();
            if (null != tmp && !"".equals(tmp)) {
                intent.putExtra(Constant.EXTRA_PROP, Integer.parseInt(tmp));
                prop = tmp;
            }
        }

        // offline asr
        {
            intent.putExtra(Constant.EXTRA_OFFLINE_ASR_BASE_FILE_PATH, "/sdcard/easr/s_1");
            if (null != prop) {
                int propInt = Integer.parseInt(prop);
                if (propInt == 10060) {
                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_Navi");
                } else if (propInt == 20000) {
                    intent.putExtra(Constant.EXTRA_OFFLINE_LM_RES_FILE_PATH, "/sdcard/easr/s_2_InputMethod");
                }
            }
            intent.putExtra(Constant.EXTRA_OFFLINE_SLOT_DATA, buildTestSlotData());
        }
    }

    private String buildTestSlotData() {
        JSONObject slotData = new JSONObject();
        JSONArray name = new JSONArray().put("李涌泉").put("郭下纶");
        JSONArray song = new JSONArray().put("七里香").put("发如雪");
        JSONArray artist = new JSONArray().put("周杰伦").put("李世龙");
        JSONArray app = new JSONArray().put("手机百度").put("百度地图");
        JSONArray usercommand = new JSONArray().put("关灯").put("开门");
        try {
            slotData.put(Constant.EXTRA_OFFLINE_SLOT_NAME, name);
            slotData.put(Constant.EXTRA_OFFLINE_SLOT_SONG, song);
            slotData.put(Constant.EXTRA_OFFLINE_SLOT_ARTIST, artist);
            slotData.put(Constant.EXTRA_OFFLINE_SLOT_APP, app);
            slotData.put(Constant.EXTRA_OFFLINE_SLOT_USERCOMMAND, usercommand);
        } catch (JSONException e) {

        }
        return slotData.toString();
    }

    private void start() {
        txtLog.setText("");
        print("点击了“开始”");
        Intent intent = new Intent();
        bindParams(intent);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        {

            String args = sp.getString("args", "");
            if (null != args) {
                print("参数集：" + args);
                intent.putExtra("args", args);
            }
        }
        boolean api = sp.getBoolean("api", false);
        if (api) {
            speechEndTime = -1;
            speechRecognizer.startListening(intent);
        } else {
            intent.setAction("com.baidu.action.RECOGNIZE_SPEECH");
            startActivityForResult(intent, REQUEST_UI);
        }

        txtResult.setText("");
    }

    private void stop() {
        speechRecognizer.stopListening();
        print("点击了“说完了”");
    }

    private void cancel() {
        speechRecognizer.cancel();
        status = STATUS_None;
        print("点击了“取消”");
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        status = STATUS_Ready;
        print("准备就绪，可以开始说话");
    }

    @Override
    public void onBeginningOfSpeech() {
        time = System.currentTimeMillis();
        status = STATUS_Speaking;
//        btn.setText("说完了");
        print("检测到用户的已经开始说话");
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        speechEndTime = System.currentTimeMillis();
        status = STATUS_Recognition;
        print("检测到用户的已经停止说话");
//        btn.setText("识别中");
    }

    @Override
    public void onError(int error) {
        time = 0;
        status = STATUS_None;
        StringBuilder sb = new StringBuilder();
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                sb.append("音频问题");
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                sb.append("没有语音输入");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                sb.append("其它客户端错误");
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                sb.append("权限不足");
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                sb.append("网络问题");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                sb.append("没有匹配的识别结果");
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                sb.append("引擎忙");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                sb.append("服务端错误");
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                sb.append("连接超时");
                break;
        }
        sb.append(":" + error);
        print("识别失败：" + sb.toString());
//        btn.setText("开始");
    }

    @Override
    public void onResults(Bundle results) {
        long end2finish = System.currentTimeMillis() - speechEndTime;
        status = STATUS_None;
        ArrayList<String> nbest = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String sttr= Arrays.toString(nbest.toArray(new String[nbest.size()]));
        print("识别成功：" + sttr);
        UploadTTs(sttr);

        String json_res = results.getString("origin_result");
        try {
            print("origin_result=\n" + new JSONObject(json_res).toString(4));
        } catch (Exception e) {
            print("origin_result=[warning: bad json]\n" + json_res);
        }
//        btn.setText("开始");
        String strEnd2Finish = "";
        if (end2finish < 60 * 1000) {
            strEnd2Finish = "(waited " + end2finish + "ms)";
        }
        txtResult.setText(nbest.get(0) + strEnd2Finish);
        time = 0;
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> nbest = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (nbest.size() > 0) {
            print("~临时识别结果：" + Arrays.toString(nbest.toArray(new String[0])));
            txtResult.setText(nbest.get(0));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        switch (eventType) {
            case EVENT_ERROR:
                String reason = params.get("reason") + "";
                print("EVENT_ERROR, " + reason);
                break;
            case VoiceRecognitionService.EVENT_ENGINE_SWITCH:
                int type = params.getInt("engine_type");
                print("*引擎切换至" + (type == 0 ? "在线" : "离线"));
                break;
        }
    }

    long time;
    private void print(String msg) {
        long t = System.currentTimeMillis() - time;
        if (t > 0 && t < 100000) {
            txtLog.append(t + "ms, " + msg + "\n");
        } else {
            txtLog.append("" + msg + "\n");
        }
        ScrollView sv = (ScrollView) txtLog.getParent();
        sv.smoothScrollTo(0, 1000000);
        Log.d(TAG, "----" + msg);
    }

    //调动服务器接口 传送语音识别结果
    public void UploadTTs(String Content) {

        Log.i(TAG,"Content="+Content);
        OkHttpClient   mOkHttpClient=new OkHttpClient();
//        RequestBody formBody = new FormBody.Builder()
//                .add("Content", Content)
//                .build();
        ContentBean bean = new ContentBean(TURING_APIKEY,Content,"北京市丰台区","grandaunt");
        String json = gson.toJson(bean);
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url("http://www.tuling123.com/openapi/api")
                .post(requestBody)
                .build();
        Log.i(TAG,"request="+request);
        Log.i(TAG,"json="+json);
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String str = response.body().string();
                java.lang.reflect.Type  Type =new TypeToken<responseBean>() {}.getType();
                responseBean relt = gson.fromJson(str,Type);
                String  Msg  = relt.getText();
                Log.i(TAG,"Msg="+Msg);

                downloadTTs(Msg);
                Log.i("wangshu", str);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "请求成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
//        txtLog.setText("...");
//        txtResult.setText("...");
    }

    /***
     * 语音合成
     */
    public void downloadTTs(String Msg) {

        OkHttpClient client = new OkHttpClient();
        String uylStr="http://tsn.baidu.com/text2audio?tex="+Msg+"&lan=zh&cuid=12345678951752&ctp=1&per=3&tok=24.443988db07896132ec13cac375b621cc.2592000.1501728772.282335-9840500";
        Request request = new Request.Builder()
                .get()
//            .url("https://www.baidu.com/img/bd_logo1.png")
                .url(uylStr)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("moer", "onFailure: ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                //拿到字节流
                InputStream is = response.body().byteStream();
                int len = 0;
                File file = new File(Environment.getExternalStorageDirectory(), "n.mp3");
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buf = new byte[128];

                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }

                fos.flush();
                //关闭流
                fos.close();
                is.close();
                /**
                 * 播放指定名称的歌曲
                 * @param audioPath 指定默认播放的音乐
                 */

//            Intent mIntent = new Intent();
//            mIntent.setAction(android.content.Intent.ACTION_VIEW);
//            Uri uri = Uri.parse("file:///sdcard/a.mp3");
//            mIntent.setDataAndType(uri , "audio/mp3");
//            IntentUtil.startIntent(mIntent);
                mediaPlayer = new MediaPlayer();
                String sdCard = Environment.getExternalStorageDirectory().getPath();
                Log.i(TAG, Environment.getExternalStorageDirectory() + "/n.mp3");
                mediaPlayer.setDataSource(Environment.getExternalStorageDirectory() + "/n.mp3");
                mediaPlayer.prepare();//准备播放
                mediaPlayer.start();//播放
//            InputStream is = response.body().byteStream();
//
//            final Bitmap bitmap = BitmapFactory.decodeStream(is);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    imageView.setImageBitmap(bitmap);
//                }
//            });
//
//            is.close();
//            start();

            }
        });
//        Intent intent = new Intent(this, ActivityWakeUp.class);
//        startService(intent);

    }
    /*
         * 判断服务是否启动,context上下文对象 ，className服务的name
         */
    public static boolean isServiceRunning(Context mContext, String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(30);

        if (!(serviceList.size() > 0)) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }
}
