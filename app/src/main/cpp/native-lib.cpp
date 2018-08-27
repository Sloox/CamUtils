#include <jni.h>
#include <string>

using namespace std;

extern "C" JNIEXPORT jstring

JNICALL
Java_wrightstuff_co_za_camutils_mainview_CamScreenActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    string hello = "May use me here";
    return env->NewStringUTF(hello.c_str());
}
