import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    kotlin("jvm") version "1.7.20"
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.0.0"
}

group = "net.razvan"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.simpleframework:simple-xml:2.7.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

pluginBundle {
    website = "https://github.com/razvn/jacoco-to-cobertura-gradle-plugin"
    vcsUrl = "https://github.com/razvn/jacoco-to-cobertura-gradle-plugin"
    tags = listOf("jacoco", "cobertura", "report", "converter")
}

gradlePlugin {
    plugins {
        create("JacocoToCoberturaPlugin") {
            id = "net.razvan.jacoco-to-cobertura"
            displayName = "Converts jacoco xml reports to cobertura"
            description = "This plugin converts jacoco xml reports to cobertura. Make sure the `jacocoTestReport` runs before."
            implementationClass = "net.razvan.JacocoToCoberturaPlugin"
        }
    }
}

