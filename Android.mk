LOCAL_PATH := $(call my-dir)

ifneq ($(filter phone2,$(TARGET_DEVICE)),)
include $(call all-makefiles-under,$(LOCAL_PATH))

endif
