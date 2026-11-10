// Copyright Dit 2026
// SPDX-License-Identifier: BUSL-1.1

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

buildscript {
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:0.54.0")
    }
}

plugins {
    kotlin("jvm") version "2.4.21"
    id("com.github.ben-manes.versions") version("0.54.0")
    id("com.vanniktech.maven.publish") version("0.37.0")
    jacoco
}

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlinx")
}

val ktlint by configurations.creating

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.slf4j:slf4j-api:2.0.20")
    ktlint("com.pinterest.ktlint:ktlint-cli:1.8.0")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.4.2")
    testImplementation("io.mockk:mockk:1.14.11")
}

// Jar configuration
group = "dev.dit"
version = when(project.hasProperty("version")) {
    true -> project.property("version")!!
    false -> "latest"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Maven publishing configuration
//
// Releases are published to Maven Central (namespace dev.dit) by the
// com.vanniktech.maven.publish plugin, which also supplies the sources jar,
// javadoc jar and signatures that Central requires. Credentials and the
// signing key come from Gradle properties (ORG_GRADLE_PROJECT_* in CI); see
// DEVELOPING.md.
mavenPublishing {
    // Upload as a deployment that waits for a manual "Publish" click in the
    // Central Portal: anything released to Central is permanent.
    publishToMavenCentral()

    // Sign only when a key is supplied, so `publishToMavenLocal` works on a
    // developer machine or pull request build with no key. A release without
    // signatures cannot slip through: Central rejects unsigned deployments.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    // Kotlin sources have no javadoc; Central accepts an empty javadoc jar.
    configureBasedOnAppliedPlugins(
        javadocJar = JavadocJar.Empty(),
        sourcesJar = SourcesJar.Sources(),
    )

    coordinates("dev.dit", "command-executor", version.toString())

    pom {
        name.set("Dit Command Executor")
        description.set("Lightweight Kotlin/Java library for invoking shell commands, shared by the Dit server and its remote providers.")
        inceptionYear.set("2026")
        url.set("https://github.com/ditdotdev/command-executor")
        licenses {
            license {
                name.set("BUSL-1.1")
                url.set("https://github.com/ditdotdev/command-executor/blob/master/LICENSE")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("ditdotdev")
                name.set("Dit")
                url.set("https://github.com/ditdotdev")
            }
        }
        scm {
            url.set("https://github.com/ditdotdev/command-executor")
            connection.set("scm:git:https://github.com/ditdotdev/command-executor.git")
            developerConnection.set("scm:git:ssh://git@github.com/ditdotdev/command-executor.git")
        }
    }
}

// The dit-maven S3 bucket (served as https://maven.dit.dev) stays as a second
// publish target while the ecosystem migrates to Maven Central: the release
// tooling still verifies artifacts there. Remove it once every dev.dit
// artifact is released from Central.
val mavenBucket = when(project.hasProperty("mavenBucket")) {
    true -> project.property("mavenBucket")
    false -> "dit-maven"
}

publishing {
    repositories {
        maven {
            name = "dit"
            url = uri("s3://$mavenBucket")
            authentication {
                create<AwsImAuthentication>("awsIm")
            }
        }
    }
}

// Treat all warnings as errors
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

// Configuration for dependencyUpdates task to ignore release candidates
tasks.withType<DependencyUpdatesTask>().configureEach {
    resolutionStrategy {
        componentSelection {
            all { selection: ComponentSelection ->
                val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview", "b", "ea", "eap").any { qualifier ->
                    selection.candidate.version.matches(Regex("(?i).*[.-]$qualifier[.\\d-+]*"))
                }
                if (rejected) {
                    selection.reject("Release candidate")
                }
            }
        }
    }
}

// Enable ktlint checks and formatting
val ktlintTask = tasks.register<JavaExec>("ktlint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("src/**/*.kt")
}

tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Fix Kotlin code style deviations"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args("-F", "src/**/*.kt")
}

tasks.named("check").get().dependsOn(ktlintTask)

// Test configuration
tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        csv.required.set(true)
    }
}
