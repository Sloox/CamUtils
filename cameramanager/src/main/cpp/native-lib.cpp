#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

using namespace std;

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "CameraUtils Native", __VA_ARGS__)

extern "C" {

JNIEXPORT void JNICALL Java_wrightstuff_co_za_cameramanager_utils_NativeUtils_RGBADisplay(
        JNIEnv *env,
        jobject obj,
        jint srcWidth,
        jint srcHeight,
        jint Y_rowStride,
        jobject Y_Buffer,
        jint UV_rowStride,
        jobject U_Buffer,
        jobject V_Buffer,
        jobject surface) {

    uint8_t *srcYPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(Y_Buffer));
    uint8_t *srcUPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(U_Buffer));
    uint8_t *srcVPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(V_Buffer));
    
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    ANativeWindow_acquire(window);
    ANativeWindow_Buffer buffer;
    //WINDOW_FORMAT_RGBA_8888(DEFAULT), WINDOW_FORMAT_RGBX_8888, WINDOW_FORMAT_RGB_565
    ANativeWindow_setBuffersGeometry(window, 0, 0, WINDOW_FORMAT_RGBA_8888);
    if (int32_t err = ANativeWindow_lock(window, &buffer, NULL)) {
        LOGE("ANativeWindow_lock failed with error code: %d\n", err);
        ANativeWindow_release(window);
    }

    size_t bufferSize = buffer.width * buffer.height * (size_t) 4;

    //YUV_420_888 to RGBA_8888 conversion and flip
    uint8_t *outPtr = reinterpret_cast<uint8_t *>(buffer.bits);
    for (size_t y = 0; y < srcHeight; y++) {
        uint8_t *Y_rowPtr = srcYPtr + y * Y_rowStride;
        uint8_t *U_rowPtr = srcUPtr + (y >> 1) * UV_rowStride;
        uint8_t *V_rowPtr = srcVPtr + (y >> 1) * UV_rowStride;
        for (size_t x = 0; x < srcWidth; x++) {
            uint8_t Y = Y_rowPtr[x];
            uint8_t U = U_rowPtr[(x >> 1)];
            uint8_t V = V_rowPtr[(x >> 1)];
            double R = (Y + (V - 128) * 1.40625);
            double G = (Y - (U - 128) * 0.34375 - (V - 128) * 0.71875);
            double B = (Y + (U - 128) * 1.765625);
            *(outPtr + (--bufferSize)) = 255; // gamma for RGBA_8888
            *(outPtr + (--bufferSize)) = (uint8_t) (B > 255 ? 255 : (B < 0 ? 0 : B));
            *(outPtr + (--bufferSize)) = (uint8_t) (G > 255 ? 255 : (G < 0 ? 0 : G));
            *(outPtr + (--bufferSize)) = (uint8_t) (R > 255 ? 255 : (R < 0 ? 0 : R));
        }
    }

    ANativeWindow_unlockAndPost(window);
    ANativeWindow_release(window);
}
}