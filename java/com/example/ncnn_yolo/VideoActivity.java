package com.example.ncnn_yolo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;

import android.content.Intent;

import android.os.Handler;
import android.widget.Switch;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VideoActivity extends Activity implements SurfaceHolder.Callback
{
    public static final int REQUEST_CAMERA = 100;

    private com.example.ncnn_yolo.ncnn_yolo yolov8ncnn = new com.example.ncnn_yolo.ncnn_yolo();
    private int facing = 1;

    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_model = 0;
    private int current_cpugpu = 0;

    private SurfaceView cameraView;

    private Switch switchSaveTarget;
    private Handler handler = new Handler();
    private Runnable checkTargetRunnable;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 获取 SurfaceView 并设置格式
        cameraView = (SurfaceView) findViewById(R.id.view);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        // 获取切换摄像头的按钮并设置点击事件
        Button buttonSwitchCamera = (Button) findViewById(R.id.buttonSwitchCamera);
        buttonSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int new_facing = 1 - facing;

                // 关闭当前摄像头
                yolov8ncnn.closeCamera();

                // 打开新摄像头
                yolov8ncnn.openCamera(new_facing);

                // 更新摄像头方向
                facing = new_facing;
            }
        });


        // ---------------切换界面-----------------------------
        Button buttonFunction = findViewById(R.id.buttonFunction);
        buttonFunction.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View arg0) {
              Intent intent = new Intent(VideoActivity.this, ImageActivity.class);
              startActivity(intent);
          }
        });




        // 获取 Spinner 组件
        spinnerModel = (Spinner) findViewById(R.id.spinnerModel);

        // 设置选项选择监听器
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                // 如果选中的位置与当前模型位置不同
                if (position != current_model) {
                    // 更新当前模型位置
                    current_model = position;
                    // 重新加载内容
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // 当没有选中任何选项时，不执行任何操作
            }
        });


        // 获取 CPU/GPU 选择 Spinner 组件
        spinnerCPUGPU = (Spinner) findViewById(R.id.spinnerCPUGPU);

        // 设置选项选择监听器
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                // 如果选中的位置与当前 CPU/GPU 位置不同
                if (position != current_cpugpu) {
                    // 更新当前 CPU/GPU 位置
                    current_cpugpu = position;
                    // 重新加载内容
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // 当没有选中任何选项时，不执行任何操作
            }
        });


        reload();
    }


    private void reload() {
        // 加载模型，根据当前选中的模型和 CPU/GPU 配置进行初始化
        boolean ret_init = yolov8ncnn.loadModel(getAssets(), current_model, current_cpugpu);

        // 如果模型加载失败，输出错误日志
        if (!ret_init) {
            Log.e("VideoActivity", "yolov8ncnn loadModel failed");
        }
    }




    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        yolov8ncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
    }

    @Override
    public void onResume() {
        super.onResume();

        // 检查是否已授予相机权限
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            // 如果没有相机权限，请求相机权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        // 打开摄像头，使用当前摄像头方向（facing）
        yolov8ncnn.openCamera(facing);
    }


    @Override
    public void onPause() {
        super.onPause();

        // 在活动暂停时关闭摄像头
        yolov8ncnn.closeCamera();
    }

}
