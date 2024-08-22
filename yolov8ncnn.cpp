#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>

#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

#include <platform.h>
#include <benchmark.h>

#include "yolo.h"

#include "ndkcamera.h"

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#if __ARM_NEON
#include <arm_neon.h>
#endif // __ARM_NEON
using namespace cv;


#define LOG_TAG "AndroidScalingalgorithmforimage"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

/*
 * 代码分块说明
 * 1.视频检测
 * 2.图像修复
 * 3.video检测
 * 4.图片检测
 * */
//-------------------------------flv-----------------------------------------




































//--------------------------------------Imgfix---------------------------------------

/**
 * 创建目标位图
 * @param env
 * @param new_width 目标位图 宽
 * @param new_height 高
 * @return 目标位图
 */
jobject createBitmap(JNIEnv *env, int new_width, int new_height) {
    jobject outputBitmap;
    jclass bitmapCls = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapCls, "createBitmap",
                                                          "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jobject java_bitmap_config = env->CallStaticObjectMethod(bitmapConfigClass,
                                                             env->GetStaticMethodID(
                                                                     bitmapConfigClass, "valueOf",
                                                                     "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;"),
                                                             configName);
    outputBitmap = env->CallStaticObjectMethod(bitmapCls,
                                               createBitmapMethod,
                                               new_width,
                                               new_height,
                                               java_bitmap_config);
    return outputBitmap;
}

/**
 * 最近邻插值算法
 */
extern "C"
JNIEXPORT jobject JNICALL Java_com_example_ncnn_1yolo_imgfix_1interface_scaleBitmapByNearestNeighbor(JNIEnv *env, jobject thiz,
                                                                                                     jobject inputBitmap,
                                                                                                     jfloat scale) {
    // 定义Android Bitmap信息结构体，用于获取Bitmap的详细信息
    AndroidBitmapInfo bitmapInfo;

    // 定义输入和输出的Mat对象，用于OpenCV的图像处理
    Mat inputMat;
    Mat outputMat;

    // 定义用于存储输出Bitmap的jobject
    jobject outputBitmap;

    // 如果缩放比例为1.0，直接返回原Bitmap，无需进行缩放操作
    if (scale == 1.0) {
        return inputBitmap;
    }

    // 获取输入Bitmap的详细信息，如果获取失败则返回NULL
    if (AndroidBitmap_getInfo(env, inputBitmap, &bitmapInfo) < 0) {
        LOGD("转换失败");
        return NULL;
    }
    LOGD("转换成功");
    LOGD("图片的格式 %d", bitmapInfo.format);

    // 锁定输入Bitmap的像素，以便进行像素数据操作
    void *inputPixels;
    if (AndroidBitmap_lockPixels(env, inputBitmap, &inputPixels) < 0) {
        LOGD("获取像素数据失败");
        return NULL;
    }

    // 创建输入Mat对象，并将输入Bitmap的像素数据拷贝到Mat对象中
    inputMat.create(bitmapInfo.height, bitmapInfo.width, CV_8UC4);
    memcpy(inputMat.data, inputPixels, inputMat.rows * inputMat.step);

    // 解锁输入Bitmap的像素
    AndroidBitmap_unlockPixels(env, inputBitmap);

    // 计算新的宽度和高度，并创建输出Mat对象
    int new_width = static_cast<int>(inputMat.cols * scale);
    int new_height = static_cast<int>(inputMat.rows * scale);
    outputMat.create(new_height, new_width, CV_8UC4);

    // 使用OpenCV的resize函数进行图像缩放，采用最近邻插值方法
    resize(inputMat, outputMat, Size(new_width, new_height), 0, 0, INTER_NEAREST);
    LOGD("图像缩放完成");

    // 创建输出Bitmap对象，用于存储缩放后的图像数据
    outputBitmap = createBitmap(env, new_width, new_height);
    LOGD("将OpenCV的Mat对象转换成Java中的Bitmap对象完成");

    // 锁定输出Bitmap的像素，以便进行像素数据操作
    void *outputPixels;
    int result = AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels);
    if (result < 0) {
        LOGD("outputPixels执行失败 %d", result);
        return NULL;
    }

    // 将缩放后的图像数据从输出Mat对象拷贝到输出Bitmap中
    memcpy(outputPixels, outputMat.data, outputMat.rows * outputMat.step);

    // 解锁输出Bitmap的像素
    AndroidBitmap_unlockPixels(env, outputBitmap);
    LOGD("执行完成");

    // 返回缩放后的Bitmap对象
    return outputBitmap;
}

