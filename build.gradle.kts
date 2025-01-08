import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    alias(libs.plugins.plugin.publish)
}

group = "net.razvan"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.simple.xml)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlin.gradle.plugin)

    runtimeOnly(libs.kotlin.reflection)

    testImplementation(platform(libs.http4k.bom))
    testImplementation(libs.junit)
    testImplementation(libs.testing.approval)

    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

gradlePlugin {
    website.set("https://github.com/razvn/jacoco-to-cobertura-gradle-plugin")
    vcsUrl.set(website)

    plugins {
        create("JacocoToCoberturaPlugin") {
            id = "net.razvan.jacoco-to-cobertura"
            displayName = "Converts jacoco xml reports to cobertura"
            description = "This plugin converts jacoco xml reports to cobertura. Make sure the `jacocoTestReport` runs before."
            tags = listOf("jacoco", "cobertura", "report", "converter")
            implementationClass = "net.razvan.JacocoToCoberturaPlugin"
        }
    }
}
