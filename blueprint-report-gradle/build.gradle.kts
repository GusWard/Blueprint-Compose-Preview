plugins {
    kotlin("jvm") version "2.0.21"
    `java-gradle-plugin`
}

kotlin { jvmToolchain(17) }

gradlePlugin {
    plugins {
        create("blueprintReport") {
            id = "uk.co.gusward.blueprint-report"
            implementationClass = "uk.co.gusward.blueprint.report.gradle.BlueprintReportPlugin"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly("androidx.annotation:annotation:1.9.1")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(project(":blueprint-report-ksp"))
    // compose-preview-renderer is invoked as a subprocess via ProcessBuilder — no direct import
    // is needed and the jar is Kotlin-2.3 compiled (incompatible with our Kotlin-2.0 toolchain).
    // The dependency is intentionally omitted from the plugin classpath; the plugin locates the
    // renderer jar at runtime via the consuming project's resolved artifacts (resolveJarFromClasspath).

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}
