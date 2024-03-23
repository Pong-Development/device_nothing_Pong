LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := RemovePackages
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_TAGS := optional
LOCAL_OVERRIDES_PACKAGES := \
    Chrome \
    Chrome-Stub \
    Drive \
    PrebuiltGmail \
    GoogleCamera \
    Maps \
    AudioFX \
    Eleven \
    Etar \
    Aperture \
    Jelly \
    MusicFX \
    Music \
    Recorder \
    Seedvault \
    MyVerizonServices \
    GoogleTTS \
    arcore \
    Videos \
    MaestroPrebuilt \
    AndroidAutoStubPrebuilt \
    talkback \
    SoundAmplifierPrebuilt \
    DevicePolicyPrebuilt \
    FilesPrebuilt \
    obdm_stub \
    OemDmTrigger \
    OPScreenRecord \
    Ornament \
    SafetyHubPrebuilt \
    ScribePrebuilt \
    Showcase \
    Snap \
    MusicFX \
    SprintDM \
    SprintHM \
    VZWAPNLib \
    VzwOmaTrigger \
    YouTube \
    YouTubeMusicPrebuilt
LOCAL_UNINSTALLABLE_MODULE := true
LOCAL_CERTIFICATE := PRESIGNED
LOCAL_SRC_FILES := /dev/null
include $(BUILD_PREBUILT)
