plugins {
    id("com.android.application") version "8.13.0"
    id("org.jetbrains.kotlin.android") version "1.9.22"
    id("com.google.gms.google-services") // Corrected this line
}

android {
    namespace = "com.example.carbontracer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.carbontracer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // Use a more current Java version like 17, as it's common for modern Android
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // Added View Binding for easier access to your layout views
    buildFeatures {
        viewBinding = true
    }
    buildToolsVersion = "36.0.0"
}

dependencies {
    // AndroidX & Material Design Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Firebase Bill of Materials (BoM) - Declared once
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Firebase Libraries (no versions needed due to BoM) // Core KTX library
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx") // Use the KTX version for Kotlin
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}