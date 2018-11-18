import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun version(): String{
    val buildNumber = System.getProperty("BUILD_NUM")
    val version = "0.1" + if(buildNumber.isNullOrEmpty()) "-SNAPSHOT" else ".$buildNumber"
    println("building version $version")
    return version
}

val projectVersion = version()
val projectDescription = """KloudFormation Specification Generator"""

val jacksonVersion = "2.9.7"
val kotlinpoetVersion = "0.6.0"
val kotlinVersion = "1.3.0"
val junitVersion = "5.0.0"
val gitPublishVersion = "5.0.0"

group = "io.kloudformation"
version = projectVersion
description = projectDescription


plugins {
    id("com.jfrog.bintray") version "1.8.4"
    kotlin("jvm") version "1.3.0"
    `maven-publish`
}

repositories {
    jcenter()
    mavenCentral()
}


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

bintray {
    user = "hexlabs-builder"
    key = System.getProperty("BINTRAY_KEY") ?: "UNKNOWN"
    setPublications("mavenJava")
    publish = true
    pkg(
    closureOf<BintrayExtension.PackageConfig> {
        repo = "kloudformation"
        name = "kloudformation-specification-generator"
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
            artifact(sourcesJar.get())
            pom.withXml {
                asNode().appendNode("dependencies").let { depNode ->
                    configurations.compile.allDependencies.forEach {
                        depNode.appendNode("dependency").apply {
                            appendNode("groupId", it.group)
                            appendNode("artifactId", it.name)
                            appendNode("version", it.version)
                        }
                    }
                }
            }
        }
    }
}