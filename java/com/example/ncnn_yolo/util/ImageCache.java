package com.example.ncnn_yolo.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageCache {
    private static ImageCache instance;
    private LruCache<String, Bitmap> memoryCache;
    private String diskCachePath;

    // 获取单例实例
    public static ImageCache getInstance() {
        if (instance == null) {
            instance = new ImageCache();
        }
        return instance;
    }

    // 初始化缓存
    public void init(Context context, String diskCachePath) {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 16;  // 调整内存缓存大小为可用内存的 1/16

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        this.diskCachePath = diskCachePath;
        File diskCacheDir = new File(diskCachePath);
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
    }

    // 从内存缓存中获取图像
    public Bitmap getBitmapFromMemory(String key) {
        return memoryCache.get(key);
    }

    // 将图像存入内存缓存
    public void putBitmapToMemory(String key, Bitmap bitmap) {
        if (getBitmapFromMemory(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    // 从磁盘缓存中获取图像
    public Bitmap getBitmapFromDisk(String key, BitmapFactory.Options options) {
        String path = diskCachePath + "/" + key.hashCode();
        return BitmapFactory.decodeFile(path, options);
    }

    // 将图像存入磁盘缓存
    public void putBitmapToDisk(String key, Bitmap bitmap) {
        String path = diskCachePath + "/" + key.hashCode();
        try (FileOutputStream out = new FileOutputStream(path)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
