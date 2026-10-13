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
        // Resolve published, pinned Compose AAR; skip Maven lookup during source rollback.
        if (providers.gradleProperty("composePublishedVersion").isPresent ||
            providers.gradleProperty("composeUsePublishedByDefault").orNull == "true") {
            providers.gradleProperty("composePublishedRepositoryDir").orNull?.let { path ->
                maven {
                    url = uri(file(path))
                    content { includeGroup("com.knigdelioglu") }
                }
            }
            maven {
                url = uri("https://raw.githubusercontent.com/knigdelioglu/compose-design-system/maven-repository/")
                content { includeGroup("com.knigdelioglu") }
            }
        }
    }
}
rootProject.name = "Puanla"
include(":app")

val composePublishedVersion = providers.gradleProperty("composePublishedVersion").orNull
    ?: providers.gradleProperty("composeDesignSystemVersion").orNull
        ?.takeIf { providers.gradleProperty("composeUsePublishedByDefault").orNull == "true" }
if (composePublishedVersion != null) {
    require(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(-[A-Za-z0-9.-]+)?").matches(composePublishedVersion)) {
        "Geçersiz yayımlanmış Compose sürümü: $composePublishedVersion"
    }
}

if (composePublishedVersion == null) {
    // Emergency local source fallback; never silently follow upstream main.
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
