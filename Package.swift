// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CameraModule",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CameraModule",
            targets: ["CameraModulePlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "CameraModulePlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CameraModulePlugin"),
        .testTarget(
            name: "CameraModulePluginTests",
            dependencies: ["CameraModulePlugin"],
            path: "ios/Tests/CameraModulePluginTests")
    ]
)