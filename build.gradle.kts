plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// On the author's Windows machine the repo lives inside OneDrive, which locks
// files mid-sync and breaks incremental builds — so build output is redirected
// outside the synced tree. This only applies on Windows; elsewhere (a fresh
// clone, CI) the default per-project build directory is used, so the absolute
// Windows path is never referenced.
if (System.getProperty("os.name").startsWith("Windows")) {
    allprojects {
        layout.buildDirectory.set(File("C:/Users/amirf/.neongrid-build/${project.name}"))
    }
}
