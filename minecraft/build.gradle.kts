plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.mojang.minecraftpe"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":fmod"))
    implementation(project(":httpclient"))
    implementation(project(":microsoft:xal"))
    implementation(project(":microsoft:xbox"))

    implementation(libs.conscrypt.android)
    implementation(libs.androidx.games.activity)
    implementation(libs.core.splashscreen)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.firebase.messaging)
    implementation(libs.firebase.iid)
}
