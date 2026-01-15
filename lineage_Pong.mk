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

PRODUCT_CHARACTERISTICS := nosdcard

PRODUCT_GMS_CLIENTID_BASE := android-nothing

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildFingerprint=Nothing/Pong/Pong:12/SKQ1.250415.001/2511191654:user/release-keys \
    DeviceName=Pong \
    DeviceProduct=Pong \
    SystemDevice=Pong \
    SystemName=Pong

#Evolution X Flags
TARGET_INCLUDE_ACCORD := true
TARGET_SUPPORT_BOOT_ANIMATIONS := true
BUILD_BCR := true
WITH_GMS := true

# Enforce Product Packages Existance.
TARGET_DISABLE_EPPE := true

#Blur
TARGET_ENABLE_BLUR := true

#Fingerprint
TARGET_HAS_UDFPS := true
