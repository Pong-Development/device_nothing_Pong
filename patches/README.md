# Refresh-rate ADPF dependencies

The 60/90/120 Hz profiles in `configs/powerhint.json` require these changes
outside the device tree:

| Repository | Required commits |
| --- | --- |
| `hardware/lineage/interfaces` | `ee24f98` (refresh selection), `b9e9ee4` (session replacement), `3e3c641` (callback lifetime) |
| `hardware/google/pixel`, `lineage-23.2` | `780aef7`, supplied in `hardware_google_pixel/0001-libperfmgr-synchronize-adpf-callbacks.patch` |

The Pixel patch is based on Lineage's `lineage-23.2` commit `8d37122`. From
the ROM source root, with this device tree at `device/nothing/Pong`, apply it
before building the corrected Power HAL:

```sh
git -C hardware/google/pixel am ../../../device/nothing/Pong/patches/hardware_google_pixel/0001-libperfmgr-synchronize-adpf-callbacks.patch
```

The device build does not apply this patch automatically. Rebuild and flash
the vendor image containing the JSON, Power HAL, and updated libperfmgr.

SurfaceFlinger supplies its active display period through ADPF. The HAL uses
the newest live SurfaceFlinger session to select a configured refresh hint.
The event node updates SurfaceFlinger, HWUI, and SYSUI sessions; other app and
game sessions retain their default profile. Unsupported rates and the absence
of a live SurfaceFlinger session release the hint and restore the default.

The profiles preserve the original boost limits and reporting rate. Their
60/90 Hz proportional gains are initial values scaled from the 120 Hz profile,
and still need performance and power measurements after flashing.

Validation includes Lineage's ADPF parser and host checks using the actual
callback and selector methods. ASan, UBSan, and TSan checks passed for session
replacement, unsupported rates, callback lifetime, registration ordering, and
concurrent session creation/close. The original callback code reproduced an
ASan heap-use-after-free. A full Android build and on-device validation of the
new profiles remain required.
