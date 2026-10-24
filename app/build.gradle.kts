plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")
fun config(name: String, fallback: String = "") = providers.environmentVariable(name).orElse(providers.gradleProperty(name)).getOrElse(fallback)
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
android {
    namespace = "app.parole"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.parole"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "API_BASE_URL", quoted(config("PAROLE_API_URL")))
        buildConfigField("String", "ADMOB_REWARDED_ID", quoted(config("ADMOB_REWARDED_ID", "ca-app-pub-3940256099942544/5224354917")))
        buildConfigField("boolean", "FIREBASE_CONFIGURED", file("google-services.json").exists().toString())
        manifestPlaceholders["admobAppId"] = config("ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
    }
    signingConfigs {
        create("production") {
            val path = config("ANDROID_KEYSTORE_PATH")
            if (path.isNotBlank()) {
                storeFile = file(path)
                storePassword = config("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = config("ANDROID_KEY_ALIAS")
                keyPassword = config("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-beta" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (config("ANDROID_KEYSTORE_PATH").isNotBlank()) signingConfig = signingConfigs.getByName("production")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true; animationsDisabled = true }
    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}") }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.01"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-ads:24.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
