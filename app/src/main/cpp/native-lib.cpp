#include <jni.h>
#include <string>

using namespace std;

extern "C" JNIEXPORT jstring

JNICALL
Java_wrightstuff_co_za_camutils_mainview_CamScreenActivity_stringFromJNI(JNIEnv *env, jobject /* this */) {
    string hello = "Remember the JNI methods!";
    return env->NewStringUTF(hello.c_str());
}