/**
 * 双线性插值
 */
extern "C"
JNIEXPORT jobject JNICALL Java_com_example_ncnn_1yolo_imgfix_1interface_scaleBitmapByBilinear(JNIEnv *env, jobject thiz,
                                                                    jobject input_bitmap, jfloat scale) {
    if (scale == 1.0) {
        return input_bitmap;
    }

    AndroidBitmapInfo bitmapInfo;
    void *pixels = nullptr;
    if (AndroidBitmap_getInfo(env, input_bitmap, &bitmapInfo) < 0) {
        return NULL;
    }

    if (AndroidBitmap_lockPixels(env, input_bitmap, &pixels) < 0) {
        return NULL;
    }
    AndroidBitmap_unlockPixels(env, input_bitmap);
    LOGD("height = %d, width = %d", bitmapInfo.height, bitmapInfo.width);

    int newWidth = static_cast<int>(bitmapInfo.width * scale);
    int newHeight = static_cast<int>(bitmapInfo.height * scale);
    LOGD("newHeight = %d, newWidth = %d", newHeight, newWidth);

    uint32_t *srcPixels = static_cast<uint32_t *>(pixels);
    uint32_t *dstPixels = new uint32_t[newWidth * newHeight];

    float xRatio = static_cast<float>(bitmapInfo.width - 1) / static_cast<float>(newWidth - 1);
    float yRatio = static_cast<float>(bitmapInfo.height - 1) / static_cast<float>(newHeight - 1);

    for (int y = 0; y < newHeight; ++y) {
        for (int x = 0; x < newWidth; ++x) {
            float gx = x * xRatio;
            float gy = y * yRatio;
            int gxi = static_cast<int>(gx);
            int gyi = static_cast<int>(gy);
            float fracx = gx - gxi;
            float fracy = gy - gyi;

            uint32_t c00 = srcPixels[gyi * bitmapInfo.width + gxi];
            uint32_t c10 = srcPixels[gyi * bitmapInfo.width + gxi + 1];
            uint32_t c01 = srcPixels[(gyi + 1) * bitmapInfo.width + gxi];
            uint32_t c11 = srcPixels[(gyi + 1) * bitmapInfo.width + gxi + 1];

            int r = static_cast<int>((1 - fracx) * (1 - fracy) * (c00 >> 16 & 0xff) +
                                     fracx * (1 - fracy) * (c10 >> 16 & 0xff) +
                                     (1 - fracx) * fracy * (c01 >> 16 & 0xff) +
                                     fracx * fracy * (c11 >> 16 & 0xff));

            int g = static_cast<int>((1 - fracx) * (1 - fracy) * (c00 >> 8 & 0xff) +
                                     fracx * (1 - fracy) * (c10 >> 8 & 0xff) +
                                     (1 - fracx) * fracy * (c01 >> 8 & 0xff) +
                                     fracx * fracy * (c11 >> 8 & 0xff));

            int b = static_cast<int>((1 - fracx) * (1 - fracy) * (c00 & 0xff) +
                                     fracx * (1 - fracy) * (c10 & 0xff) +
                                     (1 - fracx) * fracy * (c01 & 0xff) +
                                     fracx * fracy * (c11 & 0xff));
            dstPixels[y * newWidth + x] = 0xff000000 | (r << 16) | (g << 8) | b;
        }
    }

    jobject newBitmap = createBitmap(env, newWidth, newHeight);
    LOGD("将OpenCV的Mat对象转换成Java中的Bitmap对象完成");

    void *outputPixels;
    int result = AndroidBitmap_lockPixels(env, newBitmap, &outputPixels);
    if (result < 0) {
        LOGD("outputPixels执行失败 %d", result);
        return NULL;
    }

    memcpy(outputPixels, dstPixels, newWidth * newHeight * 4);
    AndroidBitmap_unlockPixels(env, newBitmap);
    LOGD("执行完成");

    delete[] dstPixels; // 释放分配的内存
    return newBitmap;
}
/*
 * 双三次插值
 * */

