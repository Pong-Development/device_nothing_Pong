#pragma once

#include <array>
#include <string_view>

namespace charging {

constexpr int kUnrestrictedCurrentMa = 9000;
constexpr int kStopTemperature = 48000;
constexpr int kResumeTemperature = 46000;

enum class Charger { Usb, Pps, Wireless };
enum class Profile { Slow, Mid, Fast };

inline Profile parseProfile(std::string_view value) {
    if (value == "slow")
        return Profile::Slow;
    if (value == "fast")
        return Profile::Fast;
    return Profile::Mid;
}

struct Step {
    int temperature;
    int currentMa;
};

constexpr std::array<Step, 5> kPpsMid = {
    {{35000, 7000}, {37000, 6000}, {39000, 4000}, {41000, 3000}, {43000, 2000}}};
constexpr std::array<Step, 4> kUsbMid = {
    {{35000, 5000}, {37000, 3000}, {39000, 2000}, {41000, 1600}}};
constexpr std::array<Step, 2> kWirelessMid = {{{35000, 2000}, {41000, 1000}}};

constexpr std::array<Step, 4> kPpsFast = {
    {{40000, 6000}, {41000, 4000}, {42000, 3000}, {43000, 2000}}};
constexpr std::array<Step, 3> kUsbFast = {{{40000, 3000}, {41000, 2000}, {43000, 1600}}};

template <std::size_t N>
inline int currentForSteps(int temperature, const std::array<Step, N> &steps, int offset = 0) {
    int current = kUnrestrictedCurrentMa;
    for (const auto &step : steps) {
        if (temperature < step.temperature + offset)
            break;
        current = step.currentMa;
    }
    return current;
}

inline int currentForProfile(int temperature, Charger charger, Profile profile) {
    const int offset = profile == Profile::Slow ? -5000 : 0;
    switch (charger) {
    case Charger::Pps:
        return profile == Profile::Fast ? currentForSteps(temperature, kPpsFast)
                                        : currentForSteps(temperature, kPpsMid, offset);
    case Charger::Usb:
        return profile == Profile::Fast ? currentForSteps(temperature, kUsbFast)
                                        : currentForSteps(temperature, kUsbMid, offset);
    case Charger::Wireless:
        return currentForSteps(temperature, kWirelessMid, offset);
    }
    return kUnrestrictedCurrentMa;
}

class TemperaturePolicy {
  public:
    int update(int temperature, Charger charger, Profile profile, bool enabled) {
        if (temperature <= 0 || temperature >= kStopTemperature) {
            mPaused = true;
            return 0;
        }
        if (mPaused && temperature >= kResumeTemperature)
            return 0;
        mPaused = false;
        return enabled ? currentForProfile(temperature, charger, profile) : kUnrestrictedCurrentMa;
    }

  private:
    bool mPaused = false;
};

}
