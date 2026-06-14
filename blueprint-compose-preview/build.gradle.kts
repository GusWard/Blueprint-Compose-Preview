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
    namespace = "uk.co.gusward.blueprint.compose.preview"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
            artifactId = "blueprint-compose-preview"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Blueprint Preview")
                description.set("A Jetpack Compose dev tool for architectural blueprint overlays.")
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

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2026.05.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

nmcp {
    publish("release") {
        username = project.findProperty("ossrhUsername") as String?
        password = project.findProperty("ossrhPassword") as String?
        publicationType = "USER_MANAGED"
    }
}
