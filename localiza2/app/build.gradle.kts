import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.localiza2"
    compileSdk = 35

    defaultConfig {
        applicationId = "es.angelgf.localiza2"
        minSdk = 26
        targetSdk = 35
        versionCode = 19
        versionName = "1.18"

        buildConfigField("String", "API_BASE_URL", "\"https://localiza2-api.angelgf.com.es/\"")
    }

    flavorDimensions.add("environment")
    productFlavors {
        create("production") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://localiza2-api.angelgf.com.es/\"")
        }
        create("development") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.0.133:5135/\"")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "API_BASE_URL", "\"https://localiza2-api.angelgf.com.es/\"")
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.play.services.location)
    implementation(libs.osmdroid)
    implementation(libs.datastore.preferences)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.glide)
    implementation(libs.security.crypto)
    implementation(libs.zxing.core)
    implementation(libs.work.runtime)
}
