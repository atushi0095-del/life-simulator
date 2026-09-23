import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Signing keys and production AdMob IDs are read from outside the repository,
 * so no key material or live ad unit ever lands in git. Three sources, in
 * order of precedence:
 *
 *   1. keystore.properties next to this build (git-ignored)
 *   2. a Gradle property, which covers ~/.gradle/gradle.properties and -P
 *   3. an environment variable, which covers CI
 *
 * When none supplies a signing key, the release build falls back to the debug
 * signature so `assembleRelease` stays runnable for R8 smoke testing - the CI
 * workflow fails the build if that happens while signing secrets are present.
 * When none supplies an AdMob ID, Google's public test IDs are used, so a
 * debug build always serves test ads and never live inventory.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun secret(key: String, env: String): String? =
    keystoreProperties.getProperty(key)
        ?: providers.gradleProperty(key).orNull
        ?: System.getenv(env)

// Google's public test IDs. Safe to commit; they never serve live inventory.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdmobBannerUnitId = "ca-app-pub-3940256099942544/9214589741" // adaptive banner
val testAdmobInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"

val prodAdmobAppId = secret("admob.appId", "COUNTDOWN_ADMOB_APP_ID")
val prodAdmobBannerUnitId = secret("admob.bannerUnitId", "COUNTDOWN_ADMOB_BANNER_UNIT_ID")
val prodAdmobInterstitialUnitId =
    secret("admob.interstitialUnitId", "COUNTDOWN_ADMOB_INTERSTITIAL_UNIT_ID")

// A release built with test IDs is a valid build, just not one that can earn
// anything. Say so, rather than letting it pass silently.
if (prodAdmobAppId == null) {
    logger.warn(
        "WARNING: no production AdMob IDs configured - release will be built with " +
            "Google test ad IDs.",
    )
}

android {
    namespace = "com.ajuworks.atonannichi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ajuworks.atonannichi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }

        manifestPlaceholders["admobAppId"] = prodAdmobAppId ?: testAdmobAppId
        buildConfigField(
            "String",
            "BANNER_AD_UNIT_ID",
            "\"${prodAdmobBannerUnitId ?: testAdmobBannerUnitId}\"",
        )
        buildConfigField(
            "String",
            "INTERSTITIAL_AD_UNIT_ID",
            "\"${prodAdmobInterstitialUnitId ?: testAdmobInterstitialUnitId}\"",
        )
        buildConfigField(
            "boolean",
            "USES_TEST_ADS",
            if (prodAdmobBannerUnitId == null) "true" else "false",
        )
    }

    signingConfigs {
        val storeFilePath = secret("store.file", "COUNTDOWN_KEYSTORE_FILE")
        if (storeFilePath != null && rootProject.file(storeFilePath).isFile) {
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = secret("store.password", "COUNTDOWN_KEYSTORE_PASSWORD")
                keyAlias = secret("key.alias", "COUNTDOWN_KEY_ALIAS")
                keyPassword = secret("key.password", "COUNTDOWN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk { debugSymbolLevel = "FULL" }
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
        debug {
            versionNameSuffix = "-debug"

            // Debug builds always serve test ads, whatever production IDs are
            // configured on the machine. Requesting live ads from a debug build
            // is a policy violation, so it is made structurally impossible
            // rather than left to discipline.
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"$testAdmobBannerUnitId\"")
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_UNIT_ID",
                "\"$testAdmobInterstitialUnitId\"",
            )
            buildConfigField("boolean", "USES_TEST_ADS", "true")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
        jniLibs.useLegacyPackaging = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = true
        // Dependency freshness is a maintenance decision, not a release
        // blocker, and the check needs network access CI does not grant.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.exifinterface)

    // Ads. Free SDKs only - this app runs no server and no paid service.
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
