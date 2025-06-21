#
# Copyright (C) 2023 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from Pong device
$(call inherit-product, device/nothing/Pong/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/lineage/config/common_full_phone.mk)

PRODUCT_NAME := lineage_Pong
PRODUCT_DEVICE := Pong
PRODUCT_MANUFACTURER := Nothing
PRODUCT_BRAND := Nothing
PRODUCT_MODEL := A065

# Singing keys
$(call inherit-product, vendor/mine/keys.mk)

# Define rear camera specs
AXION_CAMERA_REAR_INFO := 50,50

# Blur
TARGET_ENABLE_BLUR := true

# Define front camera specs
AXION_CAMERA_FRONT_INFO := 32

# Maintainer name
AXION_MAINTAINER := GHOST_|_ゴースト

# Processor name
AXION_PROCESSOR := Snapdragon®_8+_Gen_1 

# Default core groups (if not overridden by the builder)
AXION_CPU_SMALL_CORES := 0,1,2,3
AXION_CPU_BIG_CORES := 4,5,6,7
AXION_CPU_UNLIMIT_UI := 0-7
AXION_CPU_BG := 0-2
AXION_CPU_FG := 0-7
AXION_CPU_LIMIT_BG := 0-1
AXION_CPU_LIMIT_UI := 0-4
AXION_DEBUGGING_ENABLED := false

PRODUCT_CHARACTERISTICS := nosdcard

PRODUCT_GMS_CLIENTID_BASE := android-nothing

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=Nothing/Pong/Pong:12/SKQ1.240903.001/2505061805:user/release-keys \
    DeviceName=Pong \
    DeviceProduct=Pong \
    SystemDevice=Pong \
    SystemName=Pong
