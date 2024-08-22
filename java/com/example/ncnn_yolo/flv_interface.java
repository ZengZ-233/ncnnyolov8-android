package com.example.ncnn_yolo;

import android.content.res.AssetManager;
import android.view.Surface;

import java.util.ArrayList;
import java.util.HashMap;
import android.graphics.Bitmap;


public class flv_interface
{
    // JNI接口定义



    static {
        System.loadLibrary("yolov8ncnn");
    }

}
