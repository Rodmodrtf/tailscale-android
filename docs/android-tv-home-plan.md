# Android TV Home Build Plan

This fork keeps the existing Tailscale Android app behavior intact while making the TV build feel native on Android TV and Fire TV, then publishing an APK that a home server agent can install over ADB.

## Goals

- Keep all existing phone, tablet, ChromeOS, Taildrop, exit node, split tunnel, MDM, quick settings, and deep-link behavior.
- Make the TV build navigable by D-pad from the couch, without touch-only controls.
- Add explicit TV settings for starting Tailscale after device boot and guiding the user to Android's Always-on VPN system setting.
- Produce a GitHub Release APK asset that a home server agent can poll and install on known TVs.

## Current State

- The app already declares `LEANBACK_LAUNCHER`, a TV banner, and optional `android.software.leanback` support.
- Gradle already supports `-PPLATFORM=tv`, which changes TV manifest requirements and reserves a TV-specific version-code suffix.
- The Makefile already builds phone/tablet debug APKs and signed phone/TV release AABs.
- This fork adds `make tailscale-tv-debug` and a manual `Android TV APK Release` workflow that uploads `tailscale-tv-debug.apk` as both an artifact and a release asset.

## Phase 1: Releaseable Home APK

- Use the debug-signed TV APK while the fork is internal/home-only. This avoids storing a private Android signing key in GitHub.
- Trigger the `Android TV APK Release` workflow manually with a tag such as `tv-home-latest`.
- Home server agent install command:

```sh
scripts/install-tv-apk-from-github-release.sh TV_IP
```

- Manual install command:

```sh
adb connect TV_IP:5555
adb install -r tailscale-tv-debug.apk
adb shell am start -n com.tailscale.ipn/com.tailscale.ipn.MainActivity
```

- Later, add a private release keystore in GitHub Actions secrets and switch the workflow to a signed release APK target before distributing outside the home.

## Phase 2: Real TV Layout

- Introduce TV-specific composables under the existing Compose/navigation structure instead of forking app logic.
- Route Android TV devices to a `TvMainView` that reuses the existing view models and local API clients.
- First screen:
  - Large connection state and connect/disconnect action.
  - Device name, tailnet, selected exit node, and health warnings.
  - Rows for peers, exit nodes, Taildrop, settings, and about.
- Use D-pad focus states, predictable row/column navigation, and large hit targets.
- Avoid text entry where possible. For login, keep QR/deep-link login first and custom server entry second.
- Keep phone/tablet views as the default on non-TV devices.

## Phase 3: Boot And Always-on VPN

- Add a TV settings section:
  - `Start Tailscale on boot`
  - `Open Android Always-on VPN settings`
  - `Reconnect if VPN is stopped`
- Implement boot start with a `BOOT_COMPLETED` receiver that enqueues the existing `StartVPNWorker` only when the user has enabled the setting and VPN preparation is already granted.
- Do not silently bypass Android VPN consent. If `VpnService.prepare()` requires user approval, show a TV-friendly permission screen.
- Always-on VPN is an Android system setting controlled outside the app for normal apps. Provide a clear button that opens the correct settings intent and shows current guidance.
- Preserve MDM controls. If MDM forces connection behavior, surface that state instead of letting the TV toggle fight policy.

## Phase 4: Home Server Agent Install Flow

- Add a small installer script or agent task that:
  - Queries the latest GitHub Release asset.
  - Downloads `tailscale-tv-debug.apk`.
  - Checks SHA256.
  - Runs `adb install -r`.
  - Launches Tailscale.
  - Reports per-TV success/failure.
- Store TV hostnames/IPs in the home server agent config, not in this public fork.
- Prefer ADB-over-LAN for Fire TV and Android TV boxes already in developer mode.

## Phase 5: Validation

- Add Compose previews or UI tests for TV focus behavior.
- Test on at least one Android TV emulator and one physical Fire TV/Android TV device.
- Confirm:
  - Fresh install and upgrade install work.
  - Login works with D-pad only.
  - Connect/disconnect works.
  - Exit-node selection works.
  - Boot start reconnects after a restart when permission is already granted.
  - Existing phone/tablet build and tests still pass.

## Implementation Notes

- Keep UI changes isolated to TV-specific composables and routing checks using `AndroidTVUtil.isAndroidTV()`.
- Keep shared state in the existing view models.
- Keep release automation in GitHub Actions so the home server agent only needs GitHub Release read access.
- When this becomes more than a home build, switch from debug signing to a private signing key and versioned releases.
