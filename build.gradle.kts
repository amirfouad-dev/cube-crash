plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// The repo lives inside OneDrive, which locks files mid-sync and breaks
// incremental builds. Keep all build output outside the synced tree.
allprojects {
    layout.buildDirectory.set(File("C:/Users/amirf/.neongrid-build/${project.name}"))
}
