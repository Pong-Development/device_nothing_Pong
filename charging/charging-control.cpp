#define LOG_TAG "nt-charging-control"

#include <algorithm>
#include <android-base/parseint.h>
#include <android-base/properties.h>
#include <android-base/strings.h>
#include <cerrno>
#include <chrono>
#include <cstdio>
#include <dirent.h>
#include <fcntl.h>
#include <fstream>
#include <log/log.h>
#include <time.h>
#include <unistd.h>

#include "charging-control.h"
#include "charging-policy.h"

namespace {
constexpr char kThermalSysfs[] = "/sys/class/thermal/";
constexpr char kUsbOnline[] = "/sys/class/power_supply/usb/online";
constexpr char kUsbType[] = "/sys/class/power_supply/usb/usb_type";
constexpr char kWirelessOnline[] = "/sys/class/power_supply/wireless/online";
constexpr char kScenarioFcc[] = "/sys/class/qcom-battery/scenario_fcc";
constexpr char kEnabledProperty[] = "persist.sys.nt_chg.enabled";
constexpr char kProfileProperty[] = "persist.sys.nt_chg.profile";
constexpr auto kPollInterval = std::chrono::milliseconds(500);
constexpr auto kHealthInfoTimeout = std::chrono::minutes(2);

std::chrono::seconds bootTime() {
    timespec time{};
    if (clock_gettime(CLOCK_BOOTTIME, &time) != 0)
        return std::chrono::seconds::zero();
    return std::chrono::seconds(time.tv_sec);
}
}

ChargeStatusListener::ChargeStatusListener() : mWorker(&ChargeStatusListener::limitCharge, this) {}

ChargeStatusListener::~ChargeStatusListener() {
    {
        std::lock_guard<std::mutex> lock(mMutex);
        mStop = true;
    }
    mWake.notify_one();
    mWorker.join();
}

std::string ChargeStatusListener::read_line(const char *file) {
    std::ifstream input(file);
    std::string value;
    std::getline(input, value);
    return value;
}

bool ChargeStatusListener::read_int(const char *file, int *value) {
    return android::base::ParseInt(android::base::Trim(read_line(file)), value);
}

int ChargeStatusListener::write_value(const char *file, int value) {
    const std::string text = std::to_string(value);
    const int fd = TEMP_FAILURE_RETRY(open(file, O_WRONLY | O_CLOEXEC));
    if (fd < 0) {
        ALOGE("open %s failed, errno = %d", file, errno);
        return -errno;
    }
    const ssize_t written = TEMP_FAILURE_RETRY(write(fd, text.c_str(), text.size()));
    const int result =
        written < 0 ? -errno : (static_cast<size_t>(written) == text.size() ? 0 : -EIO);
    close(fd);
    if (result != 0)
        ALOGE("write %s failed: %d", file, result);
    return result;
}

int ChargeStatusListener::get_tzn(const std::string &sensor_name) {
    const auto cached = mThermalZones.find(sensor_name);
    if (cached != mThermalZones.end())
        return cached->second;

    DIR *directory = opendir(kThermalSysfs);
    if (!directory)
        return -1;
    int zone = -1;
    while (const auto *entry = readdir(directory)) {
        int candidate;
        char extra;
        if (sscanf(entry->d_name, "thermal_zone%d%c", &candidate, &extra) != 1)
            continue;
        const std::string path = std::string(kThermalSysfs) + entry->d_name + "/type";
        if (android::base::Trim(read_line(path.c_str())) == sensor_name) {
            zone = candidate;
            mThermalZones[sensor_name] = zone;
            break;
        }
    }
    closedir(directory);
    return zone;
}

std::optional<int> ChargeStatusListener::read_temperature(const std::string &sensor_name) {
    const int zone = get_tzn(sensor_name);
    if (zone < 0)
        return std::nullopt;
    const std::string path =
        std::string(kThermalSysfs) + "thermal_zone" + std::to_string(zone) + "/temp";
    int temperature;
    if (!read_int(path.c_str(), &temperature))
        return std::nullopt;
    return temperature;
}

int ChargeStatusListener::get_charge_temp() {
    const int battery = read_temperature("battery").value_or(mBatteryTemperature);
    if (battery <= 0)
        return 0;

    if (mUsbChgActive)
        return std::max(battery, read_temperature("shell_front").value_or(0));
    if (mWlsChgActive) {
        return std::max({battery, read_temperature("shell_back").value_or(0),
                         read_temperature("wls-therm").value_or(0)});
    }
    return battery;
}

void ChargeStatusListener::limitCharge() {
    charging::TemperaturePolicy policy;
    while (true) {
        HealthInfo info;
        std::chrono::seconds updated;
        {
            std::unique_lock<std::mutex> lock(mMutex);
            mWake.wait(lock, [this] { return mStop || mHasHealthInfo; });
            if (mStop)
                break;
            info = mHealthInfo;
            updated = mHealthInfoUpdated;
        }

        int online;
        mUsbChgActive = read_int(kUsbOnline, &online)
                            ? online != 0
                            : info.chargerUsbOnline || info.chargerAcOnline;
        mWlsChgActive =
            read_int(kWirelessOnline, &online) ? online != 0 : info.chargerWirelessOnline;
        const int tenths = info.batteryTemperatureTenthsCelsius;
        const auto age = bootTime() - updated;
        const bool fresh = updated > std::chrono::seconds::zero() &&
                           age >= std::chrono::seconds::zero() && age <= kHealthInfoTimeout;
        mBatteryTemperature = fresh && tenths > 0 && tenths <= 1500 ? tenths * 100 : 0;

        const bool enabled = android::base::GetBoolProperty(kEnabledProperty, true);
        const auto profile =
            charging::parseProfile(android::base::GetProperty(kProfileProperty, "mid"));
        int temperature = 0;
        int current = charging::kUnrestrictedCurrentMa;
        if (mUsbChgActive || mWlsChgActive) {
            auto charger = charging::Charger::Wireless;
            if (mUsbChgActive) {
                charger = read_line(kUsbType).find("[PD_PPS]") != std::string::npos
                              ? charging::Charger::Pps
                              : charging::Charger::Usb;
            }
            temperature = get_charge_temp();
            current = policy.update(temperature, charger, profile, enabled);
        }

        int previous;
        if (!read_int(kScenarioFcc, &previous) || previous != current) {
            if (write_value(kScenarioFcc, current) == 0) {
                ALOGD("Temperature: %d mC, charging cap: %d mA, limiter enabled: %d", temperature,
                      current, enabled);
            }
        }

        std::unique_lock<std::mutex> lock(mMutex);
        mWake.wait_for(lock, kPollInterval);
        if (mStop)
            break;
    }
}

ndk::ScopedAStatus ChargeStatusListener::healthInfoChanged(const HealthInfo &info) {
    {
        std::lock_guard<std::mutex> lock(mMutex);
        mHealthInfo = info;
        mHealthInfoUpdated = bootTime();
        mHasHealthInfo = true;
    }
    mWake.notify_one();
    return ndk::ScopedAStatus::ok();
}
