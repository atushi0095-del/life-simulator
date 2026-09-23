pluginManagement {
    repositories {
        google()
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

rootProject.name = "Atonannichi"

// :core is pure Kotlin/JVM - every day-count, notification and scheduling rule
// lives there, so the calendar arithmetic is testable without an Android SDK
// (./gradlew :core:test).
include(":core")

// :app needs a local Android SDK. Including it unconditionally would make the
// whole build unconfigurable on a machine without one, taking :core:test down
// with it, so it is included only when an SDK is actually present.
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
        "[Atonannichi] Android SDK not found - skipping :app. " +
            "Set ANDROID_HOME or sdk.dir in local.properties to build the app."
    )
}
