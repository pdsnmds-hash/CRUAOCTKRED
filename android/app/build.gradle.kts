import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

val uploadProperties = Properties().apply {
    val file = rootProject.file("signing.properties")
    if (file.exists()) load(file.inputStream())
}

// The template ships without a Firebase project, so the plugin only switches on once a real
// google-services.json is dropped in. Until then HAS_FIREBASE_CONFIG is false and the app
// runs on DemoCardsRepository.
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.cruaoctkred.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.cruaoctkred.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("Boolean", "HAS_FIREBASE_CONFIG", "$hasGoogleServices")
        buildConfigField("Boolean", "USE_FIREBASE_EMULATOR", localProperties.getProperty("USE_FIREBASE_EMULATOR", "false"))
        buildConfigField("String", "APPSFLYER_DEV_KEY", "\"${localProperties.getProperty("APPSFLYER_DEV_KEY", "")}\"")
        buildConfigField("String", "BINOM_ANALYTICS_URL", "\"${localProperties.getProperty("BINOM_ANALYTICS_URL", "")}\"")
        buildConfigField("String", "DESIGN_VERSION", "\"${localProperties.getProperty("DESIGN_VERSION", "blue-ua-3")}\"")
    }

    signingConfigs {
        if (uploadProperties.isNotEmpty()) {
            create("upload") {
                storeFile = rootProject.file(uploadProperties.getProperty("storeFile"))
                storePassword = uploadProperties.getProperty("storePassword")
                keyAlias = uploadProperties.getProperty("keyAlias")
                keyPassword = uploadProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            if (uploadProperties.isNotEmpty()) signingConfig = signingConfigs.getByName("upload")
            // R8 is what isolates the product: platform modules keep their com.cruaoctkred.app
            // source namespace and get repackaged here. See proguard-rules.pro.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    bundle { language { enableSplit = false } }

    splits {
        abi {
            // AGP resource shrinking cannot bundle several APK split resource archives.
            // App bundles perform ABI delivery themselves and retain every native ABI.
            isEnable = !providers.gradleProperty("buildBundle").map(String::toBoolean).getOrElse(false)
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
    // Core — always present.
    implementation(project(":domain"))
    implementation(project(":core"))
    implementation(project(":data-firebase"))
    implementation(project(":data-local"))
    implementation(project(":analytics"))
    implementation(project(":ui-components"))
    implementation(project(":feature-offers"))

    // Optional capabilities.
    implementation(project(":feature-calculator"))
    implementation(project(":feature-rules"))
    implementation(project(":feature-offer-web"))

    // ─── DESIGN VARIANTS ─────────────────────────────────────────────────────
    // Generated from the manifest; must match settings.gradle.kts.
    implementation(project(":ui-header-fintech-2"))
    implementation(project(":ui-feedactions-dropdown"))
    implementation(project(":ui-offercard-segmented"))
    implementation(project(":feature-saved-grid"))
    implementation(project(":feature-compare-fintech-1"))
    implementation(project(":ui-bottomnav-fintech-3"))

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.core)
    implementation(libs.compose.icons.extended)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.play.services)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)

    // Both Coil artifacts are required — without the network fetcher AsyncImage silently
    // loads nothing and no error appears in the log. AGENTS.md section 3.3.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
