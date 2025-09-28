plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.microsoft.xboxtcui"
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
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.fragment.ktx)

    implementation(libs.firebase.messaging)
    implementation(libs.firebase.iid)

    implementation(libs.pkix)

    implementation(libs.gson)
    implementation(libs.httpclient)
    implementation(libs.simple.xml)
}
