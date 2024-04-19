#!/system/bin/sh
#
# Copyright (C) 2024 The LineageOS Project
#
# SPDX-License-Identifier: Apache-2.0
#

service call phone 185 i32 0 i32 0

sleep 0.5

service call phone 185 i32 0 i32 1
