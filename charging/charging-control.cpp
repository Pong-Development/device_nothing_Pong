#define LOG_TAG "vendor.nt-charging-control"

#include <chrono>
#include <iterator>
#include <fstream>
#include <iostream>
#include <thread>
#include <log/log.h>
#include "charging-control.h"

#define USB_CHG_ONLINE "/sys/class/power_supply/usb/online"
#define USB_CHG_TYPE "/sys/class/power_supply/usb/usb_type"
#define BATTERY_SCENARIO_FCC "/sys/class/qcom-battery/scenario_fcc"
#define BATTERY_THERM_ZONE "/sys/class/thermal/thermal_zone96/temp"
#define WLS_CHG_ONLINE "/sys/class/power_supply/wireless/online"
#define WLS_THERM_ZONE "/sys/class/thermal/thermal_zone76/temp"

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

void ChargeStatusListener::limitCharge() {
    ALOGD("Start charging limit process");
    while (enableChgLimit) {
        int last_volt_limit = stoi(read_line(BATTERY_SCENARIO_FCC));
        int volt_limit_index = 0;
        int i, current_temp, current_volt_limit;

        if (usbChgActive) {
            int batteryTemp = stoi(read_line(BATTERY_THERM_ZONE));
            if (usingPps) {
                for (i = 0; i < size(ppsTempThresholds); i++) {
                    if (batteryTemp < ppsTempThresholds[i]) {
                        break;
                    } else if (batteryTemp >= ppsTempThresholds[i]) {
                        volt_limit_index = ppsVoltLimit[i];
                    }
                }
            } else {
                for (i = 0; i < size(qcTempThresholds); i++) {
                    if (batteryTemp < qcTempThresholds[i]) {
                        break;
                    } else if (batteryTemp >= qcTempThresholds[i]) {
                        volt_limit_index = qcVoltLimit[i];
                    }
                }
            }
            current_temp = batteryTemp;
        } else if (wlsChgActive) {
            int wlsTemp = stoi(read_line(WLS_THERM_ZONE));
            for (i = 0; i < size(wlsTempThresholds); i++) {
                if (wlsTemp < wlsTempThresholds[i]) {
                    break;
                } else if (wlsTemp >= wlsTempThresholds[i]) {
                    volt_limit_index = wlsVoltLimit[i];
                }
            }
            current_temp = wlsTemp;
        }

        current_volt_limit = CHG_VOLT_TBL[volt_limit_index];
        if (current_volt_limit != last_volt_limit) {
            ALOGD("Current temp: %d, limit charging to %dmA, last charging limit %dmA", 
                current_temp, current_volt_limit, last_volt_limit);
            write_value(BATTERY_SCENARIO_FCC, current_volt_limit);
        }

        this_thread::sleep_for(chrono::milliseconds(500));

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
        if (mStatus == BatteryStatus::CHARGING) {
            usbChgActive = stoi(read_line(USB_CHG_ONLINE));
            wlsChgActive = stoi(read_line(WLS_CHG_ONLINE));
            if (usbChgActive) {
                string usbChgType = read_line(USB_CHG_TYPE);
                if (usbChgType.find("[PD_PPS]") != string::npos) {
                    ALOGD("PPS charger connected");
                    usingPps = true;
                } else {
                    ALOGD("USB charger connected");
                    usingPps = false;
                }
            } else {
                usingPps = false;
            }
            if (wlsChgActive) {
                ALOGD("Wireless charger connected");
            }
            enableChgLimit = true;
            thread([=] {
                limitCharge();
            }).detach();
        } else {
            enableChgLimit = false;
        }
    }
    return ndk::ScopedAStatus::ok();
}