extern "C"
JNIEXPORT jobject JNICALL Java_com_example_ncnn_1yolo_imgfix_1interface_scaleBitmapByBicubic(JNIEnv *env, jobject thiz,
                                                                                             jobject inputBitmap,
                                                                                             jfloat scale) {
    AndroidBitmapInfo bitmapInfo;
    Mat inputMat;
    Mat outputMat;
    jobject outputBitmap;

    // 如果缩放比例为1.0，直接返回原Bitmap，无需进行缩放操作
    if (scale == 1.0) {
        return inputBitmap;
    }

    // 获取输入Bitmap的详细信息，如果获取失败则返回NULL
    if (AndroidBitmap_getInfo(env, inputBitmap, &bitmapInfo) < 0) {
        LOGD("获取Bitmap信息失败");
        return NULL;
    }

    // 锁定输入Bitmap的像素，以便进行像素数据操作
    void *inputPixels;
    if (AndroidBitmap_lockPixels(env, inputBitmap, &inputPixels) < 0) {
        LOGD("锁定Bitmap像素失败");
        return NULL;
    }

    // 创建输入Mat对象，并将输入Bitmap的像素数据拷贝到Mat对象中
    inputMat.create(bitmapInfo.height, bitmapInfo.width, CV_8UC4);
    memcpy(inputMat.data, inputPixels, inputMat.rows * inputMat.step);

    // 解锁输入Bitmap的像素
    AndroidBitmap_unlockPixels(env, inputBitmap);

    // 计算新的宽度和高度，并创建输出Mat对象
    int new_width = static_cast<int>(inputMat.cols * scale);
    int new_height = static_cast<int>(inputMat.rows * scale);
    outputMat.create(new_height, new_width, CV_8UC4);

    // 使用OpenCV的resize函数进行图像缩放，采用双三次插值方法
    resize(inputMat, outputMat, Size(new_width, new_height), 0, 0, INTER_CUBIC);
    LOGD("图像缩放完成");

    // 使用提供的createBitmap函数创建输出Bitmap对象
    outputBitmap = createBitmap(env, new_width, new_height);
    LOGD("创建输出Bitmap对象完成");

    // 锁定输出Bitmap的像素，以便进行像素数据操作
    void *outputPixels;
    int result = AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels);
    if (result < 0) {
        LOGD("锁定输出Bitmap像素失败 %d", result);
        return NULL;
    }

    // 将缩放后的图像数据从输出Mat对象拷贝到输出Bitmap中
    memcpy(outputPixels, outputMat.data, outputMat.rows * outputMat.step);

    // 解锁输出Bitmap的像素
    AndroidBitmap_unlockPixels(env, outputBitmap);
    LOGD("执行完成");

    // 返回缩放后的Bitmap对象
    return outputBitmap;
}




