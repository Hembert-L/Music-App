plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "dev.aiko.music"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.aiko.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "0.7.6"
        testInstrumentationRunner = "dev.aiko.music.MusicInstrumentation"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            // Locally installable non-debug build; keeps Music's existing identity and data.
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isMinifyEnabled = false
            matchingFallbacks += listOf("release")
            versionNameSuffix = "-benchmark"
        }
    }
    lint { abortOnError = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.media3:media3-exoplayer:1.7.1")
    implementation("androidx.media3:media3-session:1.7.1")
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    testImplementation("junit:junit:4.13.2")
}
