package com.example.ncnn_yolo.ui.fix;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ncnn_yolo.R;
import com.example.ncnn_yolo.util.ImageCache;
import com.example.ncnn_yolo.imgfix_interface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImgFix extends Fragment {

    private ImageView viewRaw;
    private ImageView viewNew;
    private static final int REQUEST_WRITE_STORAGE = 112;
    private ProgressBar progressBar;
    private int selectedMethod = 0;  // 记录用户选择的插值方法

    // 实例化 imgfix_interface
    private imgfix_interface imgfix = new imgfix_interface();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.img_fix, container, false);

        // 保持屏幕常亮
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 初始化 ImageCache
        ImageCache.getInstance().init(getContext(), getContext().getCacheDir().getPath());

        // 获取 ImageView
        viewRaw = rootView.findViewById(R.id.Raw);
        viewNew = rootView.findViewById(R.id.New);

        // 获取 ProgressBar
        progressBar = rootView.findViewById(R.id.progressBar);

        // 获取按钮并设置点击事件
        Button buttonUpImg = rootView.findViewById(R.id.UpImg);
        Button buttonImgDetection = rootView.findViewById(R.id.ImgDetection);

        // 获取 Spinner 并设置选择监听器
        Spinner spinnerMethod = rootView.findViewById(R.id.spinnerMethod);
        spinnerMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedMethod = position;  // 更新用户选择的插值方法
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 这里可以处理没有选择任何选项的情况
            }
        });

        // 设置上传图片按钮的点击事件
        buttonUpImg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        // 设置“修复图片”按钮的点击事件
        buttonImgDetection.setOnClickListener(v -> {
            if (viewRaw.getDrawable() != null) {
                Bitmap bitmap = ((BitmapDrawable) viewRaw.getDrawable()).getBitmap();
                showProgress(true);  // 显示进度条
                new Thread(() -> {
                    Bitmap processedBitmap = processImage(bitmap, selectedMethod);
                    getActivity().runOnUiThread(() -> {
                        if (processedBitmap != null) {
                            viewNew.setImageBitmap(processedBitmap);
                            viewNew.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        }
                        showProgress(false);  // 隐藏进度条
                    });
                }).start();
            } else {
                Toast.makeText(getContext(), "请先上传图片", Toast.LENGTH_SHORT).show();
            }
        });

        // 保存图像按钮
        Button buttonSaveImg = rootView.findViewById(R.id.SaveImg);
        buttonSaveImg.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // 申请写入权限
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            } else {
                // 已有权限，直接保存图像
                saveImage();
            }
        });

        return rootView;
    }

    private void showProgress(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == getActivity().RESULT_OK) {
            if (data != null && data.getData() != null) {
                try {
                    String key = data.getData().toString();
                    ImageCache imageCache = ImageCache.getInstance();
                    Bitmap bitmap = imageCache.getBitmapFromMemory(key);

                    if (bitmap == null) {
                        bitmap = imageCache.getBitmapFromDisk(key, null);
                        if (bitmap == null) {
                            bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), data.getData());
                            imageCache.putBitmapToMemory(key, bitmap);
                            imageCache.putBitmapToDisk(key, bitmap);
                        }
                    }

                    viewRaw.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Bitmap processImage(Bitmap bitmap, int method) {
        if (bitmap == null) return null;

        ImageCache imageCache = ImageCache.getInstance();
        String key = "processed_" + bitmap.hashCode() + "_" + method;
        Bitmap cachedBitmap = imageCache.getBitmapFromMemory(key);

        if (cachedBitmap != null) {
            return cachedBitmap;
        }

        try {
            Bitmap newBitmap;
            if (method == 0) {
                // 最近邻插值算法
                newBitmap = imgfix.scaleBitmapByNearestNeighbor(bitmap, 2.0f);
            } else if (method==1) {
                // 双线性插值
                newBitmap = imgfix.scaleBitmapByBilinear(bitmap, 2.0f);
            }
            {
                // 双三次插值
                newBitmap = imgfix.scaleBitmapByBicubic(bitmap, 2.0f);
            }
            // 缓存处理后的图像
            imageCache.putBitmapToMemory(key, newBitmap);
            return newBitmap;
        } catch (OutOfMemoryError e) {
            Toast.makeText(getContext(), "内存不足，无法处理图像", Toast.LENGTH_LONG).show();
            return bitmap;
        } catch (Exception e) {
            Toast.makeText(getContext(), "图像处理失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return bitmap;
        }
    }

    private void saveImage() {
        if (viewNew.getDrawable() == null) {
            Toast.makeText(getContext(), "没有图像可保存", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = ((BitmapDrawable) viewNew.getDrawable()).getBitmap();
        FileOutputStream fos = null;
        try {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String basePath = picturesDir.getAbsolutePath() + "/saved_image";
            String extension = ".png";
            String finalPath = getUniqueFilePath(basePath, extension);
            File imageFile = new File(finalPath);

            fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            Toast.makeText(getContext(), "图像保存成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "图像保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static String getUniqueFilePath(String basePath, String extension) {
        File file;
        int index = 1;
        String filePath;

        do {
            filePath = basePath + (index > 1 ? "_" + index : "") + extension;
            file = new File(filePath);
            index++;
        } while (file.exists());

        return filePath;
    }
}
