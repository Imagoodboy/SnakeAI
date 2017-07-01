package com.example.mt.snakeai;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final int REFRESH = 1; //定义消息 防止硬编码
    private final int REFRESH_INTERVAL = 10; //刷新时间间隔

    public static int mWidth;
    public static int mHeight;
    public static boolean isPaused = false; //线程停止标志
    public static Thread mRefreshThread; //用于发送刷新消息的线程

    private TextView mScore;
    private SnakeView mSnakeView;

    private Handler mHandler = new Handler() { //thread handler 消息处理
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            if(msg.arg1 == REFRESH) {
                if(mSnakeView != null) {
                    mScore.setText("当前分数:" + String.valueOf(SnakeView.mScore) + "分");
                    mSnakeView.invalidate();
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSnakeView = (SnakeView)findViewById(R.id.snake_view);
        mScore = (TextView)findViewById(R.id.tv_score);
        isPaused = false;
        mRefreshThread = new Thread(){

            @Override
            public void run() {
                // TODO Auto-generated method stub
                super.run();
                while(!isPaused) {
                    Message msg = mHandler.obtainMessage();
                    msg.arg1 = REFRESH;
                    mHandler.sendMessage(msg);
                    try {
                        Thread.sleep(REFRESH_INTERVAL); //休眠一段时间后再发送消息刷新界面
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        };
        mRefreshThread.start(); //启动线程
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) { //获取View的宽高
        super.onWindowFocusChanged(hasFocus);
        mWidth = mSnakeView.getWidth();
        mHeight = mSnakeView.getHeight();
        Log.e("VIEW--","size"+ mWidth +" " + mHeight);
    }
}
