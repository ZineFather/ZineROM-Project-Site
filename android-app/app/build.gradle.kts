import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firebase is mandatory for every build variant. Never produce an APK that
// appears installable but cannot register for ZineROM push notifications.
val googleServicesFile = file("google-services.json")
if (!googleServicesFile.isFile || googleServicesFile.length() == 0L) {
    throw GradleException(
        "Missing android-app/app/google-services.json. Configure the Firebase Android client " +
            "for project zinerom-updates and package com.zinerom.info before building."
    )
}
apply(plugin = "com.google.gms.google-services")

// Stable signing material is supplied through CI/local environment variables.
// Never commit the keystore or its passwords to the repository.
val signingKeystoreBase64 = providers.environmentVariable("ZINEROM_SIGNING_KEYSTORE_BASE64").orNull
val signingStorePassword = providers.environmentVariable("ZINEROM_SIGNING_STORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("ZINEROM_SIGNING_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("ZINEROM_SIGNING_KEY_PASSWORD").orNull
val hasStableSigning = listOf(
    signingKeystoreBase64,
    signingStorePassword,
    signingKeyAlias,
    signingKeyPassword
).all { !it.isNullOrBlank() }

val stableKeystoreFile = layout.buildDirectory.file("signing/zinerom-stable.jks").get().asFile
if (hasStableSigning) {
    stableKeystoreFile.parentFile.mkdirs()
    stableKeystoreFile.writeBytes(Base64.getDecoder().decode(signingKeystoreBase64))
}

android {
    namespace = "com.zinerom.info"
    compileSdk = 36

    defaultConfig {
        // Do not change this ID after publishing; Android uses it to identify app updates.
        applicationId = "com.zinerom.info"
        minSdk = 26
        targetSdk = 35
        // Can be overridden for release builds; always keep it higher than the installed build.
        versionCode = providers.gradleProperty("zineromVersionCode").orNull?.toInt() ?: 334
        versionName = providers.gradleProperty("zineromVersionName").orNull ?: "3.3.4"
    }

    signingConfigs {
        if (hasStableSigning) {
            create("zineromStable") {
                storeFile = stableKeystoreFile
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // CI debug APKs use the same stable certificate when signing secrets are configured.
            if (hasStableSigning) signingConfig = signingConfigs.getByName("zineromStable")
        }
        getByName("release") {
            isMinifyEnabled = false
            // Release uses the same certificate as debug to permit in-place upgrades.
            if (hasStableSigning) signingConfig = signingConfigs.getByName("zineromStable")
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
