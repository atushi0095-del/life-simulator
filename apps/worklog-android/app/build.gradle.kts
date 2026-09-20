import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

/**
 * Signing is read from keystore.properties (git-ignored) or from environment
 * variables, so no key material ever lives in the repo. When neither is
 * present the release build falls back to the debug signature, which keeps
 * `assembleRelease` runnable for R8 smoke testing without a key.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}

fun secret(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

android {
    namespace = "com.ajuworks.worklog"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ajuworks.worklog"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Google's public test IDs. Replaced at release time via
        // keystore.properties / CI env - see README.md -> "AdMob".
        manifestPlaceholders["admobAppId"] =
            secret("admob.appId", "WORKLOG_ADMOB_APP_ID")
                ?: "ca-app-pub-3940256099942544~3347511713"
        buildConfigField(
            "String",
            "ADMOB_BANNER_UNIT_ID",
            "\"${secret("admob.bannerUnitId", "WORKLOG_ADMOB_BANNER_UNIT_ID")
                ?: "ca-app-pub-3940256099942544/6300978111"}\"",
        )
        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_UNIT_ID",
            "\"${secret("admob.interstitialUnitId", "WORKLOG_ADMOB_INTERSTITIAL_UNIT_ID")
                ?: "ca-app-pub-3940256099942544/1033173712"}\"",
        )
    }

    signingConfigs {
        val storeFilePath = secret("store.file", "WORKLOG_KEYSTORE_FILE")
        if (storeFilePath != null && rootProject.file(storeFilePath).isFile) {
            create("release") {
                storeFile = rootProject.file(storeFilePath)
                storePassword = secret("store.password", "WORKLOG_KEYSTORE_PASSWORD")
                keyAlias = secret("key.alias", "WORKLOG_KEY_ALIAS")
                keyPassword = secret("key.password", "WORKLOG_KEY_PASSWORD")
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
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
        )
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Lets MigrationTestHelper read the exported schemas from instrumented tests.
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")

    lint {
        warningsAsErrors = true
        abortOnError = true
        // Play's own tooling reports these; they are not code defects.
        disable += setOf("GradleDependency", "NewerVersionAvailable")
        htmlReport = true
        xmlReport = true
    }
}

ksp {
    // Checked-in schemas make Room migrations reviewable in the diff.
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
