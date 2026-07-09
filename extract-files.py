#!/usr/bin/env -S PYTHONPATH=../../../tools/extract-utils python3
#
# SPDX-FileCopyrightText: 2024 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

from extract_utils.fixups_blob import blob_fixup, blob_fixups_user_type
from extract_utils.fixups_lib import (
    lib_fixup_remove,
    lib_fixups,
    lib_fixups_user_type,
)
from extract_utils.main import ExtractUtils, ExtractUtilsModule

namespace_imports = [
    "hardware/qcom/display",
    "hardware/qcom/display/gralloc",
    "hardware/qcom/display/libdebug",
    "vendor/qcom/common/vendor/adreno/s",
    "vendor/qcom/common/vendor/display/5.10",
    "vendor/qcom/common/vendor/media/5.10",
    "vendor/qcom/common/vendor/perf",
    "vendor/qcom/common/vendor/wlan",
]


def lib_fixup_vendor_suffix(lib: str, partition: str, *args, **kwargs):
    return f'{lib}_{partition}' if partition == 'vendor' else None


def lib_fixup_prebuilt_suffix(lib: str, *args, **kwargs):
    return f'{lib}_prebuilt'


lib_fixups: lib_fixups_user_type = {
    **lib_fixups,
    'libgrpc++_unsecure': lib_fixup_prebuilt_suffix,
    (
        'com.qualcomm.qti.dpm.api@1.0',
        'com.qualcomm.qti.imscmservice@*',
        'com.qualcomm.qti.uceservice@*',
        'libmmosal',
        'vendor.qti.data.factory@*',
        'vendor.qti.data.mwqem@1.0',
        'vendor.qti.data.slm@1.0',
        'vendor.qti.diaghal@1.0',
        'vendor.qti.hardware.ListenSoundModel@1.0',
        'vendor.qti.hardware.data.cne.internal.*@*',
        'vendor.qti.hardware.data.connection@*',
        'vendor.qti.hardware.data.connectionfactory-V1-ndk_platform',
        'vendor.qti.hardware.data.dataactivity-V1-ndk_platform',
        'vendor.qti.hardware.data.dynamicdds@*',
        'vendor.qti.hardware.data.flow@1.0',
        'vendor.qti.hardware.data.iwlan@*',
        'vendor.qti.hardware.data.ka-V1-ndk_platform',
        'vendor.qti.hardware.data.latency@1.0',
        'vendor.qti.hardware.data.lce@1.0',
        'vendor.qti.hardware.data.qmi@1.0',
        'vendor.qti.hardware.dpmservice@*',
        'vendor.qti.hardware.embmssl@*',
        'vendor.qti.hardware.limits@*',
        'vendor.qti.hardware.mwqemadapter@1.0',
        'vendor.qti.hardware.qccsyshal@*',
        'vendor.qti.hardware.qccvndhal@1.0',
        'vendor.qti.hardware.radio.am@1.0',
        'vendor.qti.hardware.radio.atcmdfwd@1.0',
        'vendor.qti.hardware.radio.ims-V12-ndk_platform',
        'vendor.qti.hardware.radio.ims@*',
        'vendor.qti.hardware.radio.internal.deviceinfo@1.0',
        'vendor.qti.hardware.radio.lpa@*',
        'vendor.qti.hardware.radio.qcrilhook@1.0',
        'vendor.qti.hardware.radio.qtiradio-V8-ndk_platform',
        'vendor.qti.hardware.radio.qtiradio@*',
        'vendor.qti.hardware.radio.qtiradioconfig-V2-ndk_platform',
        'vendor.qti.hardware.radio.uim@*',
        'vendor.qti.hardware.radio.uim_remote_client@*',
        'vendor.qti.hardware.radio.uim_remote_server@1.0',
        'vendor.qti.hardware.slmadapter@1.0',
        'vendor.qti.hardware.wifidisplaysession@1.0',
        'vendor.qti.ims.callcapability@1.0',
        'vendor.qti.ims.callinfo@1.0',
        'vendor.qti.ims.configservice@*',
        'vendor.qti.ims.connection@1.0',
        'vendor.qti.ims.factory@*',
        'vendor.qti.ims.rcsconfig@*',
        'vendor.qti.ims.rcssip@*',
        'vendor.qti.ims.rcsuce@*',
        'vendor.qti.imsrtpservice@3.0',
        'vendor.qti.latency@*',
    ): lib_fixup_vendor_suffix,
    (
        'libwpa_client',
    ): lib_fixup_remove,
}

