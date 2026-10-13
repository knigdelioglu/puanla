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
        mavenCentral()
        // Opt-in Maven-based P0 compatibility check. The published version must exist.
        if (providers.gradleProperty("composePublishedVersion").isPresent ||
            providers.gradleProperty("composeUsePublishedByDefault").orNull == "true") {
            providers.gradleProperty("composePublishedRepositoryDir").orNull?.let { path ->
                maven { url = uri(file(path)) }
            }
            maven { url = uri("https://raw.githubusercontent.com/knigdelioglu/compose-design-system/maven-repository/") }
        }
    }
}
rootProject.name = "Puanla"
include(":app")

val composePublishedVersion = if (providers.gradleProperty("composeLegacySources").orNull == "true") {
    null
} else {
    providers.gradleProperty("composePublishedVersion").orNull
        ?: providers.gradleProperty("composeDesignSystemVersion").orNull
            ?.takeIf { providers.gradleProperty("composeUsePublishedByDefault").orNull == "true" }
}
if (composePublishedVersion != null) {
    require(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(-[A-Za-z0-9.-]+)?").matches(composePublishedVersion)) {
        "Geçersiz yayımlanmış Compose sürümü: $composePublishedVersion"
    }
}

if (composePublishedVersion == null) {
val composeDesignSystemDir = providers.gradleProperty("composeDesignSystemDir")
    .map { path -> file(path) }
    .orElse(file("../compose-design-system/design-system"))
    .get()
require(composeDesignSystemDir.isDirectory) {
    "Compose Design System bulunamadı: $composeDesignSystemDir. " +
        "`-PcomposeDesignSystemDir=<design-system modül dizini>` ile yolu belirtebilirsiniz."
}

include(":design-system")
project(":design-system").projectDir = composeDesignSystemDir
}
