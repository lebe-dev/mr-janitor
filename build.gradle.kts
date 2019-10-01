import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    id("io.gitlab.arturbosch.detekt") version "1.0.1"
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

group = "ru.lebe.dev.mrjanitor"
version = "1.0.0-BETA"

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
    maven("https://oss.jfrog.org/artifactory/oss-snapshot-local/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.github.ajalt:clikt:2.1.0")

    implementation("io.arrow-kt:arrow-core-data:0.9.0")

    implementation("com.typesafe:config:1.3.3")

    implementation("commons-codec:commons-codec:1.12")
    
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.slf4j:slf4j-api:1.7.25")

    testImplementation("org.junit.jupiter:junit-jupiter-params:5.5.2")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.5.2")

    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

detekt {
    toolVersion = "1.0.1"

    input = project.files("src/main/kotlin")

    config = files("detekt.yml")

    reports {
        html {
            enabled = true
            destination = File("detekt.html")
        }
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("janitor")
        mergeServiceFiles()
        manifest {
            attributes["Main-Class"] = "ru.lebe.dev.mrjanitor.App"
        }
    }
}
