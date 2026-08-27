plugins { id("com.android.application") }

android {
    namespace = "com.rafat.munasabati"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rafat.munasabati"
        minSdk = 26
        targetSdk = 35
        versionCode = 23
        versionName = "4.5.5"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.google.zxing:core:3.5.3")
}
