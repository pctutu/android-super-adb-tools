# Android Super ADB Tools

Android TV utility source for inspecting ADB-related capabilities and guiding users through device-side debugging setup.

## Features

- Shows Android version, API level, network IPv4 address, developer-mode state, `adbd` state, and root authorization state.
- Opens available Android settings pages, including developer options, wireless debugging, and vendor-exported settings activities.
- Provides a manual settings-module browser with `Previous`, `Next`, and `Activate` actions.
- Detects exported system activities that may expose vendor-specific TV settings.
- Documents USB ADB, TCP ADB, Android 11+ wireless debugging, root-dependent methods, and their limitations.
- Keeps root and ADB operations explicit; an ordinary APK cannot grant itself root or bypass a disabled `adbd` service.

## Repository Scope

This repository is a source snapshot. It intentionally does not include Gradle project files, build output, APKs, signing keys, passwords, local machine configuration, or generated files.

The Android source is located under `app/src/main`. Integrate these files into an Android application project before building. The application package is `com.pctutu.adb` and the minimum Android API targeted by the original project is API 14.

## Security Notes

ADB exposes powerful device control. Use network ADB only on a trusted network and disable it when it is no longer needed. The application does not contain or distribute `adb`, `adbd`, a root manager, or third-party binaries.

## References

`ADB_METHODS.md` records external projects and platform documentation used as technical references. Their code is not included in this repository. Review each referenced project's current license before reusing any implementation.

## License

No repository-wide open-source license is declared by this snapshot. Add a license before publishing if you want others to redistribute or modify the source.
