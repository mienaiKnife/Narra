#!/usr/bin/env bash
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

set -euo pipefail

BUILD_TARGET="${1:-assembleDebug}"
OUTPUT_DIR="${2:-./out}"
IMAGE_TAG="narra-build:latest"

echo "Building Narra APK target: ${BUILD_TARGET}"

docker build \
    --build-arg BUILD_TARGET="${BUILD_TARGET}" \
    --tag "${IMAGE_TAG}" \
    --target export \
    --output "type=local,dest=${OUTPUT_DIR}" \
    .

echo "Done. APK output directory: ${OUTPUT_DIR}"
