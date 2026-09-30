# AppArmorX Resilience — Android sample

The sample demonstrates the existing inspector with local SDK modules. It uses compatible Kourier APIs and application ID; its visible product name is AppArmorX Resilience.

From the source repository root:

```bash
./gradlew :sample-android:assembleDebug
```

Run `sample-android` in Android Studio on an emulator or device. Generate demo traffic, then use **Open Network Inspector**, the bubble, notification, or shake trigger to inspect it.

- [Initialization and redaction](src/main/kotlin/dev/shushant/kourier/sample/android/SampleApplication.kt)
- [Sample traffic and inspector controls](src/main/kotlin/dev/shushant/kourier/sample/android/MainActivity.kt)
- [Android integration guide](../docs/quickstart-android.md)

API experiments and scenario sharing are planned for a separate roadmap branch. This sample currently demonstrates capture and inspection.
