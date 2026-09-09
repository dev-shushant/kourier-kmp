#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Kourier SDK — Multiplatform Publishing & Distribution Pipeline
#
# Target Distribution Repository: https://github.com/dev-shushant/kourier.git
#
# Capabilities:
#   1. Zero-Credential Android Maven Repository (hosted on raw.githubusercontent.com)
#   2. GitHub Packages Maven Registry (maven.pkg.github.com/dev-shushant/kourier)
#   3. iOS Swift Package Manager (Package.swift + KourierIos.xcframework.zip release asset)
#   4. Preserve independently maintained consumer documentation in the distribution repo
#   5. Automated version bumping via version.properties
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

VERSION_PROPS="${ROOT_DIR}/version.properties"
DIST_REPO_URL="https://github.com/dev-shushant/kourier.git"

# Colors for terminal output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}${BOLD}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}${BOLD}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}${BOLD}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}${BOLD}[ERROR]${NC} $1"
}

# CLI Flags
NEW_VERSION=""
DRY_RUN=false
SKIP_TESTS=false
SKIP_REMOTE_PUSH=false
SKIP_SAMPLES=true
GRADLE_WORKERS=""

print_usage() {
    echo -e "${BOLD}Usage:${NC} ./scripts/publish_optimized.sh [OPTIONS]"
    echo ""
    echo -e "${BOLD}Options:${NC}"
    echo "  --version <X.Y.Z>     Bump and set SDK version before publishing (e.g. 1.0.0)"
    echo "  --dry-run             Build, package, and generate artifacts locally without remote push"
    echo "  --skip-tests          Skip running unit and compilation tests"
    echo "  --skip-samples        Skip building sample-android module (default: true)"
    echo "  --include-samples     Build sample-android module as well"
    echo "  --no-push             Generate distribution artifacts locally without remote publication"
    echo "  --workers <N>         Gradle worker count (default: all logical CPU cores)"
    echo "  -h, --help            Show this help message"
    echo ""
}

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --version)
            NEW_VERSION="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --skip-samples)
            SKIP_SAMPLES=true
            shift
            ;;
        --include-samples)
            SKIP_SAMPLES=false
            shift
            ;;
        --no-push)
            SKIP_REMOTE_PUSH=true
            shift
            ;;
        --workers)
            GRADLE_WORKERS="$2"
            shift 2
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown argument: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Detect logical CPU cores for Gradle parallelism.
if [[ -z "${GRADLE_WORKERS}" ]]; then
    if command -v sysctl &> /dev/null; then
        GRADLE_WORKERS="$(sysctl -n hw.logicalcpu 2>/dev/null || true)"
    fi
    if [[ -z "${GRADLE_WORKERS}" ]] && command -v nproc &> /dev/null; then
        GRADLE_WORKERS="$(nproc)"
    fi
    if [[ -z "${GRADLE_WORKERS}" ]] && command -v getconf &> /dev/null; then
        GRADLE_WORKERS="$(getconf _NPROCESSORS_ONLN 2>/dev/null || true)"
    fi
    GRADLE_WORKERS="${GRADLE_WORKERS:-4}"
fi

if ! [[ "${GRADLE_WORKERS}" =~ ^[1-9][0-9]*$ ]]; then
    log_error "--workers must be a positive integer (received: ${GRADLE_WORKERS})"
    exit 1
fi

# Keep Gradle in one daemon and allow independent project tasks to use all cores.
GRADLE_ARGS=(
    --parallel
    "--max-workers=${GRADLE_WORKERS}"
    --build-cache
    --daemon
)

# Configuration cache can reduce repeat release times further, but not every
# Kotlin Multiplatform/plugin setup is compatible. Enable explicitly once verified:
#   KOURIER_CONFIGURATION_CACHE=true ./scripts/publish_optimized.sh ...
if [[ "${KOURIER_CONFIGURATION_CACHE:-false}" == "true" ]]; then
    GRADLE_ARGS+=(--configuration-cache)
fi

# Add sample-android exclusion if requested
if [[ "${SKIP_SAMPLES}" == true ]]; then
    GRADLE_ARGS+=("-PskipSamples")
fi

PIPELINE_START_SECONDS=${SECONDS}

cd "${ROOT_DIR}"

