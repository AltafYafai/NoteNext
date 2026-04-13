import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.suvojeet.notenext"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.suvojeet.notenext"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = project.properties["appVersionCode"]?.toString()?.toInt() ?: 26
        versionName = project.properties["appVersionName"]?.toString() ?: "1.3.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        androidResources {
            localeFilters.add("en")
            localeFilters.add("hi")
        }
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.rootDir.resolve(System.getenv("KEYSTORE_PATH") ?: "my-release-key.keystore"))
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
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
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":credits"))
    implementation(project(":changelog"))
    implementation(project(":feature:todo"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)

    // BOM — ek jagah version, baaki sab auto
    val bom = platform(libs.compose.bom)
    implementation(bom)
    androidTestImplementation(bom)

    // Compose UI core
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    // Material 3 Expressive — THE main one
    implementation(libs.material3)
    implementation(libs.material3.window)
    implementation(libs.material3.adaptive)
    implementation(libs.material3.adaptive.layout)
    implementation(libs.material3.adaptive.navigation)
    implementation(libs.material3.adaptive.navigation.suite)

    // Icons — manually add karna padega ab se M3 1.4.0+
    implementation(libs.icons.core)
    implementation(libs.icons.extended)

    // Dynamic Color (Material You)
    implementation(libs.dynamic.color)

    // Shape Morphing
    implementation(libs.graphics.shapes)

    // Spring animations
    implementation(libs.dynamic.animation)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // DataStore & Security
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.security.crypto)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)   // Coil 3 requires explicit network artifact

    // Jsoup for HTML parsing
    implementation(libs.jsoup)

    // Gson for JSON serialization/deserialization
    implementation(libs.gson)

    // Google Fonts
    implementation(libs.androidx.ui.text.google.fonts)

    // Biometric
    implementation(libs.androidx.biometric)

    // Glance
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Google Drive Backup
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.auth.library.oauth2.http)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // In-App Update
    implementation(libs.app.update)
    implementation(libs.app.update.ktx)

    // Google Play Billing (Donations)
    implementation(libs.billing)

    // Google Play In-App Review
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    // ACRA
    implementation(libs.acra.core)
    implementation(libs.acra.http)
    implementation(libs.acra.toast)
    implementation(libs.acra.notification)
    implementation(libs.acra.dialog)
}

android {
    defaultConfig {
        val rawKey = System.getenv("GROQ_API_1") ?: ""
        val xorKey = 0x47.toByte()
        val encryptedKey = if (rawKey.isNotEmpty()) {
            val bytes = rawKey.toByteArray()
            val encrypted = bytes.map { it.toInt() xor xorKey.toInt() }.map { it.toByte() }.toByteArray()
            Base64.getEncoder().encodeToString(encrypted)
        } else {
            ""
        }
        buildConfigField("String", "GROQ_API_KEY_ENC", "\"$encryptedKey\"")
        buildConfigField("byte", "GROQ_XOR_KEY", "(byte)0x47")
    }
}
