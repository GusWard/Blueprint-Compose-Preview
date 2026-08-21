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

rootProject.name = "Blueprint"
include(":blueprint-compose-preview")
include(":blueprint-compose-preview-no-op")
include(":blueprint-compose-preview-multiplatform")
include(":blueprint-compose-preview-multiplatform-core")
include(":blueprint-compose-preview-multiplatform-no-op")
include(":example")
include(":example-multiplatform")
