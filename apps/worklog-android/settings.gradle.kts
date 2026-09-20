pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "WorkLog"

// :core is pure Kotlin/JVM - it holds every work-time calculation and is testable
// without the Android SDK (./gradlew :core:test).
include(":core")

// :app needs a local Android SDK. Including it unconditionally makes the whole
// build unconfigurable on machines without one (CI sandboxes, JVM-only agents),
// which would also take :core:test down with it. So it is included only when an
// SDK is actually present. See README.md -> "Building".
val androidSdkDir: String? =
    System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: file("local.properties")
            .takeIf { it.isFile }
            ?.let { props ->
                java.util.Properties().apply { props.inputStream().use { load(it) } }
                    .getProperty("sdk.dir")
            }

if (androidSdkDir != null && file(androidSdkDir).isDirectory) {
    include(":app")
} else {
    logger.lifecycle(
        "[WorkLog] Android SDK not found - skipping :app. " +
            "Set ANDROID_HOME or sdk.dir in local.properties to build the app."
    )
}
