#!/usr/bin/env bash
set -euo pipefail

repo="${TAILSCALE_TV_REPO:-Rodmodrtf/tailscale-android}"
tag="${TAILSCALE_TV_TAG:-tv-home-latest}"
tv="${1:?usage: $0 TV_IP_OR_HOST[:PORT]}"
workdir="$(mktemp -d)"

cleanup() {
  rm -rf "${workdir}"
}
trap cleanup EXIT

if [[ "${tv}" != *:* ]]; then
  tv="${tv}:5555"
fi

base_url="https://github.com/${repo}/releases/download/${tag}"
apk="${workdir}/tailscale-tv-debug.apk"
sumfile="${workdir}/tailscale-tv-debug.apk.sha256"

curl -fsSL "${base_url}/tailscale-tv-debug.apk" -o "${apk}"
curl -fsSL "${base_url}/tailscale-tv-debug.apk.sha256" -o "${sumfile}"

(
  cd "${workdir}"
  sha256sum -c tailscale-tv-debug.apk.sha256
)

adb connect "${tv}"
adb install -r "${apk}"
adb shell am start -n com.tailscale.ipn/com.tailscale.ipn.MainActivity
