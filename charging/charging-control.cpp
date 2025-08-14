#define LOG_TAG "nt-charging-control"

#include <fcntl.h>
#include <chrono>
#include <iterator>
#include <fstream>
#include <thread>
#include <dirent.h>
#include <log/log.h>
#include "charging-control.h"

#define MAX_PATH		(256)
#define THERMAL_SYSFS		"/sys/class/thermal/"
#define TZ_DIR_NAME		"thermal_zone"
#define TZ_DIR_FMT		"thermal_zone%d"
#define TZ_TYPE			"type"

#define USB_CHG_ONLINE "/sys/class/power_supply/usb/online"
#define USB_CHG_TYPE "/sys/class/power_supply/usb/usb_type"
#define BATTERY_SCENARIO_FCC "/sys/class/qcom-battery/scenario_fcc"
#define WLS_CHG_ONLINE "/sys/class/power_supply/wireless/online"

#define CHG_LIMIT_INTERVAL 500

int CHG_VOLT_TBL[] = { 9000, 8500, 8000, 7500, 7000, 6500,
                      6000, 5500, 5000, 4500, 4000, 3500,
                      3000, 2500, 2400, 2000, 1600, 1500,
                      1200, 1000, 500, 0 };
int ppsTempThresholds[] = { 35000, 37000, 39000, 41000, 43000, 48000 };
int ppsVoltLimit[] = { 4, 6, 10, 12, 15, 21 };
int qcTempThresholds[] = { 35000, 37000, 39000, 41000, 48000 };
int qcVoltLimit[] = { 8, 12, 15, 16, 21 };
int wlsTempThresholds[] = { 35000, 41000, 48000 };
int wlsVoltLimit[] = { 15, 19, 21 };

string ChargeStatusListener::read_line(const char *file) {
    ifstream input_file(file);
    string output;

    if (!input_file.is_open())
        ALOGE("open %s failed", file);

    getline(input_file, output);
    return output;
}

int ChargeStatusListener::write_value(const char *file, const char *value) {
    int fd;
    int ret;

    fd = TEMP_FAILURE_RETRY(open(file, O_WRONLY));
    if (fd < 0) {
        ALOGE("open %s failed, errno = %d", file, errno);
        return -errno;
    }

    ret = TEMP_FAILURE_RETRY(write(fd, value, strlen(value) + 1));
    if (ret == -1) {
        ret = -errno;
    } else {
        ret = 0;
    }

    errno = 0;
    close(fd);

    return ret;
}

int ChargeStatusListener::write_value(const char *file, int value) {
    return write_value(file, to_string(value).c_str());
}

int ChargeStatusListener::get_tzn(string sensor_name)
{
    DIR *tdir = NULL;
    struct dirent *tdirent = NULL;
    int found = -1;
    int tzn = 0;
    char name[MAX_PATH] = {0};

    tdir = opendir(THERMAL_SYSFS);
    if (!tdir) {
        ALOGE("get_tzn: Unable to open %s", THERMAL_SYSFS);
        return found;
    }

    while ((tdirent = readdir(tdir))) {
        if (strncmp(tdirent->d_name, TZ_DIR_NAME, strlen(TZ_DIR_NAME)) != 0)
            continue;

        snprintf(name, sizeof(name), "%s%s/%s",
                 THERMAL_SYSFS, tdirent->d_name, TZ_TYPE);

        string buf = read_line(name);
        if (buf.empty()) {
            ALOGE("get_tzn: sensor name read error for tz: %s", tdirent->d_name);
            continue;
        }

        if (strncmp(buf.c_str(), sensor_name.c_str(), sensor_name.length()) == 0) {
            found = 1;
            break;
        }
    }

    if (found == 1) {
        sscanf(tdirent->d_name, TZ_DIR_FMT, &tzn);
        found = tzn;
    }

    closedir(tdir);
    return found;
}

