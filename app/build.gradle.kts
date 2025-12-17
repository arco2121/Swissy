plugins {
    alias(libs.plugins.android.application)
}
val appMajorVersion = "CutThrough"
val appNumber = "1.0.0"
val appVersion = appNumber + if (appMajorVersion != "") " ($appMajorVersion)" else ""

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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}