//------------------------------------video检测--------------------------------------------------
static int draw_unsupported(cv::Mat& rgb)
{
    const char text[] = "unsupported";

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 1.0, 1, &baseLine);

    int y = (rgb.rows - label_size.height) / 2;
    int x = (rgb.cols - label_size.width) / 2;

    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                    cv::Scalar(255, 255, 255), -1);

    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 1.0, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_fps(cv::Mat& rgb)
{
    // 计算移动平均值
    float avg_fps = 0.f;
    {
        static double t0 = 0.f; // 上一帧时间
        static float fps_history[10] = {0.f}; // 存储最近10帧的FPS

        double t1 = ncnn::get_current_time(); // 获取当前时间
        if (t0 == 0.f)
        {
            t0 = t1;
            return 0;
        }

        float fps = 1000.f / (t1 - t0); // 计算当前帧率
        t0 = t1; // 更新上一帧时间

        // 更新FPS历史记录
        for (int i = 9; i >= 1; i--)
        {
            fps_history[i] = fps_history[i - 1];
        }
        fps_history[0] = fps;

        // 如果历史记录未满，返回0
        if (fps_history[9] == 0.f)
        {
            return 0;
        }

        // 计算10帧的平均FPS
        for (int i = 0; i < 10; i++)
        {
            avg_fps += fps_history[i];
        }
        avg_fps /= 10.f;
    }

    // 绘制FPS文本
    char text[32];
    sprintf(text, "FPS=%.2f", avg_fps);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 0; // 文本位置的y坐标
    int x = rgb.cols - label_size.width; // 文本位置的x坐标

    // 在图像上绘制矩形背景
    cv::rectangle(rgb, cv::Rect(cv::Point(x, y), cv::Size(label_size.width, label_size.height + baseLine)),
                  cv::Scalar(255, 255, 255), -1);

    // 在图像上绘制FPS文本
    cv::putText(rgb, text, cv::Point(x, y + label_size.height),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

static int draw_detection_count(cv::Mat& rgb, int detection_count)
{
    // 绘制检测数量文本
    char text[32];
    sprintf(text, "Count=%d", detection_count);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 10; // 文本位置的y坐标，距离图像顶部一定距离
    int x = 0; // 文本位置的x坐标，距离图像右侧一定距离

    // 在图像上绘制矩形背景
    cv::rectangle(rgb, cv::Rect(cv::Point(x - 5, y - label_size.height - 5), cv::Size(label_size.width + 10, label_size.height + baseLine + 10)),
                  cv::Scalar(255, 255, 255), -1);

    // 在图像上绘制检测数量文本
    cv::putText(rgb, text, cv::Point(x, y),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}

//-----------------测距-------------------------
float calculate_distance(float object_height_in_image, float real_height=0.01f) {
    float focal_length = 1990.0f; // 镜头焦距
    // 计算距离的公式
    float dis_inch = (real_height * focal_length) / (object_height_in_image - 2.0f);
    float dis_cm = dis_inch * 2.54f;  // 转换为厘米
    dis_cm = static_cast<int>(dis_cm);  // 转换为整数
    float dis_m = dis_cm / 100.0f;  // 转换为米
    return dis_m;
}

static int draw_distance(cv::Mat& rgb, float distance) {
    // 绘制距离文本
    char text[32];
    sprintf(text, "Distance=%.2f m", distance);

    int baseLine = 0;
    cv::Size label_size = cv::getTextSize(text, cv::FONT_HERSHEY_SIMPLEX, 0.5, 1, &baseLine);

    int y = 40; // 文本位置的y坐标，距离图像顶部一定距离
    int x = rgb.cols - label_size.width - 10; // 文本位置的x坐标，距离图像右侧一定距离

    // 在图像上绘制矩形背景
    cv::rectangle(rgb, cv::Rect(cv::Point(x - 5, y - label_size.height - 5), cv::Size(label_size.width + 10, label_size.height + baseLine + 10)),
                  cv::Scalar(255, 255, 255), -1);

    // 在图像上绘制距离文本
    cv::putText(rgb, text, cv::Point(x, y),
                cv::FONT_HERSHEY_SIMPLEX, 0.5, cv::Scalar(0, 0, 0));

    return 0;
}



static Yolo* g_yolo = 0;
static ncnn::Mutex lock;

class MyNdkCamera : public NdkCameraWindow
{
public:
    virtual void on_image_render(cv::Mat& rgb) const;
};

void MyNdkCamera::on_image_render(cv::Mat& rgb) const
{
    // nanodet
    {
        ncnn::MutexLockGuard g(lock);  // 加锁，确保线程安全

        if (g_yolo)  // 检查YOLO模型是否已初始化
        {
            std::vector<Object> objects;  // 创建一个对象列表来存储检测到的目标
            g_yolo->detect(rgb, objects);  // 使用YOLO模型进行目标检测，将检测到的目标存储到objects中

            g_yolo->draw(rgb, objects);  // 在图像上绘制检测到的目标

            // 绘制检测数量
            int detection_count = static_cast<int>(objects.size());  // 获取检测到的目标数量
            draw_detection_count(rgb, detection_count);

//            // 目标的实际宽度、摄像头的焦距和目标的实际宽度
//            float real_width = 0.5f; // 目标的实际宽度，例如0.5米
//            float focal_length = 500.f; // 摄像头的焦距，例如500像素

//            for (const auto& obj : objects)
//            {
//                float object_height_in_image = y1f(obj.rect.height) - y0f(obj.rect.height);  // 计算目标(y1(obj.rect.width))在图像中的高度
//                float object_width_in_image = obj.rect.width;
//                float distance = calculate_distance(object_height_in_image);
//                draw_distance(rgb, distance);  // 在图像上绘制距离
//            }
        }
        else
        {
            draw_unsupported(rgb);  // 如果YOLO模型未初始化，绘制一个表示不支持的图像
        }
    }

    draw_fps(rgb);  // 在图像上绘制帧率信息
}

static MyNdkCamera* g_camera = 0;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnLoad");

    g_camera = new MyNdkCamera;

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "JNI_OnUnload");

    {
        ncnn::MutexLockGuard g(lock);

        delete g_yolo;
        g_yolo = 0;
    }

    delete g_camera;
    g_camera = 0;
}

// public native boolean loadModel(AssetManager mgr, int modelid, int cpugpu);
JNIEXPORT jboolean JNICALL Java_com_example_ncnn_1yolo_ncnn_1yolo_loadModel(JNIEnv* env, jobject thiz, jobject assetManager, jint modelid, jint cpugpu)
{
    if (modelid < 0 || modelid > 6 || cpugpu < 0 || cpugpu > 1)
    {
        return JNI_FALSE;
    }

    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "loadModel %p", mgr);

    const char* modeltypes[] =
            {
//        "n",
//        "s",
                    "best"
            };

    const int target_sizes[] =
            {
                    640,
                    640,
            };

    const float mean_vals[][3] =
            {
                    {103.53f, 116.28f, 123.675f},
                    {103.53f, 116.28f, 123.675f},
            };

    const float norm_vals[][3] =
            {
                    { 1 / 255.f, 1 / 255.f, 1 / 255.f },
                    { 1 / 255.f, 1 / 255.f, 1 / 255.f },
            };

    const char* modeltype = modeltypes[(int)modelid];
    int target_size = target_sizes[(int)modelid];
    bool use_gpu = (int)cpugpu == 1;

    // reload
    {
        ncnn::MutexLockGuard g(lock);

        if (use_gpu && ncnn::get_gpu_count() == 0)
        {
            // no gpu
            delete g_yolo;
            g_yolo = 0;
        }
        else
        {
            if (!g_yolo)
                g_yolo = new Yolo;
            g_yolo->load(mgr, modeltype, target_size, mean_vals[(int)modelid], norm_vals[(int)modelid], use_gpu);
        }
    }

    return JNI_TRUE;
}

