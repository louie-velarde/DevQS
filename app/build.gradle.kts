plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "me.velc.devqs"
    compileSdk = 36

    defaultConfig {
        applicationId = "me.velc.devqs"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
}