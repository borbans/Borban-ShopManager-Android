plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun prop(name: String): String =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(name).orNull.orEmpty()

val signingStorePath = System.getenv("BORBAN_KEYSTORE_PATH")
val signingStorePassword = System.getenv("BORBAN_KEYSTORE_PASSWORD")
val signingKeyAlias = System.getenv("BORBAN_KEY_ALIAS")
val signingKeyPassword = System.getenv("BORBAN_KEY_PASSWORD")
val hasReleaseSigning = listOf(signingStorePath, signingStorePassword, signingKeyAlias, signingKeyPassword).all { !it.isNullOrBlank() }
val releaseArtifactTasks = setOf("assemble", "assemblerelease", "bundle", "bundlerelease", "installrelease", "packagerelease")
val releaseBuildRequested = gradle.startParameter.taskNames.any {
    it.substringAfterLast(':').lowercase() in releaseArtifactTasks
}

if (releaseBuildRequested && !hasReleaseSigning) {
    throw GradleException("Permanent signing credentials are required for every release build")
}

android {
    namespace = "de.borban.shopmanager"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "de.borban.shopmanager"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.2.2"
        resValue("string", "firebase_app_id", prop("BORBAN_FIREBASE_APP_ID"))
        resValue("string", "firebase_api_key", prop("BORBAN_FIREBASE_API_KEY"))
        resValue("string", "firebase_project_id", prop("BORBAN_FIREBASE_PROJECT_ID"))
        resValue("string", "firebase_sender_id", prop("BORBAN_FIREBASE_SENDER_ID"))
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("borbanRelease") {
                storeFile = file(signingStorePath!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("borbanRelease")
            }
        }
    }
    buildFeatures { compose = true; buildConfig = false }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.firebase:firebase-messaging:24.1.0")
    testImplementation("junit:junit:4.13.2")
}
