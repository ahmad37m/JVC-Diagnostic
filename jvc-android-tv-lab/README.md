# JVC Android TV Lab

Prototype companion project for the JVC LT-58N595 television.

## Confirmed target device
- Android 7.0 / API 24
- MediaTek MT5863 / CVTE platform
- ARM 32-bit userspace
- Approximately 0.78 GB RAM
- Locked bootloader; verified boot green; SELinux enforcing

## Scope
This is a safe, standalone application prototype and compatibility assessment. It does **not** replace the factory launcher, flash firmware, root the TV, disable system apps, change DNS, or claim to boot a second Android OS.

## Reality check
An APK running on Android 7 cannot by itself boot a second Android operating system. A guest OS requires a compatible virtualization/container runtime and kernel/privileges. The target's limited RAM, 32-bit architecture, and locked bootloader are significant constraints. No compatible guest runtime has yet been demonstrated on this TV.

## Next milestones
1. Build a lightweight Android 7-compatible TV interface and device capability report.
2. Verify available app/runtime APIs without changing system settings.
3. Only investigate guest-OS execution if a compatible runtime can be established for this exact platform.
4. Keep the factory launcher and all existing apps untouched unless the owner explicitly authorizes a change.

## Build
The existing repository's GitHub Actions workflow builds the main diagnostic APK. This folder documents the separate Lab prototype scope; it is not itself an APK.
