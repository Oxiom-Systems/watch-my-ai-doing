plugins {
    id("com.android.application")
}

android {
    namespace = "com.oxiomsystems.wmaid.wear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.oxiomsystems.wmaid"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    val releaseStoreFile = System.getenv("WMAID_KEYSTORE_FILE")
    if (releaseStoreFile != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = System.getenv("WMAID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("WMAID_KEY_ALIAS")
                keyPassword = System.getenv("WMAID_KEY_PASSWORD")
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = false
            }
        }
    }
}

dependencies {
    implementation(project(":core"))
}
