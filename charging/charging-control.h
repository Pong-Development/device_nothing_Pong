#pragma once

#include <aidl/android/hardware/health/BnHealthInfoCallback.h>
#include <aidl/android/hardware/health/IHealth.h>
#include <chrono>
#include <condition_variable>
#include <map>
#include <mutex>
#include <optional>
#include <string>
#include <thread>

using aidl::android::hardware::health::BnHealthInfoCallback;
using aidl::android::hardware::health::HealthInfo;
using aidl::android::hardware::health::IHealth;

class ChargeStatusListener : public BnHealthInfoCallback {
  public:
    ChargeStatusListener();
    ~ChargeStatusListener() override;
    ndk::ScopedAStatus healthInfoChanged(const HealthInfo &info) override;

  private:
    void limitCharge();
    int write_value(const char *file, int value);
    std::string read_line(const char *file);
    bool read_int(const char *file, int *value);
    int get_tzn(const std::string &sensor_name);
    std::optional<int> read_temperature(const std::string &sensor_name);
    int get_charge_temp();

    std::mutex mMutex;
    std::condition_variable mWake;
    HealthInfo mHealthInfo{};
    std::chrono::seconds mHealthInfoUpdated{};
    bool mHasHealthInfo = false;
    bool mStop = false;

    bool mUsbChgActive = false;
    bool mWlsChgActive = false;
    int mBatteryTemperature = 0;
    std::map<std::string, int> mThermalZones;
    std::thread mWorker;
};