blob_fixups: blob_fixups_user_type = {
    'vendor/bin/hw/android.hardware.power.stats-service': blob_fixup()
        .replace_needed('android.hardware.power.stats-V1-ndk_platform.so', 'android.hardware.power.stats-V1-ndk.so'),
    ('vendor/bin/hw/android.hardware.security.keymint-service-qti', 'vendor/lib64/libqtikeymint.so'): blob_fixup()
        .replace_needed('android.hardware.security.keymint-V1-ndk_platform.so', 'android.hardware.security.keymint-V1-ndk.so')
        .replace_needed('android.hardware.security.secureclock-V1-ndk_platform.so', 'android.hardware.security.secureclock-V1-ndk.so')
        .replace_needed('android.hardware.security.sharedsecret-V1-ndk_platform.so', 'android.hardware.security.sharedsecret-V1-ndk.so')
        .add_needed('android.hardware.security.rkp-V1-ndk.so'),
    ('vendor/bin/hw/android.hardware.identity-service-qti', 'vendor/lib64/libqtiidentitycredential.so'): blob_fixup()
        .replace_needed('android.hardware.identity-V3-ndk_platform.so', 'android.hardware.identity-V3-ndk.so')
        .replace_needed('android.hardware.keymaster-V3-ndk_platform.so', 'android.hardware.keymaster-V3-ndk.so'),
    ('vendor/bin/hw/vendor.qti.hardware.vibrator.service', 'vendor/lib64/vendor.qti.hardware.vibrator.impl.so'): blob_fixup()
        .replace_needed('android.hardware.vibrator-V2-ndk_platform.so', 'android.hardware.vibrator-V2-ndk.so'),
    ('vendor/etc/media_codecs.xml', 'vendor/etc/media_codecs_cape.xml', 'vendor/etc/media_codecs_cape_vendor.xml'): blob_fixup()
        .regex_replace('.*media_codecs_(google_audio|google_c2|google_telephony|vendor_audio).*\n', ''),
    'vendor/lib64/libcamximageformatutils.so': blob_fixup()
        .replace_needed('vendor.qti.hardware.display.config-V2-ndk_platform.so', 'vendor.qti.hardware.display.config-V2-ndk.so'),
    ('vendor/lib64/libgarden.so', 'vendor/lib64/libgarden_haltests_e2e.so'): blob_fixup()
        .replace_needed('android.hardware.gnss-V1-ndk_platform.so', 'android.hardware.gnss-V1-ndk.so'),
    'vendor/lib64/libmorpho_video_stabilizer.so': blob_fixup()
        .add_needed('libutils.so'),
    ('vendor/lib64/libntcamallocator.so', 'vendor/lib64/vendor.noth.hardware.camera-service-impl.so'): blob_fixup()
        .add_needed('libui_shim.so'),
    'vendor/lib64/libwvhidl.so': blob_fixup()
        .add_needed('libcrypto_shim.so'),
    'vendor/lib64/nfc_nci_nxp_snxxx.so': blob_fixup()
        .add_needed('libbase_shim.so'),
    'vendor/lib64/vendor.libdpmframework.so': blob_fixup()
        .add_needed('libhidlbase_shim.so'),
}  # fmt: skip

module = ExtractUtilsModule(
    'phone2',
    'nothing',
    blob_fixups=blob_fixups,
    lib_fixups=lib_fixups,
    namespace_imports=namespace_imports,
)

if __name__ == '__main__':
    utils = ExtractUtils.device(module)
    utils.run()
