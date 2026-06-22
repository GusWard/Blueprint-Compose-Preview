rootProject.name = "blueprint-report-gradle-plugin"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":blueprint-report-ksp")
project(":blueprint-report-ksp").projectDir = file("../blueprint-report-ksp")
