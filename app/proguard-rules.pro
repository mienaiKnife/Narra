#
# Copyright 2025 Narra Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Sherpa-ONNX JNI classes
-keep class com.k2fsa.sherpa.onnx.** { *; }

# Keep our stable callback and the Kotlin function interface it implements
-keep class com.mienaiknife.narra.tts.ondevice.SherpaTtsCallback { *; }
-keep interface kotlin.jvm.functions.Function1 { *; }

# Preserve line numbers and source file names for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
