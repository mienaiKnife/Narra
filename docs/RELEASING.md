# Releasing Narra

## Versioning

Releases are driven by Git tags of the form `vMAJOR.MINOR.PATCH` (for example `v0.1.0`).

- `versionName` is the tag without the `v` prefix (`0.1.0`).
- `versionCode` is derived from the tag as `MAJOR * 10000 + MINOR * 100 + PATCH` (`100`).

Locally, when no version is provided, the build falls back to `versionCode = 1` and
`versionName = "0.1"`. Do not edit the version in `app/build.gradle.kts` for a release; the
`Release` workflow derives it from the tag.

## Signing

Release signing is optional. If no keystore is configured, `assembleRelease` still produces a
minified but **unsigned** `app-release-unsigned.apk`, which is what normal CI builds do.

Signing credentials are read from environment variables or `local.properties` (which is
git-ignored), in that order:

| Key | Description |
| --- | --- |
| `RELEASE_KEYSTORE_FILE` | Path to the `.jks`/`.keystore` file; an absolute path is recommended because relative paths resolve from `app/` |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |
| `VERSION_NAME` | Overrides `versionName` |
| `VERSION_CODE` | Overrides `versionCode` |

`local.properties` does not expand `~` or environment variables, so use an absolute path there.

### One-time signing setup

Create the release key once and keep it outside the repository. Losing the key means existing users
cannot install updates without uninstalling the app and losing data.

```bash
mkdir -p "$HOME/.keystores"
keytool -genkeypair -v \
  -keystore "$HOME/.keystores/narra-release.jks" \
  -alias narra \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12 \
  -dname "CN=Narra Release, O=Narra, C=US"
chmod 600 "$HOME/.keystores/narra-release.jks"
```

Any path outside the repository works; keep it consistent with `RELEASE_KEYSTORE_FILE`. Keytool
prompts for a password. Use a strong generated password from a password manager and reuse it as the
key password; PKCS12 keystores use a single password. Back up the keystore and the password
separately.

Add these **repository secrets** in GitHub under **Settings → Secrets and variables → Actions →
Secrets**. Do not use environment secrets (the workflow does not declare an environment) or
variables (those are not encrypted):

- `RELEASE_KEYSTORE_BASE64` – `base64 -w0 "$HOME/.keystores/narra-release.jks"` (on macOS use `base64 -i <file> | tr -d '\n'`)
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS` – `narra`
- `RELEASE_KEY_PASSWORD` – same as the keystore password

### Signature changes

Android only installs an update signed with the same key. If you previously installed a debug build,
or a release signed with a different key, the first signed release cannot upgrade over it. Export a
database or OPML backup, uninstall the old build, install the new release, and restore the backup.

## Cutting a release

1. Make sure `main` is green and the commit you are tagging is the one CI tested.
2. Build and smoke-test a signed release locally (see below).
3. Create and push an annotated tag:
   ```bash
   git tag -a v0.1.0 -m "Narra v0.1.0"
   git push origin v0.1.0
   ```
4. The `Release` workflow (`.github/workflows/release.yml`) decodes the keystore, builds the signed
   APK, verifies its signature and version, and publishes `narra-v0.1.0.apk` plus a `.sha256`
   checksum. If `docs/releases/v0.1.0.md` exists, it is used for the release notes; otherwise GitHub
   generates notes from the commits.

Do not create the GitHub Release in the web UI first. The web UI creates the tag, which triggers the
workflow, and then the workflow cannot create the release because it already exists. Push only the
tag and let the workflow create the release.

If the workflow fails after the tag is pushed, fix the issue and re-run the workflow. The publish step
replaces existing assets, so a re-run is safe.

## Local signed release smoke test

```bash
export RELEASE_KEYSTORE_FILE="$HOME/.keystores/narra-release.jks"
read -rsp 'Keystore password: ' STORE_PASS; echo
export RELEASE_KEYSTORE_PASSWORD="$STORE_PASS"
export RELEASE_KEY_ALIAS='narra'
export RELEASE_KEY_PASSWORD="$STORE_PASS"
export VERSION_NAME='0.1.0'
export VERSION_CODE='100'

./gradlew clean assembleRelease
```

Then verify and install:

```bash
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
APK=app/build/outputs/apk/release/app-release.apk

"$SDK/build-tools/37.0.0/apksigner" verify --print-certs "$APK"
"$SDK/build-tools/37.0.0/aapt" dump badging "$APK" | head -1
adb install -r "$APK"
```

Adjust the build-tools version if yours differs. Because R8 issues often only surface at runtime,
smoke-test Android TTS, Sherpa-ONNX TTS, RSS refresh, EPUB import, queue playback, background
playback and lock-screen controls, the widget, and backup/restore before tagging.

Release APKs include only the `arm64-v8a` and `armeabi-v7a` native libraries because the x86/x86_64
libraries are only needed by emulators and roughly double the download size. Use a debug build for
x86 emulators.

## Verifying a published release

- `apksigner verify --print-certs narra-v0.1.0.apk`
- `aapt dump badging narra-v0.1.0.apk | head -1` should show `versionCode='100'` and
  `versionName='0.1.0'`.
- `sha256sum -c narra-v0.1.0.apk.sha256` (`shasum -a 256 -c` on macOS)
- Install the APK from the release URL on a clean device.

## Minification

Release builds enable R8 (`isMinifyEnabled = true`) and resource shrinking
(`isShrinkResources = true`). Keep rules live in `app/proguard-rules.pro`. Reflection-based code
paths (Sherpa-ONNX JNI and its Kotlin callback, plus the OPML kxml/xmlpull fallback) have explicit
keeps.

Because R8 issues often only surface at runtime, smoke-test a release build on a device before
publishing if you touched reflection, serialization, or JNI code.

## Database migrations

Room schemas below `AppDatabase.MIN_SUPPORTED_VERSION` (16) predate schema export and are recreated
destructively. Any schema at or above that version needs a real `Migration` and a
`DatabaseMigrationTest` case. See [ARCHITECTURE.md](ARCHITECTURE.md#database-migrations).
