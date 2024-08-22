package com.example.ncnn_yolo.ui.detect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
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

public class ImageFragment extends Fragment {

    private com.example.ncnn_yolo.ncnn_yolo yolov8ncnn = new com.example.ncnn_yolo.ncnn_yolo();
    private int current_model = 0;
    private int current_cpugpu = 0;

    private ImageView viewRaw;
    private ImageView viewNew;

    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;

    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.img, container, false);

        // 保持屏幕常亮
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 获取 ImageView
        viewRaw = rootView.findViewById(R.id.viewRaw);
        viewNew = rootView.findViewById(R.id.viewNew);

        // 获取按钮并设置点击事件
        Button buttonUpImg = rootView.findViewById(R.id.buttonUpImg);
        Button buttonImgDetection = rootView.findViewById(R.id.buttonImgDetection);
        Button buttonFunction = rootView.findViewById(R.id.buttonFunction);

        // 设置上传图片按钮的点击事件
        buttonUpImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        //------------保存图像-----------------
        Button buttonSaveImg = rootView.findViewById(R.id.buttonSaveImg);
        buttonSaveImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 申请写入权限
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                } else {
                    // 已有权限，直接保存图像
                    saveImage();
                }
            }
        });

        // 设置图像检测按钮的点击事件
        buttonImgDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewRaw.getDrawable() != null) {
                    Bitmap bitmap = ((BitmapDrawable) viewRaw.getDrawable()).getBitmap();
                    Bitmap processedBitmap = processImage(bitmap);
                    if (processedBitmap != null) {
                        viewNew.setImageBitmap(processedBitmap);
                        viewNew.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }
                }
            }
        });

        // 设置切换界面按钮的点击事件
        buttonFunction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_video);
            }
        });

        // 获取 Spinner 并设置监听器
        spinnerModel = rootView.findViewById(R.id.spinnerModel);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != current_model) {
                    current_model = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        spinnerCPUGPU = rootView.findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != current_cpugpu) {
                    current_cpugpu = position;
                    reload();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        reload();
        return rootView;
    }

    // 其他方法（如 processImage, saveImage 等）保持不变，直接迁移到 ImageFragment 中

    private void reload() {
        boolean ret_init = yolov8ncnn.loadModel(getActivity().getAssets(), current_model, current_cpugpu);
        if (!ret_init) {
            Log.e("ImageFragment", "yolov8ncnn loadModel failed");
        }
    }

    private Bitmap processImage(Bitmap bitmap) {
        ArrayList<HashMap<String, Object>> results = yolov8ncnn.detectFromBitmap(bitmap);

        if (results == null || results.isEmpty()) {
            return bitmap;
        }

        Bitmap resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.RED);

        Paint textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(85);

        for (HashMap<String, Object> result : results) {
            float x = Float.parseFloat((String) result.get("x"));
            float y = Float.parseFloat((String) result.get("y"));
            float w = Float.parseFloat((String) result.get("w"));
            float h = Float.parseFloat((String) result.get("h"));
            String label = (String) result.get("label");
            String prob = (String) result.get("prob");

            Rect rect = new Rect((int) x, (int) y, (int) (x + w), (int) (y + h));
            canvas.drawRect(rect, paint);

            int labelIndex = Integer.parseInt(label);
            String text = getLabel(labelIndex) + ": " + prob;
            canvas.drawText(text, x, y > 60 ? y - 10 : y + 60, textPaint);
        }

        return resultBitmap;
    }

    public String getLabel(int index) {
        final String[] CLASS_NAMES = {"Longitudinal Crack","Transverse Crack","Aligator Crack","Pothole","D50","D60"};
        if (index >= 0 && index < CLASS_NAMES.length) {
            return CLASS_NAMES[index];
        }
        return "unknown";
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
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == getActivity().RESULT_OK) {
            if (data != null && data.getData() != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), data.getData());
                    viewRaw.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
