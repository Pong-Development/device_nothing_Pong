#define LOG_TAG "vendor.nt-charging-control.service"

#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <log/log.h>

#include "charging-control.h"

int main() {
    auto service_name = string() + IHealth::descriptor + "/default";

    shared_ptr<IHealth> mHealth = IHealth::fromBinder(ndk::SpAIBinder(
        AServiceManager_waitForService(service_name.c_str())));

    if (mHealth == NULL) {
        ALOGE("No health service found");
        return -1;
    } else {
        ALOGI("Got health service");
    }

    shared_ptr<ChargeStatusListener> chgStatusCallback = ndk::SharedRefBase::make<ChargeStatusListener>();
    mHealth->registerCallback(chgStatusCallback);

    // Keep the service running to continue receiving callbacks
    ABinderProcess_setThreadPoolMaxThreadCount(0);
    ABinderProcess_joinThreadPool();

    return 0;
}