int ChargeStatusListener::get_charge_temp() {
    int tzn, temp;
    char path[MAX_PATH];

    if (usbChgActive) {
        tzn = get_tzn("shell_front");
        if (tzn >= 0) {
            snprintf(path, sizeof(path), "%sthermal_zone%d/temp", THERMAL_SYSFS, tzn);
            temp = stoi(read_line(path));
            if (temp > 0) return temp;
        }
        tzn = get_tzn("battery");
        snprintf(path, sizeof(path), "%sthermal_zone%d/temp", THERMAL_SYSFS, tzn);
        temp = stoi(read_line(path));
        return temp;
    }

    if (wlsChgActive) {
        tzn = get_tzn("shell_back");
        if (tzn >= 0) {
            snprintf(path, sizeof(path), "%sthermal_zone%d/temp", THERMAL_SYSFS, tzn);
            temp = stoi(read_line(path));
            if (temp > 0) return temp;
        }
        tzn = get_tzn("wls-therm");
        snprintf(path, sizeof(path), "%sthermal_zone%d/temp", THERMAL_SYSFS, tzn);
        temp = stoi(read_line(path));
        return temp;
    }

    return 0;
}

vector<int> ChargeStatusListener::get_charge_tempThresholds() {
    if (usbChgActive)
        return usingPps
            ? vector<int>(begin(ppsTempThresholds), end(ppsTempThresholds))
            : vector<int>(begin(qcTempThresholds), end(qcTempThresholds));
    if (wlsChgActive)
        return vector<int>(begin(wlsTempThresholds), end(wlsTempThresholds));
    return {};
}

vector<int> ChargeStatusListener::get_charge_voltLimits() {
    if (usbChgActive)
        return usingPps
            ? vector<int>(begin(ppsVoltLimit), end(ppsVoltLimit))
            : vector<int>(begin(qcVoltLimit), end(qcVoltLimit));
    if (wlsChgActive)
        return vector<int>(begin(wlsVoltLimit), end(wlsVoltLimit));
    return {};
}

void ChargeStatusListener::limitCharge() {
    ALOGD("Start charging limit process");
    while (enableChgLimit) {
        int last_volt_limit = stoi(read_line(BATTERY_SCENARIO_FCC));
        int volt_limit_index = 0;
        int current_temp = get_charge_temp();
        auto tempThresholds = get_charge_tempThresholds();
        auto voltLimits = get_charge_voltLimits();
        int i, current_volt_limit;

        for (i = 0; i < tempThresholds.size(); i++) {
            if (current_temp >= tempThresholds[i]) {
                volt_limit_index = voltLimits[i];
            } else {
                break;
            }
        }

        current_volt_limit = CHG_VOLT_TBL[volt_limit_index];
        if (current_volt_limit != last_volt_limit) {
            ALOGD("Current temp: %d, limit charging to %dmA, last charging limit %dmA", 
                current_temp, current_volt_limit, last_volt_limit);
            write_value(BATTERY_SCENARIO_FCC, current_volt_limit);
        }

        this_thread::sleep_for(chrono::milliseconds(CHG_LIMIT_INTERVAL));

        if (!enableChgLimit) {
            ALOGD("Stop charging limit process");
            break;
        }
    }
}

ndk::ScopedAStatus ChargeStatusListener::healthInfoChanged(const HealthInfo& info) {
    ALOGV("healthInfoChanged: %d", info.batteryStatus);
    if (info.batteryStatus != mStatus) {
        mStatus = info.batteryStatus;
        ALOGD("Battery status changed: %d", mStatus);
        usbChgActive = stoi(read_line(USB_CHG_ONLINE));
        wlsChgActive = stoi(read_line(WLS_CHG_ONLINE));
        if (mStatus == BatteryStatus::CHARGING) {
            if (usbChgActive) {
                string usbChgType = read_line(USB_CHG_TYPE);
                usingPps = (usbChgType.find("[PD_PPS]") != string::npos);
                ALOGD("%s charger connected", usingPps ? "PPS" : "USB");
            } else {
                usingPps = false;
            }
            if (wlsChgActive) {
                ALOGD("Wireless charger connected");
            }
            enableChgLimit = true;
            thread([this] {
                limitCharge();
            }).detach();
        } else {
            enableChgLimit = false;
            usingPps = false;
        }
    }
    return ndk::ScopedAStatus::ok();
}