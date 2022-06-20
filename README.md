# JacocoToCobertura Gradle Plugin

The aim of the plugin is to convert the Jacoco XML report to Cobertura report in order for GitLab to use the infos 
for showing the lines covered by tests in the Merge Request.

The project is an adaptation of the python version [cover2cover](https://github.com/rix0rrr/cover2cover).

## How to use the plugin

### Add the plugin to your project
```kotlin
plugins {
    jacoco
    id("net.razvan.jacoco-to-cobertura") version "0.x.x"
}
```

### Configure the plugin
You must set at least the `inputFile` to the location where Jacoco XML report can be found.
If you set an `outoutFile` the Cobertura generated report will be generated there otherwise the default will be in the 
same directory as the `inputFile` with a `cobertura-`

```kotlin
jacocoToCobertura {
    inputFile.set("./build/reports/xml/coverage.xml")
    outputFile.set("./build/reports/xml/cobertura.xml")
}
```
(in this exemple if you don't set an `outputFil` the default one will be: `./build/reports/xml/cobertura-coverage.xml`)

### Run the plugin
Run the task: `jacocoToCobertura`. The task should be run after `jacocoTestReport` in order to have the JacocoReport generated.
```shell
./gradlew jacocoToCobertura
```
