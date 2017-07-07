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

    private final float[] angles = new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private final float[] deltaRotationMatrix = new float[9];
    private float[] currentRotationMatrix = new float[9];
    private float[] initialRotationMatrix = new float[9];
    private float[] accMagOrientation = new float[3];
    private float[] gyroOrientation = new float[3];
    private float[] fusedOrientation = new float[3];
    //基于加速度和磁力计的旋转矩阵
    private float[] rotationMatrix = new float[9];
    private float[] accel = new float[3];
    private float[] magnet = new float[3];
    private float[] gyro = new float[3];
    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private float timestamp = 0;
    private float timestampOld = 0;
    private boolean stateInitialized = false;
    private boolean hasInitialOrientation = false;
    private final float EPSILON = 0;
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
                //Timer fuseTimer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(isPaused) return;
                        writeFile(false);
                        // TODO Auto-generated method stub
                    }},0, 20);
                //fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(), 1000, TIME_CONSTANT);
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
    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0]
                            + oneMinusCoeff * accMagOrientation[0];

            fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1]
                            + oneMinusCoeff * accMagOrientation[1];

            fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2]
                            + oneMinusCoeff * accMagOrientation[2];

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            currentRotationMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
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
        gyroscopeView.setText("Gyroscope: " + gyroOrientation[0] + ", " + gyroOrientation[1] + ", " + gyroOrientation[2]);
        accelerometerView.setText("Accelerometer: " + accel[0] + ", " + accel[1] + ", " + accel[2]);
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
        filedetail += sDateFormat.format(new Date()) + "\nx1 " + gyroOrientation[0] + " y1 " + gyroOrientation[1] + " z1 " + gyroOrientation[2]
                + "\nx2 " + accel[0] + " y2 " + accel[1] + " z2 " + accel[2] + "\nx3 " + magnet[0] + " y3 " + magnet[1] + " z3 " + magnet[2] + "\n";

        data_num++;
    }

    private void sendDataToServer(){
        try
        {
            if(data_num % 2 == 0){
                String content = new String(String.valueOf(System.currentTimeMillis()) + ' ' +
                        String.valueOf(gyroOrientation[0]) + ' ' +
                        String.valueOf(gyroOrientation[1]) + ' ' +
                        String.valueOf(gyroOrientation[2]) + '\n');
                int l = content.length();
                String sentData = new String(String.valueOf(l) + ' ' + content);
                //Log.i("de","sa "+sentData);
                BufferedWriter os= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                os.write(sentData);
                os.flush();
//                String content = new String(String.valueOf(System.currentTimeMillis()) + ' ' +
//                        String.valueOf(gyroOrientation[0]) + ' ' +
//                        String.valueOf(gyroOrientation[1]) + ' ' +
//                        String.valueOf(gyroOrientation[2]) + ' ' +
//                        String.valueOf(accel[0]) + ' ' +
//                        String.valueOf(accel[1]) + ' ' +
//                        String.valueOf(accel[2]) + ' ' +
//                        String.valueOf(magnet[0]) + ' ' +
//                        String.valueOf(magnet[1]) + ' ' +
//                        String.valueOf(magnet[2]) + '\n');
//                BufferedWriter os= new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//                //os.write(int)意思是发送一个char字符
//                os.write(content.length());
//                os.write(content);
//                os.flush();
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

            //得到加速度的值
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !isPaused) {
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
            }
            else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && !isPaused) {
                System.arraycopy(event.values, 0, magnet, 0, 3);
            }
            else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE && !isPaused) {
                gyroFunction(event);
            }

        }
        //重写变化
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
    public void gyroFunction(SensorEvent event) {
        if (!hasInitialOrientation)
        {
            hasInitialOrientation = SensorManager.getRotationMatrix(
                    initialRotationMatrix, null, accel, magnet);
//            float [] a = matrixMultiSpec(initialRotationMatrix, initialRotationMatrix);
//            for (int k = 0; k < 9; k++){
//                Log.v("matrix", "a"+k+"="+a[k]);
//            }
            if(!hasInitialOrientation) return;
        }

        // Initialization of the gyroscope based rotation matrix
        if (!stateInitialized)
        {
            currentRotationMatrix = initialRotationMatrix.clone();
            stateInitialized = true;
        }

        // This timestep's delta rotation to be multiplied by the current
        // rotation after computing it from the gyro sample data.

        if (stateInitialized)
        {
            // Axis of the rotation sample, not normalized yet.
            timestamp = event.timestamp;
            final float dT = (timestamp - timestampOld) * NS2S;
            timestampOld = timestamp;
            if(dT == 0) return;
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            // Calculate the angular speed of the sample
            float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY
                    * axisY + axisZ * axisZ);
            // Normalize the rotation vector if it's big enough to get the axis
            if (omegaMagnitude > EPSILON)
            {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the
            // timestep. We will convert this axis-angle representation of the
            // delta rotation into a quaternion before turning it into the
            // rotation matrix.
            float thetaOverTwo = omegaMagnitude * dT / 2.0f;

            float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

            deltaRotationVector[0] = sinThetaOverTwo * axisX;
            deltaRotationVector[1] = sinThetaOverTwo * axisY;
            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;

            SensorManager.getRotationMatrixFromVector(
                    deltaRotationMatrix,
                    deltaRotationVector);

            currentRotationMatrix = matrixMultiplication(
                    currentRotationMatrix,
                    deltaRotationMatrix);
//            Log.v("deltaRotationMatrix", String.valueOf(deltaRotationMatrix[1]));
            SensorManager.getOrientation(currentRotationMatrix,
                    gyroOrientation);

            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
            fusedOrientation[0] =
                    FILTER_COEFFICIENT * gyroOrientation[0]
                            + oneMinusCoeff * accMagOrientation[0];

            fusedOrientation[1] =
                    FILTER_COEFFICIENT * gyroOrientation[1]
                            + oneMinusCoeff * accMagOrientation[1];

            fusedOrientation[2] =
                    FILTER_COEFFICIENT * gyroOrientation[2]
                            + oneMinusCoeff * accMagOrientation[2];

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            currentRotationMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
//            SensorManager.getOrientation(matrixMultiSpec(currentRotationMatrix, initialRotationMatrix) ,
//                    gyroOrientation);
            gyroOrientation[0]*=180/(float)Math.PI;
            gyroOrientation[1]*=180/(float)Math.PI;
            gyroOrientation[2]*=180/(float)Math.PI;
        }

    }
    public void calculateAccMagOrientation() {
        if(SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }
    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
    private float[] matrixMultiplication(float[] a, float[] b)
    {
        float[] result = new float[9];

        result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
        result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
        result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

        result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
        result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
        result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

        result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
        result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
        result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

        return result;
    }
    private float[] matrixMultiSpec(float[] a, float[] b)
    {
        /*返回a乘以b的转置
        * <pre>
        *   /  a[ 0]   a[ 1]   a[ 2]   \
        *   |  a[ 3]   a[ 4]   a[ 5]   |
        *   \  a[ 6]   a[ 7]   a[ 8]   /
        *   /  b[ 0]   b[ 3]   b[ 6]   \
        *   |  b[ 1]   b[ 4]   b[ 7]   |
        *   \  b[ 2]   b[ 5]   b[ 8]   /
        *</pre>
        */
        float[] result = new float[9];

        result[0] = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        result[1] = a[0] * b[3] + a[1] * b[4] + a[2] * b[5];
        result[2] = a[0] * b[6] + a[1] * b[7] + a[2] * b[8];

        result[3] = a[3] * b[0] + a[4] * b[1] + a[5] * b[2];
        result[4] = a[3] * b[3] + a[4] * b[4] + a[5] * b[5];
        result[5] = a[3] * b[6] + a[4] * b[7] + a[5] * b[8];

        result[6] = a[6] * b[0] + a[7] * b[1] + a[8] * b[2];
        result[7] = a[6] * b[3] + a[7] * b[4] + a[8] * b[5];
        result[8] = a[6] * b[6] + a[7] * b[7] + a[8] * b[8];

        return result;
    }
}

