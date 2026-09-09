# Quickstart — iOS (Swift Package Manager)

This guide shows how to add Kourier to an iOS network module using Swift Package Manager (SPM).

1) Add the package
- In Xcode: File → Add Packages… → add the repo URL and choose the kourier-ios / Package.swift target.
- Or add to your Package.swift as a dependency:
.package(url: "https://github.com/dev-shushant/kourier-kmp.git", from: "0.1.0"),

2) Use the interceptor in a network module
import KourierInterceptorDarwin

let session = URLSession(configuration: {
  let config = URLSessionConfiguration.default
  // configure as needed
  return config
}())
let interceptor = KourierInterceptor(/* config if any */)
// Wire interceptor according to how you implement network middleware (URLProtocol or custom wrapper)

3) Validate package
Run locally:
swift package resolve
swift test (if tests are present in SPM targets)

Notes
- If you publish XCFrameworks in the future we’ll include example integration with binary targets.

