plugins { id("com.android.application") }

android {
    namespace = "com.rafat.munasabati"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rafat.munasabati"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "2.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
