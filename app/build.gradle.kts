// ADD THIS BLOCK AT THE TOP OF YOUR FILE
configurations.configureEach {
    resolutionStrategy {
        exclude(group = "com.google.inject", module = "guice")
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.carbontracer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.carbontracer"
        minSdk = 24
        targetSdk = 36
        multiDexEnabled = true
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

        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    // Added View Binding for easier access to your layout views
    buildFeatures {
        viewBinding = true
        mlModelBinding = true
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

    // Image Cropper
    implementation("com.github.yalantis:ucrop:2.2.11-native") // Check for the latest version

    // TensorFlow Lite for ML model inference
    implementation("org.tensorflow:tensorflow-lite:2.9.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.0")

    // Testing Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    // Retrofit: The main networking library
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
// Gson Converter: Converts JSON data to/from Kotlin objects
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    implementation("org.tensorflow:tensorflow-lite:2.15.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("org.tensorflow:tensorflow-lite:2.15.0")
}