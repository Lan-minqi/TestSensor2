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
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.NetworkOnMainThreadException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Math;

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
    private RadioOnClick radioOnClick = new RadioOnClick(0);
    private ListView areaListView;
    private boolean isPaused = true;
    private TextView now_time;
    private String start_time;
    private String filedetail;
    private int data_num;
    private Context mContext;
    SDFileHelper fHelper;

    private String urlPath="192.168.56.1";
    private Socket socket;
    private boolean stopClicked = false;
    private boolean useNet = true;
    //！！！使用时手机要和服务器处于同一局域网下。。。

    private float x1, y1, z1;
    private float x2, y2, z2;
    private float x3, y3, z3;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;
    private final float EPSILON = 0.06f;
    private final float[] gravity = new float[3];
    private static final float alpha = 0.8f;
    private float zeroShift = 0;


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
                SimpleDateFormat sDateFormat = new SimpleDateFormat("HH-mm-ss");
                start_time = sDateFormat.format(new Date());
                try {
                    fHelper = new SDFileHelper(mContext, start_time + areas[radioOnClick.getIndex()]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                filedetail = new String();
                data_num = 0;
                timestamp = 0;
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(isPaused) return;
                        writeFile(false);
                        // TODO Auto-generated method stub
                    }},0, 50);
                Log.i("debug start", start_time+ Environment.getExternalStorageDirectory());
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
                stopClicked = true;
                isPaused = true;
                timestamp = 0;
                if (useNet){
                try {
                    socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                writeFile(true);
                sensorManager.unregisterListener(sensorEventListener);
            }
        });
        open.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder=new AlertDialog.Builder(Accelerometer.this);
                builder.setTitle("输入IP");
