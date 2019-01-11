import com.jfrog.bintray.gradle.BintrayExtension
import groovy.util.Node
import groovy.util.NodeList
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun version(): String {
    val buildNumber = System.getProperty("BUILD_NUM")
    val version = "0.1" + if (buildNumber.isNullOrEmpty()) "-SNAPSHOT" else ".$buildNumber"
    println("building version $version")
    return version
}

val projectVersion = version()
val projectDescription = """KloudFormation Specification Generator"""

val kotlinVersion = "1.3.11"
val jacksonVersion = "2.9.8"
val kotlinpoetVersion = "1.0.1"
val junitVersion = "5.0.0"

val artifactId = "kloudformation-specification-generator"
group = "io.kloudformation"
version = projectVersion
description = projectDescription

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.11"
    id("org.jlleitschuh.gradle.ktlint") version "6.3.1"
    id("com.jfrog.bintray") version "1.8.4"
    `maven-publish`
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = jacksonVersion)
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)
    implementation(group = "com.squareup", name = "kotlinpoet", version = kotlinpoetVersion)

    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test-junit5", version = kotlinVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testImplementation(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion)
    testRuntime(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
}

bintray {
    user = "hexlabs-builder"
    key = System.getProperty("BINTRAY_KEY") ?: "UNKNOWN"
    setPublications("mavenJava")
    publish = true
    pkg(
            closureOf<BintrayExtension.PackageConfig> {
                repo = "kloudformation"
                name = artifactId
                userOrg = "hexlabsio"
                setLicenses("Apache-2.0")
                vcsUrl = "https://github.com/hexlabsio/kloudformation-specification-generator.git"
                version(closureOf<BintrayExtension.VersionConfig> {
                    name = projectVersion
                    desc = projectVersion
                })
            })
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifactId = artifactId
            artifact(sourcesJar.get())
            pom.withXml {
                val dependencies = (asNode()["dependencies"] as NodeList)
                configurations.compile.allDependencies.forEach {
                    dependencies.add(Node(null, "dependency").apply {
                        appendNode("groupId", it.group)
                        appendNode("artifactId", it.name)
                        appendNode("version", it.version)
                    })
                }
            }
        }
    }
}