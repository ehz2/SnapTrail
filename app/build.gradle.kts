plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Firebase
    id("com.google.gms.google-services")
    // Google map plugin
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    // Navigation
    id("androidx.navigation.safeargs.kotlin")
    // Parsing
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.snaptrail"
    compileSdk = 35

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.snaptrail"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.activity)
    implementation(libs.firebase.auth)

    implementation(libs.play.services.location)
    
    // Firestore
    implementation(libs.firebase.firestore.ktx)  
    implementation(libs.firebase.firestore)
    implementation(libs.glide)

    testImplementation(libs.junit)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")

    // Google map implementation
    implementation(libs.play.services.maps)

    // Google places implementation
    val kotlin_version = "1.8.0"
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlin_version"))
    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Google AI
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    //Confetti animation
    implementation("nl.dionsegijn:konfetti-xml:2.0.4")
}