# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)

# Reusable Sensor test classes and helpers

include $(CLEAR_VARS)

LOCAL_MODULE := cts-sensors-tests
LOCAL_MODULE_TAGS := tests

# uncomment when b/13281332 is fixed
# please also uncomment the equivalent code in
# cts/apps/CtsVerifiers/Android.mk
#
# LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, src/android/hardware/cts/helpers)

include $(BUILD_STATIC_JAVA_LIBRARY)


# CtsHardwareTestCases package

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := tests

LOCAL_JAVA_LIBRARIES := android.test.runner

LOCAL_STATIC_JAVA_LIBRARIES := ctsdeviceutil ctstestrunner mockito-target android-ex-camera2

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := CtsHardwareTestCases

LOCAL_INSTRUMENTATION_FOR := CtsTestStubs

# uncomment when b/13281332 is fixed
# please also uncomment the equivalent code in
# cts/apps/CtsVerifiers/Android.mk
#
# LOCAL_SDK_VERSION := current
LOCAL_JAVA_LIBRARIES := android.test.runner

include $(BUILD_CTS_PACKAGE)
