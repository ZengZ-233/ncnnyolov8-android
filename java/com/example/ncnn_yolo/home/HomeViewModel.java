package com.example.ncnn_yolo.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;
    private final MutableLiveData<String> mTextState;
    private final MutableLiveData<Integer> mTextStateColor; // MutableLiveData for color

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("道路安全检测");

        mTextState = new MutableLiveData<>();
        mTextState.setValue("说明\n" +
                "用于道路裂缝或坍塌等情况的道路危险检测\n" +
                "在“摄像头检测”可以进行摄像头的实时检测\n" +
                "在“图片检测”中可以对图像进行检测以及保存\n" +
                "在“图像增强”中可以对图像进行高清处理\n" +
                "在“视频检测”中可以导入视频对视频进行检测");

        mTextStateColor = new MutableLiveData<>();
        mTextStateColor.setValue(android.graphics.Color.BLACK); // Default color is black
    }

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<String> getTextState() {
        return mTextState;
    }

    public LiveData<Integer> getTextStateColor() {
        return mTextStateColor;
    }

    public void setTextState(String state) {
        mTextState.setValue(state);
    }

    public void setTextStateColor(int color) {
        mTextStateColor.setValue(color);
    }
}
