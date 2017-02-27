package com.example.lmq.testsensor2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Button;
/**
 * Created by lmq on 2017/2/26.
 */

public class StopWatch extends AppCompatActivity {
    private TextView minText;       //分
    private TextView secText;       //秒
    private Button back;
    private Button start;           //开始按钮
    private Button stop;            //停止按钮
    private boolean isPaused = false;
    private String timeUsed;
    private int timeUsedInsec;

    private Handler uiHandle = new Handler(){
        public void handleMessage(android.os.Message msg) {
            switch(msg.what){
                case 1:
                    if(!isPaused) {
                        updateClockUI();
                        addTimeUsed();
                    }
                    uiHandle.sendEmptyMessageDelayed(1, 1000);
                    break;
                default: break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_watch);

        //获取界面的控件
        minText = (TextView) findViewById(R.id.min);
        secText = (TextView) findViewById(R.id.sec);
        back = (Button) findViewById(R.id.stop_watch_back);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(StopWatch.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
        //为按钮Start注册监听器
        start.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                uiHandle.removeMessages(1);
                isPaused = false;
                startTime();
            }
        });

        //为按钮stop注册监听器
        stop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                isPaused = true;
                timeUsedInsec = 0;
            }
        });
    }

    private void updateClockUI(){
        minText.setText(getMin() + ":");
        secText.setText(getSec());
    }

    private void startTime(){
        uiHandle.sendEmptyMessageDelayed(1,0);
    }

    public void addTimeUsed(){
        timeUsedInsec = timeUsedInsec + 1;
        timeUsed = this.getMin() + ":" + this.getSec();
    }

    public CharSequence getMin(){
        return String.valueOf(timeUsedInsec / 60);
    }

    public CharSequence getSec(){
        int sec = timeUsedInsec % 60;
        return sec < 10? "0" + sec :String.valueOf(sec);
    }
}
