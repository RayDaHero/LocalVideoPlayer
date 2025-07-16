plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.localvideoplayer"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.localvideoplayer"
        minSdk = 30
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Standard Android KTX libraries
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // RecyclerView for displaying the list of videos
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ViewModel and LiveData for MVVM architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Coroutines for background tasks (scanning for videos)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Glide for efficiently loading video thumbnails
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("androidx.media3:media3-exoplayer:1.3.1") // Or the latest version
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("com.google.android.material:material:1.10.0") //

    implementation("com.mrljdx:ffmpeg-kit-full:6.1.1")
//    implementation("io.github.xch168:ffmpeg-kit-full-gpl:1.0.2")
//    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")

    implementation("com.mrljdx:smart-exception-java:0.2.1")
    // WorkManager for reliable background tasks (exporting)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // ViewPager2 for tabbed layout
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // NEW: Required for activityViewModels() delegate
    implementation("androidx.fragment:fragment-ktx:1.6.2")
}