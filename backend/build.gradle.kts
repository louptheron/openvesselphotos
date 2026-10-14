import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension

val springBootVersion = "4.1.1"
val kotlinVersion = "2.4.21"

plugins {
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.kotlin.plugin.spring") version "2.4.21"

    kotlin("jvm") version "2.4.21"
    kotlin("plugin.jpa") version "2.4.21"

    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "org.openvesselphotos"
version = "0.0.1"
description = "Open Vessel Photos"
java.sourceCompatibility = JavaVersion.VERSION_21

springBoot {
    mainClass.set("org.openvesselphotos.OpenVesselPhotosApplicationKt")
}

ktlint {
    version = "1.5.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
    }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-json:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-flyway:$springBootVersion")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Data / persistence
    implementation("org.flywaydb:flyway-core:13.9.0")
    implementation("org.flywaydb:flyway-database-postgresql:13.9.0")
    runtimeOnly("org.postgresql:postgresql:42.7.14")

    // Images: EXIF orientation
    implementation("com.drewnoakes:metadata-extractor:2.21.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test:$springBootVersion")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
}

// The built frontend (frontend/build) is served by Spring Boot as static files.
tasks.named<ProcessResources>("processResources") {
    from(rootProject.file("frontend/build")) {
        into("static")
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("openvesselphotos.jar")
}

tasks.named<Jar>("jar") {
    enabled = false
}

configure<KtlintExtension> {
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}

// Same workaround as MonitorFish: ktlint cannot parse with Kotlin >= 2.4.20.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.1.0")
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    systemProperty("user.timezone", "UTC")
    testLogging {
        events("passed", "failed")
    }
}
