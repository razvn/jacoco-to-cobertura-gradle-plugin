# JacocoToCobertura Gradle Plugin

![Current release](https://img.shields.io/github/v/release/razvn/jacoco-to-cobertura-gradle-plugin)
[![Gradle Plugin Portal](https://img.shields.io/badge/Gradle-v1.1.1-blue.svg)](https://plugins.gradle.org/plugin/net.razvan.jacoco-to-cobertura)

The aim of the plugin is to convert the Jacoco XML report to Cobertura report in order for GitLab to use the infos 
for [showing the lines covered by tests in the Merge Request](https://docs.gitlab.com/ee/ci/testing/test_coverage_visualization.html).

The project is an adaptation of the python version [cover2cover](https://github.com/rix0rrr/cover2cover).

## How to use the plugin

### Add the plugin to your project
```kotlin
plugins {
    jacoco
    id("net.razvan.jacoco-to-cobertura") version "1.1.1"
}
```

### Configure the plugin

Options:
- `inputFile` (required): location of Jacoco XML location
- `outputFile`: location file the Cobertura report will be generated
- `splitByPackage`: to generate multiple Cobertura reports split by packages
- `verbose`: show some output information or not when the task runs

You must set at least the `inputFile` to the location where Jacoco XML report can be found.

If you set an `outoutFile` the Cobertura generated report will be generated there otherwise the default will be in the 
same directory as the `inputFile` with a `cobertura-` prefix. 
If the directory `outputFile`'s directory or parent directory does not exist, they wil be created (if possible).

If you set `splitByPackage` property to `true` it will generate multiple reports files by package name.

If `verbose` is set to true some output infos will be printed out (can be useful for debugging), otherwise no output.

```kotlin
jacocoToCobertura {
    inputFile.set(file("$buildDir/reports/xml/coverage.xml"))
    outputFile.set(file("$buildDir/reports/xml/cobertura.xml"))
}
```
(in this exemple if you don't set an `outputFile` the default one will be: `$buildDir/reports/xml/cobertura-coverage.xml`)

Adding the property:
```kotlin
jacocoToCobertura {
    splitByPackage.set(true)
}
```
will generate multiple output files by the package name.

Adding the property:
```kotlin
jacocoToCobertura {
    verbose.set(true)
}
```
will produce some output to the console (confirming the task run, its configuration, ...) otherwise no output will be generated.

### Run the plugin
Run the task: `jacocoToCobertura`. The task should be run after `jacocoTestReport` in order to have the JacocoReport generated.
```shell
./gradlew jacocoToCobertura
```

or just set it after `jacocoTestReport`
```kotlin
tasks.jacocoTestReport {
    finalizedBy(tasks.jacocoToCobertura)
}
```
or `koverXmlReport`
```kotlin
tasks.koverXmlReport {
    finalizedBy(tasks.jacocoToCobertura)
}
```

