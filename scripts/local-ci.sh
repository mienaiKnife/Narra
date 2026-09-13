#!/bin/bash

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

set -e

# Default settings
FAST_MODE=false
LOW_MEM_MODE=false
SKIP_PAPARAZZI=false
GRADLE_OPTS=""
GRADLE_PROPS=""

function show_help {
    echo "Usage: ./scripts/local-ci.sh [options]"
    echo ""
    echo "Options:"
    echo "  --fast        Skip Lint and Paparazzi tests (only Spotless and Unit Tests)"
    echo "  --low-mem     Run with reduced memory footprint (no parallel, smaller daemon heap)"
    echo "  --no-paparazzi Skip screenshot tests but run everything else"
    echo "  --help        Show this help message"
}

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --fast) FAST_MODE=true ;;
        --low-mem) LOW_MEM_MODE=true ;;
        --no-paparazzi) SKIP_PAPARAZZI=true ;;
        --help) show_help; exit 0 ;;
        *) echo "Unknown parameter: $1"; show_help; exit 1 ;;
    esac
    shift
done

echo "Starting local CI checks..."

if [ "$LOW_MEM_MODE" = true ]; then
    echo "Mode: Low Memory (Parallelism disabled, smaller heap)"
    GRADLE_OPTS="-Dorg.gradle.parallel=false -Dorg.gradle.workers.max=1"
    GRADLE_PROPS="-Porg.gradle.jvmargs=-Xmx2048m"
fi

# 1. Spotless
echo "Step 1/3: Running Spotless Check..."
./gradlew $GRADLE_OPTS $GRADLE_PROPS spotlessCheck

# 2. Lint (skip in fast mode)
if [ "$FAST_MODE" = false ]; then
    echo "Step 2/3: Running Lint..."
    ./gradlew $GRADLE_OPTS $GRADLE_PROPS lintDebug
else
    echo "Step 2/3: Skipping Lint (Fast Mode)"
fi

# 3. Unit Tests
echo "Step 3/3: Running Unit Tests..."
UNIT_TEST_PROPS=""
if [ "$FAST_MODE" = true ] || [ "$SKIP_PAPARAZZI" = true ]; then
    echo "Excluding Paparazzi screenshot tests..."
    UNIT_TEST_PROPS="-PskipPaparazzi"
fi

./gradlew $GRADLE_OPTS $GRADLE_PROPS $UNIT_TEST_PROPS testDebugUnitTest

echo ""
echo "SUCCESS: All local CI checks passed!"
