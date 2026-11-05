plugins {
    alias(libs.plugins.android.application)

    // Firebase Google Services
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.dotoday"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.dotoday"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    // EXISTING ANDROID LIBRARIES

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // TODOIST API INTEGRATION

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // FIREBASE CLOUD MESSAGING

    implementation(
        platform("com.google.firebase:firebase-bom:35.1.0")
    )

    implementation(
        "com.google.firebase:firebase-messaging"
    )

    // TESTING

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}