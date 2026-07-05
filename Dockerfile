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

# Builds a Narra APK without Android Studio.
# Usage:
#   docker build --output type=local,dest=./out .
# or, for a debug APK:
#   docker build --build-arg BUILD_TARGET=assembleDebug --output type=local,dest=./out .

FROM eclipse-temurin:17-jdk

# Pin the versions required by the project (see app/build.gradle.kts and gradle/libs.versions.toml).
ARG ANDROID_COMPILE_SDK=36
ARG ANDROID_BUILD_TOOLS=36.0.0
ARG ANDROID_COMMAND_LINE_TOOLS=11076708
ARG BUILD_TARGET=assembleDebug

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}

# Install base packages needed by the Android SDK and Gradle.
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        git \
        wget \
        unzip \
        file \
    && rm -rf /var/lib/apt/lists/*

# Download and install the Android command-line tools.
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_COMMAND_LINE_TOOLS}_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install the exact platform and build-tools versions.
RUN yes | sdkmanager --licenses && \
    sdkmanager --install \
        "platforms;android-${ANDROID_COMPILE_SDK}" \
        "build-tools;${ANDROID_BUILD_TOOLS}" \
        "platform-tools"

WORKDIR /workspace

# Copy the project source into the container.
COPY . .

# Build the APK. Mount a persistent Gradle cache so dependency downloads survive
# between runs even when source files change.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew ${BUILD_TARGET} --no-daemon

# Make the output APK easy to extract with `docker build --output`.
FROM scratch AS export
COPY --from=0 /workspace/app/build/outputs/apk/ /
