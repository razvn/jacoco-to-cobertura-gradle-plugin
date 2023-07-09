import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.0"
}

group = "net.razvan"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.simpleframework:simple-xml:2.7.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}

tasks.test {
    useJUnitPlatform()
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}

gradlePlugin {
    website.set("https://github.com/razvn/jacoco-to-cobertura-gradle-plugin")
    vcsUrl.set(website)

    plugins {
        create("JacocoToCoberturaPlugin") {
            id = "net.razvan.jacoco-to-cobertura"
            displayName = "Converts jacoco xml reports to cobertura"
            description = "This plugin converts jacoco xml reports to cobertura. Make sure the `jacocoTestReport` runs before."
            tags.set(listOf("jacoco", "cobertura", "report", "converter"))
            implementationClass = "net.razvan.JacocoToCoberturaPlugin"
        }
    }
}
