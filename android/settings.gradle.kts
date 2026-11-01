pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Route Robolectric exclusively to the GCS Central mirror: CI's
        // shared IP can be rate-limited by repo.maven.apache.org.
        exclusiveContent {
            forRepository {
                maven {
                    name = "RobolectricCentralMirror"
                    url = uri("https://maven-central.storage-download.googleapis.com/maven2/")
                }
            }
            filter { includeGroup("org.robolectric") }
        }
        mavenCentral()
    }
}

rootProject.name = "blank-product"

// Core — every product takes all of it, nothing to choose here.
val core = "core"
include(":domain");         project(":domain").projectDir         = file("$core/domain")
include(":core");           project(":core").projectDir           = file("$core/core")
include(":data-local");     project(":data-local").projectDir     = file("$core/data-local")
include(":data-firebase");  project(":data-firebase").projectDir  = file("$core/data-firebase")
include(":analytics");      project(":analytics").projectDir      = file("$core/analytics")
include(":ui-components");  project(":ui-components").projectDir  = file("$core/ui-components")
include(":feature-offers"); project(":feature-offers").projectDir = file("$core/feature-offers")

// Optional capabilities — drop the ones this product does not need.
val optional = "optional"
include(":feature-calculator"); project(":feature-calculator").projectDir = file("$optional/feature-calculator")
include(":feature-rules");      project(":feature-rules").projectDir      = file("$optional/feature-rules")
include(":feature-offer-web");  project(":feature-offer-web").projectDir  = file("$optional/feature-offer-web")

// ─── DESIGN VARIANTS ─────────────────────────────────────────────────────────
// Generated from the manifest. One variant per slot; swap a name here, change the
// matching dependency in app/build.gradle.kts and the import in AppNavGraph.kt.
// Browse the options: platform/catalog/index.html
val modules = "modules"
listOf(
    "ui-header-fintech-2",
    "ui-feedactions-dropdown",
    "ui-offercard-segmented",
    "feature-saved-grid",
    "feature-compare-fintech-1",
    "ui-bottomnav-fintech-3",
).forEach { name ->
    include(":$name")
    project(":$name").projectDir = file("$modules/$name")
}

include(":app")
