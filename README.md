# JacocoToCobertura Gradle Plugin

[![Current release](https://img.shields.io/github/v/release/razvn/jacoco-to-cobertura-gradle-plugin)](https://github.com/razvn/jacoco-to-cobertura-gradle-plugin/releases) [![Gradle Plugin Portal](https://img.shields.io/badge/Gradle-v1.3.0-blue.svg)](https://plugins.gradle.org/plugin/net.razvan.jacoco-to-cobertura)

This plugin aims to convert the JaCoCo XML report to a Cobertura report for GitLab to use the data for [showing the lines covered by tests in the Merge Request](https://docs.gitlab.com/ee/ci/testing/test_coverage_visualization.html).

This project adapts the python version [cover2cover](https://github.com/rix0rrr/cover2cover).

## How to use the plugin

### Add the plugin to your project

```kotlin
plugins {
    jacoco
    id("net.razvan.jacoco-to-cobertura") version "<see latest version above>"
}
```

### Configure the task

| Property               | Description                                                | Default Value |
|------------------------|------------------------------------------------------------|--|
| `inputFile`            | JaCoCo XML file to read                                    | XML report of single `JacocoReport` task found in the project; must be specified manually if zero or more than one `JacocoReport` task exists |
| `outputFile`           | Cobertura XML file to generate                             | `cobertura-${inputFile.nameWithoutExtension}.xml` in the directory of `inputFile` |
| `sourceDirectories`    | Directories containing source files the JaCoCo report used | Source directories of single `JacocoReport` task found in the project; must be specified manually if zero or more than one `JacocoReport` tasks exist |
| `splitByPackage`       | Whether to generate one Cobertura report per package       | `false` |
| `rootPackageToRemove`  | Root package to remove from the begging of filenames       | `` |

> **Note**
> Due to the way the default values are set, projects that apply either the [JaCoCo plugin](https://docs.gradle.org/userguide/jacoco_plugin.html#jacoco_plugin) or the [JaCoCo Report Aggregation plugin](https://docs.gradle.org/userguide/jacoco_report_aggregation_plugin.html), work without requiring custom configuration of the `inputFile`, `outputFile` and `sourceDirectories` properties.

Should the default values not work for you, you can configure the task manually. For example:
```kotlin
tasks.named<JacocoToCoberturaTask>(JacocoToCoberturaPlugin.TASK_NAME) {
    inputFile.set(layout.buildDirectory.file("reports/xml/coverage.xml"))
    outputFile.set(layout.buildDirectory.file("reports/xml/cobertura.xml"))
    sourceDirectories.from(layout.projectDirectory.dir("src/main/java"))
    splitByPackage.set(true)
    rootPackageToRemove.set("com.example")
}
```

### Run the task

Run the task `jacocoToCobertura`:
```shell
./gradlew jacocoToCobertura
```

To ensure the Cobertura report is generated whenever the JaCoCo task executes, the plugin task should be run after the `JacocoReport` task, e.g., `jacocoTestReport`. As the configuration table above shows, the default values should work in most cases. However, if your project works differently, you must manually ensure the report task runs before this plugin's conversion task. For example:

```kotlin
tasks.jacocoTestReport {
    finalizedBy(tasks.jacocoToCobertura)
}
```
or
```kotlin
tasks.koverXmlReport {
    finalizedBy(tasks.jacocoToCobertura)
}
```
