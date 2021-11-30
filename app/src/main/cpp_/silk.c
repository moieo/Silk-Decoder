#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <malloc.h>
#include <android/log.h>

extern char decoder[];
extern int decoder_size;

#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, "SILK_NDK", __VA_ARGS__))

jstring getPath(JNIEnv* env);

JNIEXPORT void JNICALL Java_com_ecodemo_silk_MainActivity_initSilkDecoder(JNIEnv *env, __attribute__((__unused__)) jobject obj){
    /* unsigned char* byteData[105074];
    (*env)->GetByteArrayRegion(env, decoder, 0, decoder_size, decoder);*/
    
    jstring path_ = getPath(env);
    char* path_decoder = malloc(128);
    const char* path = (*env)->GetStringUTFChars(env, path_, 0);
    
    strcpy(path_decoder, path);
    strcat(path_decoder, "/decoder");
    // LOGW("%s", path_decoder);
    
    FILE *fp_;
    if((fp_=fopen(path_decoder, "r")) != NULL) return;
    
    FILE *fp;
    if((fp=fopen(path_decoder, "wb")) == NULL) return;
    fwrite(decoder, sizeof(char), decoder_size, fp);
    fclose(fp);
    
    char* cmd = malloc(128);
    strcpy(cmd, "/system/bin/chmod 755 ");
    strcat(cmd, path_decoder);
    system(cmd);
}


JNIEXPORT void JNICALL Java_com_ecodemo_silk_MxRecyclerAdapter_exec(JNIEnv *env, __attribute__((__unused__)) jobject obj, jstring cmd ){
    jstring path_ = getPath(env);
    char* path_decoder = malloc(128);
    const char* path = (*env)->GetStringUTFChars(env, path_, 0);
    
    strcpy(path_decoder, path);
    strcat(path_decoder, "/");
    strcat(path_decoder, (*env)->GetStringUTFChars(env, cmd, 0));
    system(path_decoder);
}


jstring getPath(JNIEnv* env) {
    //获取Activity Thread的实例对象
    jclass activityThread = (*env)->FindClass(env, "android/app/ActivityThread");
    jmethodID currentActivityThread = (*env)->GetStaticMethodID(env, activityThread, "currentActivityThread", "()Landroid/app/ActivityThread;");
    jobject at = (*env)->CallStaticObjectMethod(env, activityThread, currentActivityThread);
    //获取Application
    jmethodID getApplication = (*env)->GetMethodID(env, activityThread, "getApplication", "()Landroid/app/Application;");
    jobject context = (*env)->CallObjectMethod(env, at, getApplication);
    
    /* 得到 File */
    jclass context_clazz = (*env)->GetObjectClass(env, context);
    jmethodID get_files_id = (*env)->GetMethodID(env, context_clazz, "getFilesDir", "()Ljava/io/File;");
    jobject file_obj = (*env)->CallObjectMethod(env, context, get_files_id);
    
    
    /* 得到文件夹路径 */
    jclass file_path_clazz = (*env)->GetObjectClass(env, file_obj);
    jmethodID get_path_id = (*env)->GetMethodID(env, file_path_clazz, "getAbsolutePath", "()Ljava/lang/String;");
    return (*env)->CallObjectMethod(env, file_obj, get_path_id);
}