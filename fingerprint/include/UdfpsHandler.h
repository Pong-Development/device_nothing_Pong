/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include <condition_variable>
#include <cstdint>
#include <functional>
#include <mutex>
#include <string>
#include <thread>

class UdfpsHandler {
public:
    UdfpsHandler(std::string readyPath, std::function<void(bool)> setFingerDown);
    ~UdfpsHandler();

    void onUiReady(bool waitForIllumination);
    void onFingerUp();

private:
    void run();

    const std::string mReadyPath;
    const std::function<void(bool)> mSetFingerDown;
    std::mutex mMutex;
    std::condition_variable mCondition;
    bool mStopped = false;
    bool mFingerDown = false;
    bool mPending = false;
    uint64_t mRequestId = 0;
    std::thread mThread;
};
