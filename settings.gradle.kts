pluginManagement {
    includeBuild("blueprint-report-gradle") {
        name = "blueprint-report-gradle-plugin"
    }
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

rootProject.name = "Blueprint"
include(":blueprint-compose-preview")
include(":blueprint-compose-preview-no-op")
include(":example")
include(":blueprint-report-ksp")
include(":blueprint-report-gradle")