# 1. Version extraction and optional update
if [[ ! -f "${VERSION_PROPS}" ]]; then
    log_error "version.properties not found at ${VERSION_PROPS}"
    exit 1
fi

if [[ -n "${NEW_VERSION}" ]]; then
    log_info "Updating SDK version to: ${BOLD}${NEW_VERSION}${NC}"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/^VERSION_NAME=.*/VERSION_NAME=${NEW_VERSION}/" "${VERSION_PROPS}"
    else
        sed -i "s/^VERSION_NAME=.*/VERSION_NAME=${NEW_VERSION}/" "${VERSION_PROPS}"
    fi
fi

VERSION_NAME=$(grep "^VERSION_NAME=" "${VERSION_PROPS}" | cut -d'=' -f2 | tr -d '[:space:]')
GROUP=$(grep "^GROUP=" "${VERSION_PROPS}" | cut -d'=' -f2 | tr -d '[:space:]')
GITHUB_REPO=$(grep "^GITHUB_REPO=" "${VERSION_PROPS}" | cut -d'=' -f2 | tr -d '[:space:]')
DIST_REPO_URL="https://github.com/${GITHUB_REPO}.git"

# Automatic GitHub Token Resolution (prefer DIST_REPO_TOKEN or GH_PAT for cross-repo push)
PUBLISH_AUTH_TOKEN="${DIST_REPO_TOKEN:-${GH_PAT:-${GITHUB_TOKEN:-}}}"
if [[ -z "${PUBLISH_AUTH_TOKEN}" ]] && command -v gh &> /dev/null; then
    PUBLISH_AUTH_TOKEN=$(gh auth token 2>/dev/null || echo "")
fi

if [[ -n "${PUBLISH_AUTH_TOKEN}" ]]; then
    export GH_TOKEN="${PUBLISH_AUTH_TOKEN}"
    export GITHUB_TOKEN="${PUBLISH_AUTH_TOKEN}"
    AUTH_DIST_REPO_URL="https://x-access-token:${PUBLISH_AUTH_TOKEN}@github.com/${GITHUB_REPO}.git"
else
    AUTH_DIST_REPO_URL="${DIST_REPO_URL}"
fi

# Ensure git credentials and author identity are set for automated commits
if ! git config --get user.name >/dev/null 2>&1; then
    git config --global user.name "${GITHUB_ACTOR:-github-actions[bot]}"
fi
if ! git config --get user.email >/dev/null 2>&1; then
    git config --global user.email "${GITHUB_ACTOR:-github-actions[bot]}@users.noreply.github.com"
fi

if [[ -z "${GITHUB_ACTOR:-}" ]]; then
    export GITHUB_ACTOR="dev-shushant"
fi

echo -e "${CYAN}${BOLD}================================================================${NC}"
echo -e "${CYAN}${BOLD}  KOURIER SDK RELEASE PIPELINE                                  ${NC}"
echo -e "${CYAN}${BOLD}================================================================${NC}"
echo -e "  SDK Version:         ${BOLD}${VERSION_NAME}${NC}"
echo -e "  Group ID:            ${BOLD}${GROUP}${NC}"
echo -e "  Distribution Repo:   ${BOLD}https://github.com/${GITHUB_REPO}${NC}"
echo -e "  Zero-Cred Maven URL: ${BOLD}https://raw.githubusercontent.com/${GITHUB_REPO}/mvn-repo${NC}"
echo -e "  SPM Target:          ${BOLD}https://github.com/${GITHUB_REPO}${NC}"
echo -e "  Dry Run:             ${BOLD}${DRY_RUN}${NC}"
echo -e "  Skip Samples:        ${BOLD}${SKIP_SAMPLES}${NC}"
echo -e "  Gradle Workers:      ${BOLD}${GRADLE_WORKERS}${NC}"
echo -e "${CYAN}${BOLD}================================================================${NC}\n"

# Fail early if attempting remote push without credentials for external repo
if [[ "${DRY_RUN}" == false && "${SKIP_REMOTE_PUSH}" == false ]]; then
    if [[ -z "${PUBLISH_AUTH_TOKEN}" ]]; then
        log_error "Cannot push to external distribution repository '${GITHUB_REPO}' without authentication."
        log_error "Please set DIST_REPO_TOKEN, GH_PAT, or GITHUB_TOKEN with write access to '${GITHUB_REPO}'."
        exit 1
    fi
