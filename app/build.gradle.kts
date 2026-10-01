import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.secrets.gradle)
}

extensions.configure<ApplicationExtension> {
    namespace = "com.locationdots.app"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.locationdots.app"
        minSdk = 26
        targetSdk = 37
        versionCode = providers.gradleProperty("versionCode")
            .map(String::toInt)
            .orElse(1)
            .get()
        versionName = providers.gradleProperty("versionName")
            .orElse("0.1.0")
            .get()
    }

    signingConfigs {
        create("locationDotsDebug") {
            storeFile = rootProject.file("keystore/location-dots-debug.keystore")
            storePassword = "locationdots"
            keyAlias = "location-dots-debug"
            keyPassword = "locationdots"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("locationDotsDebug")
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.location)
    implementation(libs.google.maps.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    add("ksp", libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
