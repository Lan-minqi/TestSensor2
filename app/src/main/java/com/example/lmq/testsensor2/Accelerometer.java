package com.example.lmq.testsensor2;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lmq on 2017/2/26.
 * 重力加速度的误差？
 * 传感器的是以左下角为原点的，x向右，y向上
 * 文件中加速度传感器在后
 */

public class Accelerometer extends AppCompatActivity {
    private TextView accelerometerView;
    private TextView gyroscopeView;
    private SensorManager sensorManager;
    private MySensorEventListener sensorEventListener;

    private Button back;
    private Button start;
    private Button stop;
    private Button open;
    private Button settings;
    private String[] areas = new String[]{"FASTEST", "GAME", "UI", "NORMAL" };
    //对应SensorManager的0,1,2,3
    private RadioOnClick radioOnClick = new RadioOnClick(2);
    private ListView areaListView;
    private boolean isPaused = true;
    private TextView now_time;
    private String start_time;
    private String filedetail;
    private int data_num;
    private Context mContext;
    FileHelper fHelper;

    private float x1, y1, z1;
    private float x2, y2, z2;


    private Handler uiHandle = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!isPaused) {
                        updateClockUI();
                    }
                    uiHandle.sendEmptyMessageDelayed(1, 1000);
                    break;
                default:
                    break;
            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accelerometer);
        sensorEventListener = new MySensorEventListener();
        accelerometerView = (TextView) this.findViewById(R.id.accelerometerView);
        gyroscopeView = (TextView) this.findViewById(R.id.gyroscopeView);
        //获取感应器管理器
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        back = (Button) findViewById(R.id.accelerometer_back);
        start = (Button) findViewById(R.id.start_test);
        stop = (Button) findViewById(R.id.stop_test);
        open = (Button) findViewById(R.id.open);
        settings = (Button) findViewById(R.id.settings);

        now_time = (TextView) findViewById(R.id.now_time);
        mContext = getApplicationContext();

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(Accelerometer.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        //为按钮Start注册监听器
        start.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                fHelper = new FileHelper(mContext);
                SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss");
                start_time = sDateFormat.format(new Date());
                filedetail = new String();
                data_num = 0;
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(isPaused) return;
                        writeFile();
                        // TODO Auto-generated method stub
                    }},0, 1);
                Log.i("debug", mContext.getFilesDir() + "/" + start_time);
                onResume();
                uiHandle.removeMessages(1);
                isPaused = false;
                uiHandle.sendEmptyMessageDelayed(1, 0);
            }
        });

        //为按钮stop注册监听器
        stop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                isPaused = true;
                writeFile();
                sensorManager.unregisterListener(sensorEventListener);
            }
        });
        open.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isPaused) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    try {
                        startActivityForResult(Intent.createChooser(intent, "查看保存的文件"), 1);
                    } catch (ActivityNotFoundException ex) {
                        // Potentially direct the user to the Market with a Dialog
                        Toast.makeText(Accelerometer.this, "请安装文件管理器", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }
        });
        settings.setOnClickListener(new RadioClickListener());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    class RadioClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v){
            if(isPaused){
                AlertDialog ad =new AlertDialog.Builder(Accelerometer.this).setTitle("选择区域")
                        .setSingleChoiceItems(areas,radioOnClick.getIndex(),radioOnClick).create();
                areaListView=ad.getListView();
                ad.show();
            }
        }
    }

    class RadioOnClick implements DialogInterface.OnClickListener{
        private int index;

        public RadioOnClick(int index){
            this.index = index;
        }
        public void setIndex(int index){
            this.index=index;
        }
        public int getIndex(){
            return index;
        }

        public void onClick(DialogInterface dialog, int whichButton){
            setIndex(whichButton);
            Toast.makeText(Accelerometer.this, "您已经选择了 " +  ":" + areas[index], Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
    }

    private void updateClockUI() {
        SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss");
        now_time.setText(sDateFormat.format(new Date()));
    }

    private void writeFile(){
        SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        if (data_num >= 1000) {
            Log.i("debug", filedetail.length()+" at " + sDateFormat.format(new Date()));
            data_num = 0;
            try {
                fHelper.save(mContext.getFilesDir() + "/" + start_time + areas[radioOnClick.getIndex()], filedetail);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "数据写入失败", Toast.LENGTH_SHORT).show();
            }
            filedetail = "";
        }
        filedetail += sDateFormat.format(new Date()) + "\nx1 " + x1 + " y1 " + y1 + " z1 " + z1 + "\nx2 " + x2 + " y2 " + y2 + " z2 " + z2 + "\n";
        data_num++;
    }

    @Override
    protected void onResume() {
        //获取陀螺仪传感器
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(sensorEventListener, gyroscopeSensor, radioOnClick.getIndex());

        //获取加速度传感器
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, radioOnClick.getIndex());
        super.onResume();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Accelerometer Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    private final class MySensorEventListener implements SensorEventListener {
        //可以得到传感器实时测量出来的变化值
        @Override
        public void onSensorChanged(SensorEvent event) {
            //得到角速度的值
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && !isPaused) {
                x1 = event.values[SensorManager.DATA_X];
                y1 = event.values[SensorManager.DATA_Y];
                z1 = event.values[SensorManager.DATA_Z];
                gyroscopeView.setText("Gyroscope: " + x1 + ", " + y1 + ", " + z1);
            }
            //得到加速度的值
            else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !isPaused) {
                x2 = event.values[SensorManager.DATA_X];
                y2 = event.values[SensorManager.DATA_Y];
                z2 = event.values[SensorManager.DATA_Z];
                accelerometerView.setText("Accelerometer: " + x2 + ", " + y2 + ", " + z2);
            }

        }

        //重写变化
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}

