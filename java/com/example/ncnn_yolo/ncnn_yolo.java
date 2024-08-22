package com.example.ncnn_yolo;

import android.content.res.AssetManager;
import android.view.Surface;

import java.util.ArrayList;
import java.util.HashMap;
import android.graphics.Bitmap;
import android.util.Log;

public class ncnn_yolo
{
    public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
    public native boolean openCamera(int facing);
    public native boolean closeCamera();
    public native boolean setOutputWindow(Surface surface);
    public native ArrayList<HashMap<String, Object>> detectFromBitmap(Bitmap bitmap);


    static {
        System.loadLibrary("yolov8ncnn");
    }

}
