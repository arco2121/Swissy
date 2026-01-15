plugins {
    alias(libs.plugins.android.application)
}
//Variables
val appMajorVersion = "CutThrough"
val appNumber = "1.0.1"
val appVersion = appNumber + if (appMajorVersion != "") " ($appMajorVersion)" else ""
val cameraxVersion = "1.3.0"

android {
    namespace = "com.arco2121.swissy"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.arco2121.swissy"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = appVersion
        resValue("string", "app_version", appVersion)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.location)
    implementation(libs.object1.detection.common)
    implementation(libs.object1.detection)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.guava:guava:31.1-android")
}