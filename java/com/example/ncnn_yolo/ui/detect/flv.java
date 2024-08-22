package com.example.ncnn_yolo.ui.detect;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ncnn_yolo.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import android.view.TextureView;


public class flv extends Fragment {

    private VideoView videoView;
    private SeekBar seekBar;
    private TextView timeTextView;
    private Uri videoUri;
    private static final int REQUEST_VIDEO_GET = 1;
    private int playbackPosition = 0;
    private boolean isVideoEnded = false;

    private Spinner spinnerModel;
    private Spinner spinnerCPUGPU;
    private int current_model = 0;
    private int current_cpugpu = 0;
    int currentPosition;

    //----------------目标检测----------------------
    private com.example.ncnn_yolo.ncnn_yolo yolov8ncnn = new com.example.ncnn_yolo.ncnn_yolo();
    private boolean autoDetect = false;
    private TextureView textureView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.flv, container, false);

        // 保持屏幕常亮
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 初始化VideoView、SeekBar、TextView及其他UI元素
        videoView = rootView.findViewById(R.id.videoView);
        seekBar = rootView.findViewById(R.id.seekBar);
        timeTextView = rootView.findViewById(R.id.timeTextView);
        textureView=rootView.findViewById(R.id.textureView);


        Button btnPlay = rootView.findViewById(R.id.btnPlay);
        Button btnPause = rootView.findViewById(R.id.btnPause);
        Button btnReplay = rootView.findViewById(R.id.btnReplay);
        Button btnUpload = rootView.findViewById(R.id.btnUpload);
        Button buttonDetect = rootView.findViewById(R.id.buttonDetect);
        Button btnZoom = rootView.findViewById(R.id.btnZoom);

        // 播放按钮点击事件
        btnPlay.setOnClickListener(v -> {
            if (!videoView.isPlaying()) {
                videoView.start();
                isVideoEnded = false;
                seekBar.postDelayed(updateSeekBar, 1000);
            }
        });

        // 暂停按钮点击事件
        btnPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
            }
        });

        // 重播按钮点击事件
        btnReplay.setOnClickListener(v -> {
            videoView.seekTo(0);
            videoView.start();
            isVideoEnded = false;
            seekBar.setProgress(0);
            timeTextView.setText("00:00 / " + formatTime(videoView.getDuration()));
            seekBar.removeCallbacks(updateSeekBar);
            seekBar.postDelayed(updateSeekBar, 1000);
        });

        // 上传按钮点击事件
        btnUpload.setOnClickListener(v -> selectVideo());

        // 视频检测按钮点击事件
        buttonDetect.setOnClickListener(v -> {
            if (videoUri != null) {
                autoDetect = !autoDetect;
                if (autoDetect) {
                    Toast.makeText(getContext(), "自动检测已启动", Toast.LENGTH_SHORT).show();
                    seekBar.postDelayed(updateSeekBar, 1000);
                } else {
                    Toast.makeText(getContext(), "自动检测已停止", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "请先上传视频", Toast.LENGTH_SHORT).show();
            }
        });

        // 设置Spinner
        spinnerModel = rootView.findViewById(R.id.spinnerModel);
        spinnerModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != current_model) {
                    current_model = position;
                    reloadModel();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 无操作
            }
        });

        spinnerCPUGPU = rootView.findViewById(R.id.spinnerCPUGPU);
        spinnerCPUGPU.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != current_cpugpu) {
                    current_cpugpu = position;
                    reloadModel();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 无操作
            }
        });

        // 设置SeekBar的准备事件监听器
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            if (playbackPosition > 0) {
                videoView.seekTo(playbackPosition);
                playbackPosition = 0;
            }
            seekBar.setMax(videoView.getDuration());
            updateTimeTextView();
            seekBar.postDelayed(updateSeekBar, 1000);
        });

        // 设置视频播放结束监听器
        videoView.setOnCompletionListener(mp -> {
            isVideoEnded = true;
            seekBar.setProgress(videoView.getDuration());
            updateTimeTextView();
            seekBar.removeCallbacks(updateSeekBar);
        });

        // 设置SeekBar的监听器
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTimeTextView();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                videoView.seekTo(seekBar.getProgress());
                if (!videoView.isPlaying()) {
                    videoView.start();
                }
            }
        });

        // 初始化视频路径
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            initVideoPath();
        }

        // 设置缩放按钮的点击事件
        btnZoom.setOnClickListener(v -> toggleZoom());

        return rootView;
    }

    private void initVideoPath() {
        File file = new File(Environment.getExternalStorageDirectory(), "big_buck_bunny.mp4");
        if (file.exists()) {
            videoView.setVideoPath(file.getPath());
            videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                if (playbackPosition > 0) {
                    videoView.seekTo(playbackPosition);
                    playbackPosition = 0;
                }
                seekBar.setMax(videoView.getDuration());
                seekBar.postDelayed(updateSeekBar, 1000);
            });
        }
    }

    private void selectVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_VIDEO_GET);
        isVideoEnded = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initVideoPath();
            } else {
                Toast.makeText(getContext(), "权限被拒绝，无法使用应用。", Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_VIDEO_GET && resultCode == getActivity().RESULT_OK) {
            videoUri = data.getData();
            videoView.setVideoURI(videoUri);
            videoView.setOnPreparedListener(mp -> {
                videoView.start();
                seekBar.setProgress(0);
                seekBar.setMax(videoView.getDuration());
                timeTextView.setText("00:00 / " + formatTime(videoView.getDuration()));
                seekBar.removeCallbacks(updateSeekBar);
                seekBar.postDelayed(updateSeekBar, 1000);
            });
            autoDetect = false; // 上传新视频后开始检测
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            playbackPosition = videoView.getCurrentPosition();
            videoView.suspend();
            isVideoEnded = true;
        }
        seekBar.removeCallbacks(updateSeekBar);
    }

    private void reloadModel() {
        boolean ret_init = yolov8ncnn.loadModel(getActivity().getAssets(), current_model, current_cpugpu);
        if (!ret_init) {
            Log.e("FlvFragment", "yolov8ncnn loadModel failed");
        }
    }


    private void toggleZoom() {
        videoView.setScaleX(videoView.getScaleX() == 1.0f ? 2.0f : 1.0f);
        videoView.setScaleY(videoView.getScaleY() == 1.0f ? 2.0f : 1.0f);
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (videoView.isPlaying() && !isVideoEnded) {
                int currentPosition = videoView.getCurrentPosition();
                seekBar.setProgress(currentPosition);
                updateTimeTextView();
                // 在视频播放时自动检测
                if (autoDetect) {
                    detectCurrentFrame();
                }

                seekBar.postDelayed(this, 1000);
            }
        }
    };

    private void updateTimeTextView() {
        if (videoView != null) {
            if (videoView.isPlaying() || isVideoEnded) {
                currentPosition = videoView.getCurrentPosition();
            } else {
                currentPosition = seekBar.getProgress();
            }
            int duration = videoView.getDuration();
            String currentTime = formatTime(currentPosition);
            String totalTime = formatTime(duration);
            timeTextView.setText(String.format("%s / %s", currentTime, totalTime));
        } else {
            Log.e("FlvFragment", "videoView is null in updateTimeTextView");
        }
    }

    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    //----------------目标检测---------------------------
    private Bitmap getVideoFrame(Uri videoUri, long positionMs) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        try {
            retriever.setDataSource(getContext(), videoUri);
            bitmap = retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
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
        textPaint.setTextSize(60);

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

//    private void detectCurrentFrame() {
//        if (videoUri != null) {
//            new Thread(() -> {
//                // 获取当前帧并进行目标检测
//                Bitmap currentFrame = getVideoFrame(videoUri, videoView.getCurrentPosition());
//                if (currentFrame != null) {
//                    // 处理图像并绘制检测结果
//                    Bitmap resultBitmap = processImage(currentFrame);
//                    if (resultBitmap != null) {
//                        // 在UI线程更新TextureView
//                        getActivity().runOnUiThread(() -> {
//                            if (textureView.isAvailable()) {
//                                Canvas canvas = textureView.lockCanvas();
//                                if (canvas != null) {
//                                    canvas.drawBitmap(resultBitmap, 0, 0, null); // 将处理后的Bitmap绘制到TextureView上
//                                    textureView.unlockCanvasAndPost(canvas); // 解锁并发布内容到TextureView
//                                } else {
//                                    Toast.makeText(getContext(), "无法获取Canvas", Toast.LENGTH_SHORT).show();
//                                }
//                            } else {
//                                Toast.makeText(getContext(), "TextureView不可用", Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    } else {
//                        getActivity().runOnUiThread(() ->
//                                Toast.makeText(getContext(), "绘制检测结果失败", Toast.LENGTH_SHORT).show()
//                        );
//                    }
//                } else {
//                    getActivity().runOnUiThread(() ->
//                            Toast.makeText(getContext(), "无法获取视频帧", Toast.LENGTH_SHORT).show()
//                    );
//                }
//            }).start();
//        } else {
//            Toast.makeText(getContext(), "请先上传视频", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void detectCurrentFrame() {
        if (videoUri != null) {
            new Thread(() -> {
                while (videoView.isPlaying()) {
                    Bitmap currentFrame = getVideoFrame(videoUri, videoView.getCurrentPosition());
                    if (currentFrame != null) {
                        new Thread(() -> {
                            Bitmap resultBitmap = processImage(currentFrame);
                            if (resultBitmap != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (textureView.isAvailable()) {
                                        Canvas canvas = textureView.lockCanvas();
                                        if (canvas != null) {
                                            canvas.drawBitmap(resultBitmap, 0, 0, null);
                                            textureView.unlockCanvasAndPost(canvas);
                                        }
                                        else {
                                            Toast.makeText(getContext(), "无法获取Canvas", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    else {
                                        Toast.makeText(getContext(), "TextureView不可用", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            else {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "绘制检测结果失败", Toast.LENGTH_SHORT).show()
                                );
                            }
                        }).start();
                    }
                    else {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "无法获取视频帧", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }).start();
        }
        else{
            Toast.makeText(getContext(), "请先上传视频", Toast.LENGTH_SHORT).show();
        }
    }


}