fi

# 2-4. Build/verify local artifacts in ONE Gradle invocation.
#
# Use assembleKourierIosReleaseXCFramework instead of assembleKourierIosXCFramework
# so we only compile & link release binaries (saving ~10-15m on CI).
GRADLE_TASKS=(
    publishAllPublicationsToDistributionRepoRepository
    :kourier-ios:assembleKourierIosReleaseXCFramework
)

if [[ "${SKIP_TESTS}" == false ]]; then
    GRADLE_TASKS=(
        test
        "${GRADLE_TASKS[@]}"
    )
    log_info "Running verification + Android Maven build + iOS Release XCFramework in parallel (${GRADLE_WORKERS} workers)..."
else
    log_warn "Skipping tests (--skip-tests active)"
    log_info "Building Android Maven artifacts + iOS Release XCFramework in parallel (${GRADLE_WORKERS} workers)..."
fi

./gradlew "${GRADLE_ARGS[@]}" "${GRADLE_TASKS[@]}"
log_success "Local verification/build tasks completed."

# GitHub Packages is kept as a separate, non-fatal network publication.
# Most compile/package tasks should now be UP-TO-DATE from the build above.
if [[ -n "${GITHUB_TOKEN:-}" && "${DRY_RUN}" == false && "${SKIP_REMOTE_PUSH}" == false ]]; then
    log_info "Publishing Maven packages to GitHub Packages registry (${GITHUB_REPO})..."
    if ! ./gradlew "${GRADLE_ARGS[@]}" publishAllPublicationsToGitHubPackagesRepository \
        -PGITHUB_TOKEN="${GITHUB_TOKEN}" \
        -PGITHUB_ACTOR="${GITHUB_ACTOR}" \
        -PGITHUB_REPOSITORY="${GITHUB_REPO}"; then
        log_warn "GitHub Packages upload failed (non-fatal, zero-cred mvn-repo branch will be used)"
    fi
fi

XCF_DIR="${ROOT_DIR}/kourier-ios/build/XCFrameworks/release"
ZIP_PATH="${XCF_DIR}/KourierIos.xcframework.zip"

log_info "Compressing KourierIos.xcframework into zip archive..."
(
    cd "${XCF_DIR}"
    rm -f KourierIos.xcframework.zip

    # `zip` is single-threaded. Prefer 7-Zip when available because it can
    # compress ZIP archives using multiple CPU threads.
    if command -v 7zz &> /dev/null; then
        7zz a -tzip -mx=5 "-mmt=${GRADLE_WORKERS}" \
            KourierIos.xcframework.zip KourierIos.xcframework >/dev/null
    elif command -v 7z &> /dev/null; then
        7z a -tzip -mx=5 "-mmt=${GRADLE_WORKERS}" \
            KourierIos.xcframework.zip KourierIos.xcframework >/dev/null
    else
        log_warn "7-Zip not found; falling back to single-threaded zip"
        zip -r -X -q KourierIos.xcframework.zip KourierIos.xcframework
    fi
)

# Compute SHA256 Checksum
log_info "Computing SHA256 checksum for KourierIos.xcframework.zip..."
if command -v shasum &> /dev/null; then
    CHECKSUM=$(shasum -a 256 "${ZIP_PATH}" | awk '{print $1}')
elif command -v sha256sum &> /dev/null; then
    CHECKSUM=$(sha256sum "${ZIP_PATH}" | awk '{print $1}')
else
    CHECKSUM=$(xcrun swift package compute-checksum "${ZIP_PATH}")
fi
log_success "Computed Checksum: ${BOLD}${CHECKSUM}${NC}"

# 5. Prepare Staging Distribution Directory for dev-shushant/kourier
DIST_STAGE_DIR="${ROOT_DIR}/build/dist-repo-staging"
rm -rf "${DIST_STAGE_DIR}"
mkdir -p "${DIST_STAGE_DIR}"

log_info "Preparing distribution package in ${DIST_STAGE_DIR}..."

# Generate .gitignore
cat << 'EOF' > "${DIST_STAGE_DIR}/.gitignore"
.DS_Store
*.swp
*~
.build/
.swiftpm/
EOF

