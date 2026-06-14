plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    id("com.gradleup.nmcp")
    id("signing")
}

group = "uk.co.gusward"
version = "1.0.0"

android {
    namespace = "uk.co.gusward.blueprint.compose.preview.noop"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "uk.co.gusward"
            artifactId = "blueprint-compose-preview-no-op"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Blueprint Preview No-Op")
                description.set("No-op version of Blueprint Preview for release builds.")
                url.set("https://github.com/GusWard/Blueprint-Compose-Preview")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("gusward")
                        name.set("Gus Ward")
                        email.set("guss.warrd@googlemail.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:github.com/GusWard/Blueprint-Compose-Preview.git")
                    developerConnection.set("scm:git:ssh://github.com/GusWard/Blueprint-Compose-Preview.git")
                    url.set("https://github.com/GusWard/Blueprint-Compose-Preview")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
}
nmcp {
    publish("release") {
        username = project.findProperty("ossrhUsername") as String?
        password = project.findProperty("ossrhPassword") as String?
        publicationType = "USER_MANAGED"
    }
}

