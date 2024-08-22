package com.example.ncnn_yolo.ui.detect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.ncnn_yolo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class VideoFragment extends Fragment implements SurfaceHolder.Callback {
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.video, container, false);

        // 保持屏幕常亮
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 获取 SurfaceView 并设置格式
        cameraView = rootView.findViewById(R.id.view);
        cameraView.getHolder().setFormat(PixelFormat.RGBA_8888);
        cameraView.getHolder().addCallback(this);

        // 获取切换摄像头的按钮并设置点击事件
        Button buttonSwitchCamera = rootView.findViewById(R.id.buttonSwitchCamera);
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
        Button buttonFunction = rootView.findViewById(R.id.buttonFunction);
        buttonFunction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_img); // 导航到 ImageFragment
            }
        });

        // 获取 Spinner 组件
        spinnerModel = rootView.findViewById(R.id.spinnerModel);

        // 设置选项选择监听器
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                if (position != current_model) {
                    current_model = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        // 获取 CPU/GPU 选择 Spinner 组件
        spinnerCPUGPU = rootView.findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
                if (position != current_cpugpu) {
                    current_cpugpu = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        reload();

        return rootView;
    }

    private void reload() {
        boolean ret_init = yolov8ncnn.loadModel(getActivity().getAssets(), current_model, current_cpugpu);
        if (!ret_init) {
            Log.e("VideoFragment", "yolov8ncnn loadModel failed");
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        yolov8ncnn.setOutputWindow(holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
        yolov8ncnn.openCamera(facing);
    }

    @Override
    public void onPause() {
        super.onPause();
        yolov8ncnn.closeCamera();
    }
}
