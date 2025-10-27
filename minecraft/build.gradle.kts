plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.mojang.minecraftpe"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/INDEX.LIST",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.annotations)
    implementation(libs.androidx.annotation)
    implementation(libs.okhttp)
    implementation(libs.httpclient)
    implementation(libs.androidx.annotation)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.iid)

    implementation(libs.pkix)

    implementation(libs.gson)
    implementation(libs.httpclient)
    implementation(libs.simple.xml)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.browser)

    implementation(libs.pkix)
    implementation(libs.core)
    implementation(libs.prov)
    implementation(libs.pkix)

    // Architectural Components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.billing)
    implementation(libs.conscrypt.android)
    implementation(libs.androidx.games.activity)
    implementation(libs.core.splashscreen)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.firebase.messaging)
    implementation(libs.firebase.iid)
}