//                    builder.setIcon(android.R.drawable.ic_dialog_info);
                final EditText ed = new EditText(Accelerometer.this);
                ed.setText(urlPath);
                builder.setView(ed);

                builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //设置url，IP地址、端口
                        urlPath = ed.getText().toString();
                        Log.i("debug click", start_time+ Environment.getExternalStorageDirectory());
                    }
                    });

                builder.setNegativeButton("连接", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Looper.prepare();
                                try {
                                    socket=new Socket(urlPath, 12345);
                                    Toast.makeText(Accelerometer.this, "Socket创建成功", Toast.LENGTH_LONG).show();
                                } catch (SocketException e) {
                                    Log.i("ipsss",urlPath);
                                    Toast.makeText(Accelerometer.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    e.getCause();
                                } catch (UnknownHostException e) {
                                    Log.i("ipsss",urlPath);
                                    Toast.makeText(Accelerometer.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    Log.i("ipsss",urlPath);
                                    Toast.makeText(Accelerometer.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                } catch(NetworkOnMainThreadException e){
                                    Log.i("ipsss",urlPath);
                                    Toast.makeText(Accelerometer.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                });
                AlertDialog dialog=builder.create();
                dialog.show();
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
        gyroscopeView.setText("Gyroscope: " + x1 + ", " + y1 + ", " + z1);
        accelerometerView.setText("Accelerometer: " + x2 + ", " + y2 + ", " + z2);
    }
    private void test(){
        SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        Log.i("debug", sDateFormat.format(new Date()));
    }
    private void writeFile(boolean force){
        SimpleDateFormat sDateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
        if (useNet) sendDataToServer();
        if (data_num >= 500 || force) {
            Log.i("debug", filedetail.length()+" at " + sDateFormat.format(new Date()));
            data_num = 0;
            try {
                fHelper.savaFileToSD(start_time + areas[radioOnClick.getIndex()], filedetail);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "数据写入失败", Toast.LENGTH_SHORT).show();
            }
            filedetail = "";
        }
//        filedetail += sDateFormat.format(new Date()) + "\nx1 " + x1 + " y1 " + y1 + " z1 " + z1 + "\nx2 " + x2 + " y2 " + y2 + " z2 " + z2 + "\n";
        filedetail += sDateFormat.format(new Date()) + "\nQx " + deltaRotationVector[0] + " Qy " + deltaRotationVector[1] +
                " Qz " + deltaRotationVector[2] + " Q0 " + deltaRotationVector[3] +
                "\nx2 " + x2 + " y2 " + y2 + " z2 " + z2 + "\n";
        data_num++;
    }

    private void sendDataToServer(){
        try
        {
            if(true){
                //传数据给matlab不使用json
//                JSONObject json1 = new JSONObject();
//                json1.put("time", System.currentTimeMillis());
//                json1.put("QX", deltaRotationVector[0]);
//                json1.put("QY", deltaRotationVector[1]);
//                json1.put("QZ", deltaRotationVector[2]);
//                json1.put("Q", deltaRotationVector[3]);
//                json1.put("accX", x2);
//                json1.put("accY", y2);
//                json1.put("accZ", z2);
                String content = new String(String.valueOf(System.currentTimeMillis()) + ' ' +
                        String.valueOf(x1) + ' ' +
                        String.valueOf(y1) + ' ' +
                        String.valueOf(z1) + ' ' +
                        String.valueOf(x2) + ' ' +
                        String.valueOf(y2) + ' ' +
                        String.valueOf(z2) + ' ' +
                        String.valueOf(x3) + ' ' +
                        String.valueOf(y3) + ' ' +
                        String.valueOf(z3) + '\n');
//                String content = new String(String.valueOf(System.currentTimeMillis()) + ' ' +
//                        String.valueOf(deltaRotationVector[0]) + ' ' +
//                        String.valueOf(deltaRotationVector[1]) + ' ' +
//                        String.valueOf(deltaRotationVector[2]) + ' ' +
//                        String.valueOf(deltaRotationVector[3]) + ' ' +
//                        String.valueOf(x2) + ' ' +
//                        String.valueOf(y2) + ' ' +
//                        String.valueOf(z2) + '\n');
                int l = content.length();
                String sentData = new String(String.valueOf(l) + ' ' + content);
                Log.i("de","sa "+sentData);
                BufferedWriter os= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                os.write(sentData);
                os.flush();
            }
        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        //获取陀螺仪传感器
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(sensorEventListener, gyroscopeSensor, radioOnClick.getIndex());
        //获取加速度传感器
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorEventListener, accelerometerSensor, radioOnClick.getIndex());
        //获取磁力传感器
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(sensorEventListener, magneticSensor, radioOnClick.getIndex());
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
        client.disconnect();
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
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
//                if (timestamp != 0) {
//                    final float dT = (event.timestamp - timestamp) * NS2S;
//                    // Axis of the rotation sample, not normalized yet.
//                    float axisX = event.values[0];
//                    float axisY = event.values[1];
//                    float axisZ = event.values[2];
//
//                    // Calculate the angular speed of the sample
//                    float omegaMagnitude = (float)Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);
//                    zeroShift = omegaMagnitude;
//                    // Normalize the rotation vector if it's big enough to get the axis
//                    if (omegaMagnitude > EPSILON) {
//                        axisX /= omegaMagnitude;
//                        axisY /= omegaMagnitude;
//                        axisZ /= omegaMagnitude;
//                    }else Log.i("debug omega", start_time+omegaMagnitude);
//
//                    // Integrate around this axis with the angular speed by the timestep
//                    // in order to get a delta rotation from this sample over the timestep
//                    // We will convert this axis-angle representation of the delta rotation
//                    // into a quaternion before turning it into the rotation matrix.
//                    float thetaOverTwo = omegaMagnitude * dT / 2.0f;
//                    float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
//                    float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
//                    deltaRotationVector[0] = sinThetaOverTwo * axisX;
//                    deltaRotationVector[1] = sinThetaOverTwo * axisY;
//                    deltaRotationVector[2] = sinThetaOverTwo * axisZ;
//                    deltaRotationVector[3] = cosThetaOverTwo;
//                }
//                timestamp = event.timestamp;
            }
            //得到加速度的值
            else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !isPaused) {
                x2 = event.values[SensorManager.DATA_X];
                y2 = event.values[SensorManager.DATA_Y];
                z2 = event.values[SensorManager.DATA_Z];
                //去除重力
//                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
//                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
//                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
//
//                x2 = event.values[0] - gravity[0];
//                y2 = event.values[1] - gravity[1];
//                z2 = event.values[2] - gravity[2];
            }
            else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && !isPaused) {
                x3 = event.values[SensorManager.DATA_X];
                y3 = event.values[SensorManager.DATA_Y];
                z3 = event.values[SensorManager.DATA_Z];
            }

        }
        //重写变化
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}

