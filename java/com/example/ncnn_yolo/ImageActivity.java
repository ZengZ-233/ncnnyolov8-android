package com.example.ncnn_yolo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

//--------正则表达式匹配------------------


public class ImageActivity extends Activity implements SurfaceHolder.Callback {
    private com.example.ncnn_yolo.ncnn_yolo yolov8ncnn = new com.example.ncnn_yolo.ncnn_yolo();
    private int current_model = 0;
    private int current_cpugpu = 0;

    private ImageView viewRaw;
    private ImageView viewNew;

    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;

    private static final int REQUEST_WRITE_STORAGE = 112;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.img);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 获取 ImageView
        viewRaw = findViewById(R.id.viewRaw);
        viewNew = findViewById(R.id.viewNew);

        // 获取按钮并设置点击事件
        Button buttonUpImg = findViewById(R.id.buttonUpImg);
        Button buttonImgDetection = findViewById(R.id.buttonImgDetection);
        Button buttonFunction = findViewById(R.id.buttonFunction);

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
        Button buttonSaveImg = findViewById(R.id.buttonSaveImg);
        buttonSaveImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(ImageActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // 申请写入权限
                    ActivityCompat.requestPermissions(ImageActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
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
                } else {
                    // 显示提示，没有图片可以检测
                    Toast.makeText(ImageActivity.this, "没有图片可以检测", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // 设置切换界面按钮的点击事件
        buttonFunction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ImageActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // 获取 Spinner 并设置监听器
        spinnerModel = findViewById(R.id.spinnerModel);
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

        spinnerCPUGPU = findViewById(R.id.spinnerCPUGPU);
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
    }

    private void reload() {
        boolean ret_init = yolov8ncnn.loadModel(getAssets(), current_model, current_cpugpu);
        if (!ret_init) {
            Log.e("ImageActivity", "yolov8ncnn loadModel failed");
        }
    }

    private Bitmap processImage(Bitmap bitmap) {
        // 调用本地方法处理图像
        ArrayList<HashMap<String, Object>> results = yolov8ncnn.detectFromBitmap(bitmap);

        if (results == null || results.isEmpty()) {
            return bitmap;
        }

        // 创建新的 Bitmap 以便绘制检测框
        Bitmap resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.RED);

        Paint textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(110); // 增加文本大小

        for (HashMap<String, Object> result : results) {
            float x = Float.parseFloat((String) result.get("x"));
            float y = Float.parseFloat((String) result.get("y"));
            float w = Float.parseFloat((String) result.get("w"));
            float h = Float.parseFloat((String) result.get("h"));
            String label = (String) result.get("label");
            String prob = (String) result.get("prob");

            // 绘制矩形框
            Rect rect = new Rect((int) x, (int) y, (int) (x + w), (int) (y + h));
            canvas.drawRect(rect, paint);

            // 绘制类别和置信度
            int labelIndex = Integer.parseInt(label);
            String text = getLabel(labelIndex) + ": " + prob;
            canvas.drawText(text, x, y > 60 ? y - 10 : y + 60, textPaint); // 确保文字在图像范围内
        }

        return resultBitmap;
    }
    //----------------将标签下标转为真实的标签---------------------------
    public String getLabel(int index) {
        final String[] CLASS_NAMES = {"Longitudinal Crack","Transverse Crack","Aligator Crack","Pothole","D50","D60"};
        if (index >= 0 && index < CLASS_NAMES.length) {
            return CLASS_NAMES[index];
        }
        return "unknown";
    }
    //-------------上传图片----------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            try {
                // 从 URI 中获取图片并显示在 viewRaw 中
                Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                viewRaw.setImageBitmap(bitmap);
                viewRaw.setScaleType(ImageView.ScaleType.FIT_CENTER); // 自适应视图
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //----------------保存图像-------------------------
    private void saveImage() {
        if (viewNew.getDrawable() == null) {
            Toast.makeText(this, "没有图像可保存", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = ((BitmapDrawable) viewNew.getDrawable()).getBitmap();
        FileOutputStream fos = null;
        try {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//            String path = picturesDir.getAbsolutePath(); // 获取路径字符串
//
//            // 正则表达式，提取第一个 / 左边的内容
//            Pattern pattern = Pattern.compile("^[^/]+");
//            Matcher matcher = pattern.matcher(path);
//            String leftOfFirstSlash="手机存储";
//            if (matcher.find()) {
//                leftOfFirstSlash = matcher.group();
//                System.out.println("第一个 / 左边的内容: " + leftOfFirstSlash);
//            } else {
//                System.out.println("没有找到符合条件的内容");
//            }
//            File imageFile = new File(leftOfFirstSlash, "DCIM/Screenshots/saved_image.png");//存储位置
            String basePath = picturesDir.getAbsolutePath() + "/saved_image";
            String extension = ".png";
            String finalPath = getUniqueFilePath(basePath, extension);
            File imageFile = new File(finalPath);//存储位置,并且路径唯一，因为对路径进行了每次都+1的操作

            fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            Toast.makeText(this, "图像保存成功", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "图像保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    // 获取唯一的文件路径
    private static String getUniqueFilePath(String basePath, String extension) {
        File file;
        int index = 1;
        String filePath;

        // 尝试生成唯一文件路径
        do {
            filePath = basePath + (index > 1 ? "_" + index : "") + extension;
            file = new File(filePath);
            index++;
        } while (file.exists());

        return filePath;
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImage();
            } else {
                Toast.makeText(this, "没有写入存储的权限", Toast.LENGTH_SHORT).show();
            }
        }
    }
}



