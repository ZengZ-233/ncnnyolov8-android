package com.example.ncnn_yolo;

import android.content.res.AssetManager;
import android.view.Surface;

import java.util.ArrayList;
import java.util.HashMap;
import android.graphics.Bitmap;
import android.util.Log;

public class imgfix_interface
{
    // JNI接口定义
    public native Bitmap scaleBitmapByNearestNeighbor(Bitmap inputBitmap, float scale);//最近邻
    public native Bitmap scaleBitmapByBilinear(Bitmap inputBitmap, float scale);//双线性
    public native Bitmap scaleBitmapByBicubic(Bitmap inputBitmap, float scale);//双三次插值
    static {
        System.loadLibrary("yolov8ncnn");
    }

}
