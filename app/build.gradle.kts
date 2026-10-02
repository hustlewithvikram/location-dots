import java.util.Properties
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
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
            .orElse(100)
            .get()
        versionName = providers.gradleProperty("versionName")
            .orElse("1.0.0")
            .get()
    }

    val releaseSigningProperties = Properties().apply {
        val file = rootProject.file("secrets.properties")
        if (file.isFile) {
            file.inputStream().use { load(it) }
        }
    }

    val releaseStoreFile = releaseSigningProperties.getProperty("releaseStoreFile")
    val releaseStorePassword = releaseSigningProperties.getProperty("releaseStorePassword")
    val releaseKeyAlias = releaseSigningProperties.getProperty("releaseKeyAlias")
    val releaseKeyPassword = releaseSigningProperties.getProperty("releaseKeyPassword")
    val hasReleaseSigning = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasReleaseSigning) {
            create("locationDotsRelease") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            // Use the Android Gradle Plugin's generated debug keystore.
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("locationDotsRelease")
            }
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.location)
    implementation(libs.maplibre.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    add("ksp", libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