# Generate Package.swift
cat <<EOF > "${DIST_STAGE_DIR}/Package.swift"
// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "Kourier",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "Kourier",
            targets: ["KourierSwift", "KourierIos"]
        ),
        .library(
            name: "KourierIos",
            targets: ["KourierIos"]
        ),
    ],
    targets: [
        .target(
            name: "KourierSwift",
            dependencies: [
                "KourierIos"
            ],
            path: "Sources/KourierSwift",
            linkerSettings: [
                .linkedLibrary("sqlite3"),
                .linkedLibrary("c++")
            ]
        ),
        .binaryTarget(
            name: "KourierIos",
            url: "https://github.com/${GITHUB_REPO}/releases/download/v${VERSION_NAME}/KourierIos.xcframework.zip",
            checksum: "${CHECKSUM}"
        )
    ]
)
EOF

# Copy Sources for SPM if present
if [[ -d "${ROOT_DIR}/Sources" ]]; then
    rm -rf "${DIST_STAGE_DIR}/Sources"
    cp -R "${ROOT_DIR}/Sources" "${DIST_STAGE_DIR}/"
fi

# Also update root Package.swift
cp "${DIST_STAGE_DIR}/Package.swift" "${ROOT_DIR}/Package.swift"

# Generate LICENSE
cat << 'EOF' > "${DIST_STAGE_DIR}/LICENSE"
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

Copyright 2026 Shushant Tiwari.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
EOF

# README.md is maintained directly in the distribution repository.
# Do not generate it from the source README or rewrite its version examples.

