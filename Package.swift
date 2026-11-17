// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "Kourier",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "Kourier",
            targets: ["KourierSwift", "KourierIos"]
        ),
        .library(
            name: "KourierIos",
            targets: ["KourierIos"]
        ),
    ],
    targets: [
        .target(
            name: "KourierSwift",
            dependencies: [
                "KourierIos"
            ],
            path: "Sources/KourierSwift",
            linkerSettings: [
                .linkedLibrary("sqlite3"),
                .linkedLibrary("c++")
            ]
        ),
        .binaryTarget(
            name: "KourierIos",
            url: "https://github.com/dev-shushant/kourier/releases/download/v0.0.3/KourierIos.xcframework.zip",
            checksum: "56f7d98b5a2d53de4f8d368b34487f9f24f6d823a35bec4b2cf8402bb2a30c99"
        )
    ]
)
