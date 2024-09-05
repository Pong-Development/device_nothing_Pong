#pragma once

#include <aidl/android/hardware/health/IHealth.h>
#include <aidl/android/hardware/health/IHealthInfoCallback.h>
#include <aidl/android/hardware/health/BnHealthInfoCallback.h>
#include <string.h>

using aidl::android::hardware::health::BatteryStatus;
using aidl::android::hardware::health::HealthInfo;
using aidl::android::hardware::health::IHealthInfoCallback;
using aidl::android::hardware::health::IHealth;
using aidl::android::hardware::health::BnHealthInfoCallback;

using namespace std;

class ChargeStatusListener : public BnHealthInfoCallback {
public:
    BatteryStatus mStatus;
    bool usbChgActive;
    bool usingPps;
    bool wlsChgActive;
    bool enableChgLimit;
    ndk::ScopedAStatus healthInfoChanged(const HealthInfo& info) override;
    void limitCharge();
    int write_value(const char *file, const char *value);
    int write_value(const char *file, int value);
    string read_line(const char *file);
};
