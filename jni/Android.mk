LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := julius_arm
LOCAL_SRC_FILES := interface.c
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_STATIC_LIBRARIES := julius sent
LOCAL_CFLAGS    := -DANDROID_DEBUG
LOCAL_LDLIBS    := -lc -lz -lgcc -llog -L$(LOCAL_PATH)/lib -ljulius -lsent

include $(BUILD_SHARED_LIBRARY)
