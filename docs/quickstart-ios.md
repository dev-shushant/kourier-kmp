# AppArmorX Resilience — iOS quickstart

AppArmorX Resilience, formerly Kourier, retains existing Swift products and framework. In Xcode, use **File → Add Package Dependencies** with `https://github.com/dev-shushant/kourier.git` and choose a [published version](https://github.com/dev-shushant/kourier/releases). Add the `Kourier` product for Swift conveniences, or retain your existing `KourierIos` integration.

Initialize once at app startup:

```swift
import KourierIos

Kourier.shared.doInit { builder in
    builder.maxPayloadSize(bytes: 500 * 1024)
    builder.maxRetentionCount(count: 1000)
}
```

Configure the session before creating it:

```swift
import Foundation

let configuration = URLSessionConfiguration.default
KourierURLSessionConfiguration.shared.enable(configuration: configuration)
let session = URLSession(configuration: configuration)
```

Open with `Kourier.shared.showUI()`. The app's `Info.plist` must include:

```xml
<key>CADisableMinimumFrameDurationOnPhone</key>
<true/>
```

The `Kourier` Swift product supplies SQLite/libc++ linker settings; manual integration needs `-lsqlite3 -lc++`. Check the selected release binary's actual deployment target. The manifest's iOS 15 declaration does not determine a framework's compiled minimum OS. Custom URLProtocol interception does not cover background sessions.

Configure redaction before capturing sensitive traffic; see the [consumer guide](https://github.com/dev-shushant/kourier#readme) and [SwiftUI sample](../samples/sample-ios/KourierSampleApp/Sources/KourierSampleApp.swift) for KotlinArray helpers. Keep this inspector in development/internal builds according to your packaging strategy. The rebrand does not establish an iOS production no-op guarantee or add scenario controls.

See [the brand transition](rebranding.md).
