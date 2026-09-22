/*
 * Copyright (C) 2026 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include "UdfpsHandler.h"

#include <log/log.h>

#include <chrono>
#include <fstream>
#include <utility>

using namespace std::chrono_literals;

UdfpsHandler::UdfpsHandler(std::string readyPath, std::function<void(bool)> setFingerDown)
    : mReadyPath(std::move(readyPath)),
      mSetFingerDown(std::move(setFingerDown)),
      mThread(&UdfpsHandler::run, this) {}

UdfpsHandler::~UdfpsHandler() {
    {
        std::lock_guard<std::mutex> lock(mMutex);
        mStopped = true;
        mCondition.notify_all();
    }
    mThread.join();
}

void UdfpsHandler::onUiReady(bool waitForIllumination) {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mFingerDown) return;

    mFingerDown = true;
    ++mRequestId;
    if (waitForIllumination) {
        mPending = true;
        mCondition.notify_all();
    } else {
        mSetFingerDown(true);
    }
}

void UdfpsHandler::onFingerUp() {
    std::lock_guard<std::mutex> lock(mMutex);
    mFingerDown = false;
    mPending = false;
    ++mRequestId;
    mCondition.notify_all();
    mSetFingerDown(false);
}

void UdfpsHandler::run() {
    std::unique_lock<std::mutex> lock(mMutex);
    while (!mStopped) {
        mCondition.wait(lock, [this] { return mStopped || mPending; });
        if (mStopped) break;

        const auto deadline = std::chrono::steady_clock::now() + 500ms;
        const auto requestId = mRequestId;
        while (!mStopped && mPending && requestId == mRequestId) {
            int ready = 0;
            std::ifstream state(mReadyPath);
            if (!(state >> ready)) {
                ALOGE("Unable to read FOD illumination state from %s", mReadyPath.c_str());
                mPending = false;
                break;
            }

            if (ready == 1) {
                mPending = false;
                mSetFingerDown(true);
                break;
            }

            if (std::chrono::steady_clock::now() >= deadline) {
                ALOGW("Timed out waiting for FOD illumination");
                mPending = false;
                break;
            }

            mCondition.wait_for(lock, 10ms);
        }
    }
}