# 6. Push to Remote Distribution Repository (dev-shushant/kourier)
if [[ "${DRY_RUN}" == false && "${SKIP_REMOTE_PUSH}" == false ]]; then
    log_info "Synchronizing distribution repository: ${DIST_REPO_URL}..."

    # Maven branch and SPM/main branch are independent, so publish them in
    # parallel. This overlaps Git/network time without racing on build outputs.
    push_maven_repo() {
        log_info "Updating zero-credential Maven artifacts on branch mvn-repo..."
        local mvn_stage_dir="${ROOT_DIR}/build/dist-mvn-git"
        rm -rf "${mvn_stage_dir}"

        # Preserve remote branch history instead of reinitializing + force-pushing
        # the entire Maven repository every release.
        if git -c http.extraheader= clone --depth 1 --branch mvn-repo --single-branch \
            "${AUTH_DIST_REPO_URL}" "${mvn_stage_dir}" 2>/dev/null; then
            (
                cd "${mvn_stage_dir}"
                git config --unset-all http.https://github.com/.extraheader 2>/dev/null || true
                git remote set-url origin "${AUTH_DIST_REPO_URL}"
            )
        else
            log_info "mvn-repo branch not found; initializing it..."
            mkdir -p "${mvn_stage_dir}"
            (
                cd "${mvn_stage_dir}"
                git config --unset-all http.https://github.com/.extraheader 2>/dev/null || true
                git init -b mvn-repo
                git remote add origin "${AUTH_DIST_REPO_URL}"
            )
        fi

        # Do NOT delete old Maven versions. Overlay only current/changed artifacts.
        if command -v rsync &> /dev/null; then
            rsync -a "${ROOT_DIR}/build/repo/" "${mvn_stage_dir}/"
        else
            cp -R "${ROOT_DIR}/build/repo/." "${mvn_stage_dir}/"
        fi

        (
            cd "${mvn_stage_dir}"
            git config --unset-all http.https://github.com/.extraheader 2>/dev/null || true
            git remote set-url origin "${AUTH_DIST_REPO_URL}" 2>/dev/null || true
            git add -A

            if git diff --cached --quiet; then
                log_info "No Maven repository changes to push."
                exit 0
            fi

            git commit -m "release: Kourier Android Maven artifacts v${VERSION_NAME}"
            git -c "pack.threads=${GRADLE_WORKERS}" \
                -c pack.compression=1 \
                -c http.extraheader= \
                push -u origin mvn-repo
        )
        log_success "Updated Maven artifacts on branch mvn-repo."
    }

    push_maven_repo &
    MVN_PUSH_PID=$!

    # 6.2 Push clean SPM layout to main branch
    CLONE_DIR="${ROOT_DIR}/build/dist-repo-git"
    rm -rf "${CLONE_DIR}"

    if ! git -c http.extraheader= clone --depth 1 --branch main --single-branch \
        "${AUTH_DIST_REPO_URL}" "${CLONE_DIR}" 2>/dev/null; then
        log_info "Initializing new distribution main branch..."
        mkdir -p "${CLONE_DIR}"
        (
            cd "${CLONE_DIR}"
            git config --unset-all http.https://github.com/.extraheader 2>/dev/null || true
            git init -b main
            git remote add origin "${AUTH_DIST_REPO_URL}"
        )
    fi

    (
        cd "${CLONE_DIR}"
        git config --unset-all http.https://github.com/.extraheader 2>/dev/null || true
        git remote set-url origin "${AUTH_DIST_REPO_URL}" 2>/dev/null || true

        # Sync contents (clean SPM structure ONLY — NO Maven cache folder in main).
        # Preserve README.md and its assets: consumer docs are maintained separately.
        rm -rf repo Package.swift LICENSE .gitignore Sources
        cp "${DIST_STAGE_DIR}/.gitignore" .
        cp "${DIST_STAGE_DIR}/Package.swift" .
        cp "${DIST_STAGE_DIR}/LICENSE" .
        if [[ -d "${DIST_STAGE_DIR}/Sources" ]]; then
            cp -R "${DIST_STAGE_DIR}/Sources" .
        fi

        git add -A

        if git diff --cached --quiet; then
            log_info "No changes detected in distribution repository main branch."
        else
            git commit -m "release: Kourier SDK v${VERSION_NAME}"
            log_info "Pushing main branch to ${DIST_REPO_URL}..."
            git -c "pack.threads=${GRADLE_WORKERS}" \
                -c pack.compression=1 \
                -c http.extraheader= \
                push -u origin main
            log_success "Pushed distribution repository main branch."
        fi

        # Create/update both tags and push them in ONE network round trip.
        log_info "Tagging release v${VERSION_NAME}..."
        git tag -f "v${VERSION_NAME}"
        git tag -f "${VERSION_NAME}"
        git -c http.extraheader= push -f origin "v${VERSION_NAME}" "${VERSION_NAME}"
        log_success "Pushed tags v${VERSION_NAME} & ${VERSION_NAME}."

        # Create GitHub Release via gh CLI
        if command -v gh &> /dev/null; then
            log_info "Creating GitHub Release on ${GITHUB_REPO}..."
            gh release delete "v${VERSION_NAME}" --repo "${GITHUB_REPO}" --yes 2>/dev/null || true
            gh release create "v${VERSION_NAME}" "${ZIP_PATH}" \
                --repo "${GITHUB_REPO}" \
                --title "Kourier SDK v${VERSION_NAME}" \
                --notes "Release of Kourier SDK v${VERSION_NAME} for Android (Public Zero-Credential Maven) and iOS (SPM)."
            log_success "GitHub Release created on ${GITHUB_REPO} with KourierIos.xcframework.zip attached!"
        fi
    )

    # Background branch push is a required publication; surface its failure.
    if ! wait "${MVN_PUSH_PID}"; then
        log_error "Maven repository branch publication failed."
        exit 1
    fi
fi

echo ""
echo -e "${GREEN}${BOLD}================================================================${NC}"
echo -e "${GREEN}${BOLD}  KOURIER SDK v${VERSION_NAME} PUBLISHED SUCCESSFULLY!           ${NC}"
echo -e "${GREEN}${BOLD}================================================================${NC}"
echo -e "  • Distribution Repo:   ${BOLD}https://github.com/${GITHUB_REPO}${NC}"
echo -e "  • Android Maven URL:   ${BOLD}https://raw.githubusercontent.com/${GITHUB_REPO}/mvn-repo${NC}"
echo -e "  • Android Dependency:  ${BOLD}dev.shushant.kourier:kourier-android:${VERSION_NAME}${NC}"
echo -e "  • Release No-Op:       ${BOLD}dev.shushant.kourier:kourier-noop:${VERSION_NAME}${NC}"
echo -e "  • iOS SPM Package:     ${BOLD}https://github.com/${GITHUB_REPO}.git${NC} (Tag: v${VERSION_NAME})"
echo -e "  • iOS Checksum:        ${BOLD}${CHECKSUM}${NC}"
echo -e "  • Total Time:          ${BOLD}$((SECONDS - PIPELINE_START_SECONDS))s${NC}"
echo -e "${GREEN}${BOLD}================================================================${NC}\n"
