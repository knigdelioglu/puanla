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
    }
}
rootProject.name = "Puanla"
include(":app")

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
