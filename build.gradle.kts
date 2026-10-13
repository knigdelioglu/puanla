plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// The shared library source lives in the sibling compose-design-system repository.
// Keep generated outputs inside this checkout so an external read-only source tree
// remains untouched during normal app builds.
findProject(":design-system")?.let { sharedLibrary ->
    sharedLibrary.layout.buildDirectory.set(layout.buildDirectory.dir("design-system"))
}
