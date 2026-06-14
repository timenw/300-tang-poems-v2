// settings.gradle.kts
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
        // Pangle (穿山甲) SDK
        maven { url = uri("https://artifact.bytedance.com/repository/pangle/") }
        // AdMob / Google Ads
        maven { url = uri("https://maven.google.com") }
    }
}

rootProject.name = "Poem300"
include(":app")
