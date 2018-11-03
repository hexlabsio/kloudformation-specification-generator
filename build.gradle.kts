import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar

plugins {
    kotlin("jvm") version "1.3.0"
    `maven-publish`
}

repositories {
    jcenter()
    mavenCentral()
}

group = "io.kloudformation"
version = "1.0-SNAPSHOT"
description = """KloudFormation Specification Generator"""

val jacksonVersion = "2.9.7"
val kotlinpoetVersion = "0.6.0"
val kotlinVersion = "1.3.0"
val junitVersion = "5.0.0"
val gitPublishVersion = "5.0.0"

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = kotlinVersion)
    compile(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)
    compile(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion)
    compile(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = jacksonVersion)
    compile(group = "com.squareup", name = "kotlinpoet", version = kotlinpoetVersion)

    testCompile(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testCompile(kotlin("test-junit5"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}