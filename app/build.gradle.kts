import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21"
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.dual_shock.paralax"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.dual_shock.paralax"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // Specifies the ABIs Gradle should build and package
            abiFilters.addAll(listOf("arm64-v8a"))
        }
        buildConfigField("String", "FROST_USERNAME", "\"${localProperties.getProperty("FROST_USERNAME") ?: ""}\"")
        buildConfigField("String", "FROST_PASSWORD", "\"${localProperties.getProperty("FROST_PASSWORD") ?: ""}\"")
        resValue("string", "mapbox_access_token", localProperties.getProperty("MAPBOX_ACCESS_TOKEN")?.toString() ?: "")
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.18.1" // Make sure this matches your CMakeLists.txt version (3.18) or what you have installed
        }
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
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }


    //not compress the .so files when packaging the APK
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.auth)
    //datastore
    implementation(libs.androidx.datastore)

    //mapbox
    implementation(libs.mapbox.maps.compose)
    implementation(libs.mapbox.maps.android)
    implementation(libs.androidx.room.common.jvm)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.datastore.core)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.core.animation)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.remote.creation.compose)
    implementation(libs.androidx.benchmark.traceprocessor)
    implementation(libs.androidx.compose.ui.test)
    implementation(libs.androidx.camera.camera2.pipe)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.splashscreen)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
}

