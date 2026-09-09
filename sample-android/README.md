# Sample Android — Network Module example

This sample demonstrates wiring Kourier into an Android network module (OkHttp).

Run the sample
- Open sample-android in Android Studio and run the app (or run a module build):
./gradlew :sample-android:assembleDebug

Network module snippet
- See sample-android/src/main/java/.../network/KourierNetwork.kt for an example of creating OkHttpClient with Kourier interceptor.

Testing
- The sample includes a MockWebServer test that demonstrates retry/caching behavior. Run:
./gradlew :sample-android:test

