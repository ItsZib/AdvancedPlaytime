plugins {
    id("java-library")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "com.zib"
version = "1.2.1"

repositories {
    mavenCentral()
}

dependencies {
    // 1. Hytale Server API (Local File)
    compileOnly(files("libs/hytale-server.jar"))

    // 2. Database Drivers (HikariCP + MySQL + SQLite)
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.mysql:mysql-connector-j:8.4.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    // 3. Common Utils
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.code.findbugs:jsr305:3.0.2") // For @Nonnull
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("Playtime")
        archiveClassifier.set("")
        // minimize() is REMOVED to ensure database drivers are included!
    }
    build {
        dependsOn(shadowJar)
    }
}