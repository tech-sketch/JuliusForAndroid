#include <stdbool.h>
#include "jp_co_tis_stc_julius_JuliusActivity.h"
#include <julius/juliuslib.h>

#ifdef ANDROID_DEBUG
#include <android/log.h>
#endif

Recog *recog;
JNIEnv *genv;
jobject *gobj;

static void output_result(Recog *recog, void *dummy);

JNIEXPORT jboolean JNICALL Java_jp_co_tis_stc_julius_JuliusActivity_initJulius
  (JNIEnv *env, jobject obj, jstring jconfpath)
{
  Jconf *jconf;
  genv = env;
  gobj = &obj;

  char* path = (char *)(*genv)->GetStringUTFChars(genv, jconfpath, NULL);

#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "initJulius start path:%s", path);
#endif

  jconf = j_config_load_file_new(path);
  if (jconf == NULL){
#ifdef ANDROID_DEBUG
    __android_log_print(ANDROID_LOG_ERROR, "Julius interface.c", "j_config_load_file_new error");
#endif
    return false;
  }
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "configuration loaded");
#endif

  recog = j_create_instance_from_jconf(jconf);
  if (recog == NULL) {
#ifdef ANDROID_DEBUG
    __android_log_print(ANDROID_LOG_ERROR, "Julius interface.c", "j_create_instance_from_jconf error");
#endif
    return false;
  }
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "recognition instance created");
#endif

  callback_add(recog, CALLBACK_RESULT, output_result, NULL);
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "result callback added");
#endif

  if (j_adin_init(recog) == false) {
#ifdef ANDROID_DEBUG
    __android_log_print(ANDROID_LOG_ERROR, "Julius interface.c", "j_adin_init error");
#endif
    return false;
  }

  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "%d %d", jconf->input.speech_input, SP_MFCFILE);
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "initJulius end");
#endif
  return true;
}

JNIEXPORT void JNICALL Java_jp_co_tis_stc_julius_JuliusActivity_recognize
  (JNIEnv *env, jobject obj, jstring jwavepath)
{
  int ret;
  genv = env;
  gobj = &obj;

  char* wave = (char *)(*env)->GetStringUTFChars(env, jwavepath, NULL);
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "recognize start wave:%s", wave);
#endif

  if (j_open_stream(recog, wave) == -1) {
#ifdef ANDROID_DEBUG
    __android_log_print(ANDROID_LOG_ERROR, "Julius interface.c", "j_open_stream error");
#endif
    jclass jcls = (*env)->GetObjectClass(env, obj);
    jmethodID jmethod = (*env)->GetMethodID(env, jcls, "callback", "(Ljava/lang/String;)V");
    jstring jstr = (*env)->NewStringUTF(env, "wave open error");
    (*env)->CallVoidMethod(env, obj, jmethod, jstr);
    (*env)->DeleteLocalRef(env, jstr);
    return;
  }
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "stream opened %s", wave);
#endif
  if (j_recognize_stream(recog) == -1) {
#ifdef ANDROID_DEBUG
    __android_log_print(ANDROID_LOG_ERROR, "Julius interface.c", "j_recognize_stream error");
#endif
    jclass jcls = (*env)->GetObjectClass(env, obj);
    jmethodID jmethod = (*env)->GetMethodID(env, jcls, "callback", "(Ljava/lang/String;)V");
    jstring jstr = (*env)->NewStringUTF(env, "wave recognize error");
    (*env)->CallVoidMethod(env, obj, jmethod, jstr);
    (*env)->DeleteLocalRef(env, jstr);
    return;
  }
}

static void output_result(Recog *recog, void *dummy) {
  WORD_INFO *winfo;
  WORD_ID *seq;
  int seqnum;
  int n,i;
  Sentence *s;
  RecogProcess *r;

  jbyteArray jbarray;
  int len = 0;

  char *p;
  char result[1024];

  result[0]='\0';

#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "output_result start");
#endif
  for(r=recog->process_list;r;r=r->next) {
    if (! r->live) continue;
    if (r->result.status < 0) continue;
    winfo = r->lm->winfo;
    for(n = 0; n < r->result.sentnum; n++) {
      s = &(r->result.sent[n]);
      seq = s->word;
      seqnum = s->word_num;
      for(i=0; i<seqnum; i++) {
        char *c = winfo->woutput[seq[i]];
        strcat(result, c);
      }
    }
  }
#ifdef ANDROID_DEBUG
  for(p=result; *p; p++) {
    __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "%#x", *p);
  }
#endif

  len = strlen(result);
  jbarray = (*genv)->NewByteArray(genv, len);
  (*genv)->SetByteArrayRegion(genv, jbarray, 0, len, (jbyte*)result);

  jclass jcls = (*genv)->GetObjectClass(genv, *gobj);
  jmethodID jmethod = (*genv)->GetMethodID(genv, jcls, "callback", "([B)V");
  (*genv)->CallVoidMethod(genv, *gobj, jmethod, jbarray);
  (*genv)->DeleteLocalRef(genv, jbarray);
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "output_result end");
#endif
}

JNIEXPORT void JNICALL Java_jp_co_tis_stc_julius_JuliusActivity_terminateJulius
  (JNIEnv *env, jobject obj)
{
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "terminateJulius start");
#endif
  j_close_stream(recog);
//  j_recog_free(recog);
#ifdef ANDROID_DEBUG
  __android_log_print(ANDROID_LOG_DEBUG, "Julius interface.c", "terminateJulius end");
#endif
}
