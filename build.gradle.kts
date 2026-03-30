plugins {
    id("java")
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.example.devfastjavafx"
version = "0.1.0-alpha"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2024.3")
        instrumentationTools()
    }

    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    testImplementation(kotlin("test"))
}

intellijPlatform {
    pluginConfiguration {
        id = "com.example.devfastjavafx"
        name = "devFastJavaFx"
        version = "0.1.0-alpha"

        vendor {
            name = "Example"
        }

        description = "A custom, internal JetBrains IDE plugin for fetching and inserting JavaFX templates."
    }
}

kotlin {
    jvmToolchain(21)
}
