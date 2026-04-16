plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = (rootProject.extra["NAMESPACE"] as String) + ".core"
    compileSdk = (rootProject.extra["COMPILE_SDK"] as Int)

    defaultConfig {
        minSdk = (rootProject.extra["MIN_SDK"] as Int)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.serialization.json)
    
    val bom = platform(libs.compose.bom)
    implementation(bom)
    
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.preview)
    implementation(libs.material3)
    
    // Biometric
    implementation(libs.androidx.biometric)
    
    // Google Fonts
    implementation(libs.androidx.ui.text.google.fonts)
    
    // Gson
    implementation(libs.gson)
}