// public native boolean openCamera(int facing);
JNIEXPORT jboolean JNICALL Java_com_example_ncnn_1yolo_ncnn_1yolo_openCamera(JNIEnv* env, jobject thiz, jint facing)
{
    if (facing < 0 || facing > 1)
        return JNI_FALSE;

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "openCamera %d", facing);

    g_camera->open((int)facing);

    return JNI_TRUE;
}

// public native boolean closeCamera();
JNIEXPORT jboolean JNICALL Java_com_example_ncnn_1yolo_ncnn_1yolo_closeCamera(JNIEnv* env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "closeCamera");

    g_camera->close();

    return JNI_TRUE;
}

// public native boolean setOutputWindow(Surface surface);
JNIEXPORT jboolean JNICALL Java_com_example_ncnn_1yolo_ncnn_1yolo_setOutputWindow(JNIEnv* env, jobject thiz, jobject surface)
{
    ANativeWindow* win = ANativeWindow_fromSurface(env, surface);

    __android_log_print(ANDROID_LOG_DEBUG, "ncnn", "setOutputWindow %p", win);

    g_camera->set_window(win);

    return JNI_TRUE;
}


// -------------------------------处理图像并返回检测结果-------------------------------------
JNIEXPORT jobject JNICALL Java_com_example_ncnn_1yolo_ncnn_1yolo_detectFromBitmap(JNIEnv* env, jobject thiz, jobject bitmap) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "Yolo", "Failed to get Bitmap info");
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGB_565 && info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        __android_log_print(ANDROID_LOG_ERROR, "Yolo", "Unsupported Bitmap format");
        return nullptr;
    }

    void* pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "Yolo", "Failed to lock Bitmap pixels");
        return nullptr;
    }

    // Convert Bitmap to cv::Mat
    size_t pixelCount = info.width * info.height;
    size_t rgbDataSize = pixelCount * 3;
    std::vector<unsigned char> rgbData(rgbDataSize);
    uint8_t* p = static_cast<uint8_t*>(pixels);

    for (size_t i = 0; i < pixelCount; ++i) {
        if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
            uint16_t pixel = *reinterpret_cast<uint16_t*>(p);
            rgbData[i * 3] = ((pixel >> 8) & 0xF8) | ((pixel >> 13) & 0x07);
            rgbData[i * 3 + 1] = ((pixel >> 3) & 0xF8) | ((pixel >> 11) & 0x07);
            rgbData[i * 3 + 2] = ((pixel << 3) & 0xF8) | ((pixel >> 5) & 0x07);
            p += 2;
        } else if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
            rgbData[i * 3] = p[0];
            rgbData[i * 3 + 1] = p[1];
            rgbData[i * 3 + 2] = p[2];
            p += 4;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
    cv::Mat mat(info.height, info.width, CV_8UC3, rgbData.data());

    std::vector<Object> objects;
    ncnn::MutexLockGuard g(lock);
    if (g_yolo) {
        g_yolo->detect(mat, objects);
    }

    // Create ArrayList to store results
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListInit = env->GetMethodID(arrayListClass, "<init>", "()V");
    jobject arrayListObj = env->NewObject(arrayListClass, arrayListInit);

    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");

    for (const auto& obj : objects) {
        jclass hashMapClass = env->FindClass("java/util/HashMap");
        jmethodID hashMapInit = env->GetMethodID(hashMapClass, "<init>", "()V");
        jobject hashMapObj = env->NewObject(hashMapClass, hashMapInit);

        jmethodID putMethod = env->GetMethodID(hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

        jstring xKey = env->NewStringUTF("x");
        jstring xValue = env->NewStringUTF(std::to_string(obj.rect.x).c_str());
        env->CallObjectMethod(hashMapObj, putMethod, xKey, xValue);

        jstring yKey = env->NewStringUTF("y");
        jstring yValue = env->NewStringUTF(std::to_string(obj.rect.y).c_str());
        env->CallObjectMethod(hashMapObj, putMethod, yKey, yValue);

        jstring wKey = env->NewStringUTF("w");
        jstring wValue = env->NewStringUTF(std::to_string(obj.rect.width).c_str());
        env->CallObjectMethod(hashMapObj, putMethod, wKey, wValue);

        jstring hKey = env->NewStringUTF("h");
        jstring hValue = env->NewStringUTF(std::to_string(obj.rect.height).c_str());
        env->CallObjectMethod(hashMapObj, putMethod, hKey, hValue);

        jstring labelKey = env->NewStringUTF("label");
        jstring labelValue = env->NewStringUTF(std::to_string(obj.label).c_str());
        env->CallObjectMethod(hashMapObj, putMethod, labelKey, labelValue);

        jstring probKey = env->NewStringUTF("prob");
        jstring probValue = env->NewStringUTF(std::to_string(obj.prob).c_str());
        env->CallObjectMethod(hashMapObj, putMethod, probKey, probValue);

        env->CallBooleanMethod(arrayListObj, addMethod, hashMapObj);
        env->DeleteLocalRef(hashMapObj);
    }

    return arrayListObj;
}

}



