# CLAUDE.md - AI Assistant Development Guide

This document provides comprehensive guidance for AI assistants working on the JaCoCo to Cobertura Gradle Plugin project.

## Project Overview

**Project**: JaCoCo to Cobertura Gradle Plugin
**Version**: 2.0.0
**Language**: Kotlin 2.2.0
**Build System**: Gradle 8.12
**License**: MIT

### Purpose

Converts JaCoCo XML coverage reports to Cobertura XML format for GitLab's test coverage visualization in merge requests. This enables GitLab to show which lines are covered by tests directly in merge requests.

### Key Features

- Automatic JaCoCo report detection and conversion
- Package-based splitting to avoid GitLab's 10MB file limit
- Root package stripping for cleaner file paths (useful for Kotlin projects)
- Auto-configuration when JaCoCo plugin is detected
- Kover compatibility (with limitations)

## Codebase Structure

```
jacoco-to-cobertura-gradle-plugin/
├── src/
│   ├── main/kotlin/
│   │   ├── JacocoToCoberturaPlugin.kt      # Main plugin class (140 lines)
│   │   ├── J2CJackson.kt                   # XML processor using Jackson (current)
│   │   ├── J2CSimpleXML.kt                 # Legacy XML processor (deprecated)
│   │   ├── models/                         # Data models directory
│   │   │   ├── JacocoModels.kt            # JaCoCo XML structure
│   │   │   ├── CoberturaModels.kt         # Cobertura XML structure
│   │   │   └── CounterTypes.kt            # Coverage counter constants
│   │   └── models_simplexml.kt            # Legacy SimpleXML models
│   └── test/
│       ├── kotlin/
│       │   ├── JacocoToCoberturaPluginTest.kt    # Plugin registration tests
│       │   ├── JacocoToCoberturaTaskTest.kt      # Task integration tests
│       │   ├── J2CJacksonTest.kt                 # XML processing tests
│       │   ├── JacocoJKTest.kt                   # Approval/snapshot tests
│       │   ├── JacocoModelsTest.kt               # Model parsing tests
│       │   ├── CoberturaModelsTest.kt            # Cobertura model tests
│       │   └── ClassElementTest.kt               # Class element tests
│       └── resources/
│           ├── jacoco-sample*.xml          # Test fixtures (4 samples)
│           ├── c2c.py                      # Python reference impl
│           └── net/razvan/*.approved       # Approval test baselines
├── gradle/
│   ├── libs.versions.toml                  # Version catalog (centralized deps)
│   └── wrapper/                            # Gradle wrapper
├── .github/workflows/
│   └── gradle.yml                          # CI/CD pipeline
├── build.gradle.kts                        # Build configuration
├── settings.gradle.kts                     # Project settings
├── gradle.properties                       # Version and properties
├── README.md                               # User documentation
├── CHANGELOG.md                            # Version history
└── LICENSE.md                              # MIT License
```

## Core Architecture

### Plugin Registration Flow

1. **Plugin Application** (`JacocoToCoberturaPlugin.apply()`):
   - Registers the `jacocoToCobertura` task
   - Sets default output file based on input file location
   - Detects JaCoCo plugin presence
   - Auto-configures task dependencies and properties

2. **Task Execution** (`JacocoToCoberturaTask.convert()`):
   - Validates input file exists
   - Creates output directory if needed
   - Loads JaCoCo XML using Jackson
   - Transforms data to Cobertura format
   - Writes output (single file or split by package)

### Data Flow

```
JaCoCo XML → J2CJackson.loadJacocoData() → JacocoModels
           ↓
JacocoModels → J2CJackson.transformData() → CoberturaModels
           ↓
CoberturaModels → J2CJackson.writeCoberturaData() → Cobertura XML
```

## Development Workflow

### Prerequisites

- Java 11+ (runtime compatibility)
- JDK 21 (for development/CI)
- Gradle 8.12 (via wrapper)

### Common Commands

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Run tests with approval updates (when intentionally changing output)
./gradlew test --rerun-tasks

# Publish to local Maven repository (for testing)
./gradlew publishToMavenLocal

# Publish to Gradle Plugin Portal (requires credentials)
./gradlew publishPlugins
```

### Testing Workflow

1. **Unit Tests**: Test individual components in isolation
2. **Approval Tests**: Compare XML output against approved baselines
3. **Integration Tests**: Test full task execution with real files

### CI/CD Pipeline

**GitHub Actions** (`.github/workflows/gradle.yml`):
- Triggers on push and pull requests
- Uses Ubuntu latest with JDK 21
- Runs `./gradlew build --no-daemon`
- Uploads test reports as artifacts

## Key Conventions and Patterns

### Code Style

- **Kotlin Official Code Style** (enforced via `.idea/codeStyles/`)
- **Package**: `net.razvan` (models in `net.razvan.models`)
- **Task Name**: `jacocoToCobertura` (constant in companion object)

### Naming Conventions

- **Classes**: PascalCase (e.g., `JacocoToCoberturaPlugin`)
- **Properties**: camelCase (e.g., `inputFile`, `splitByPackage`)
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `TASK_NAME`)
- **Functions**: camelCase (e.g., `loadJacocoData()`)

### Kotlin Patterns

1. **Data Classes**: All models are immutable data classes
2. **Property Delegates**: Use `lazy` for expensive computations
3. **Extension Functions**: For utility operations
4. **Null Safety**: Proper use of nullable types with `?` and `!!`
5. **Default Parameters**: Prefer defaults over overloads
6. **Smart Casts**: Leverage Kotlin's type system

### Gradle Task Configuration

- Use **Gradle Provider API** for lazy evaluation
- Declare inputs with `@InputFile`, `@InputFiles`
- Declare outputs with `@OutputFile`
- Use `@Optional` for optional properties
- Set task group to `LifecycleBasePlugin.VERIFICATION_GROUP`

### Error Handling

- Custom exception: `JacocoToCoberturaException`
- Validate files exist before processing
- Create output directories with proper error messages
- Use `try-catch` with meaningful error context

## Dependency Management

**Version Catalog** (`gradle/libs.versions.toml`):

```toml
[versions]
jackson = "2.19.0"          # Primary XML processor
kotlin = "2.2.0"            # Kotlin language
junit = "5.13.1"            # Testing framework
simplexml = "2.7.1"         # Legacy (deprecated)

[bundles]
jackson = ["jackson-databind", "jackson-dataformat", "jackson-kotlin", "woodstox"]
```

### Key Dependencies

**Production**:
- `com.fasterxml.jackson.*:2.19.0` - XML processing (current)
- `org.simpleframework:simple-xml:2.7.1` - Legacy (to be removed)
- `org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0`
- `com.fasterxml.woodstox:woodstox-core:7.1.1` - XML parser

**Testing**:
- `org.junit.jupiter:junit-jupiter:5.13.1` - Test framework
- `org.http4k:http4k-testing-approval:6.15.0.1` - Approval testing

## Testing Guidelines

### Test Organization

1. **Plugin Tests** (`JacocoToCoberturaPluginTest.kt`):
   - Plugin application
   - Task registration
   - Auto-configuration

2. **Task Tests** (`JacocoToCoberturaTaskTest.kt`):
   - File I/O with `@TempDir`
   - Error conditions
   - Configuration options

3. **XML Processing Tests** (`J2CJacksonTest.kt`):
   - Parsing errors
   - Malformed XML
   - Edge cases

4. **Approval Tests** (`JacocoJKTest.kt`):
   - Regression testing
   - XML output validation
   - Baseline comparisons

### Approval Testing

- **Framework**: http4k-testing-approval
- **Baselines**: `src/test/resources/net/razvan/*.approved`
- **Update**: Delete `.approved` files and run tests to regenerate

### Test Fixtures

- `jacoco-sample.xml` - Basic coverage report
- `jacoco-sample2.xml` - Multiple packages
- `jacoco-sample3.xml` - Complex scenarios
- `jacoco-sample4.xml` - Edge cases
- `c2c.py` - Python reference implementation

## Configuration Reference

### Task Properties

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `inputFile` | `RegularFileProperty` | Conditional | Auto-detected from JacocoReport | JaCoCo XML file to read |
| `outputFile` | `RegularFileProperty` | No | `cobertura-{input}.xml` in same dir | Cobertura XML output file |
| `sourceDirectories` | `ConfigurableFileCollection` | Conditional | Auto-detected from JacocoReport | Source directories for file mapping |
| `splitByPackage` | `Property<Boolean>` | No | `false` | Split output by package (for large reports) |
| `rootPackageToRemove` | `Property<String>` | No | `""` | Root package to strip from filenames |

### Configuration Examples

**Basic** (auto-configured):
```kotlin
plugins {
    jacoco
    id("net.razvan.jacoco-to-cobertura") version "2.0.0"
}
```

**Manual configuration**:
```kotlin
tasks.named<JacocoToCoberturaTask>(JacocoToCoberturaPlugin.TASK_NAME) {
    inputFile.set(layout.buildDirectory.file("reports/xml/coverage.xml"))
    outputFile.set(layout.buildDirectory.file("reports/xml/cobertura.xml"))
    sourceDirectories.from(layout.projectDirectory.dir("src/main/java"))
    splitByPackage.set(true)
    rootPackageToRemove.set("com.example")
}
```

**Conditional execution**:
```kotlin
tasks.named<JacocoToCoberturaTask>(JacocoToCoberturaPlugin.TASK_NAME) {
    val jacocoReport = project.layout.buildDirectory.file("reports/jacoco.xml")
    onlyIf { jacocoReport.get().asFile.exists() }
    inputFile.set(jacocoReport)
}
```

**Task dependency**:
```kotlin
tasks.jacocoTestReport {
    finalizedBy(tasks.jacocoToCobertura)
}
```

## Important Implementation Details

### XML Processing Migration (v2.0.0)

- **Current**: Jackson (J2CJackson.kt) - DTD compliant, better performance
- **Legacy**: SimpleXML (J2CSimpleXML.kt) - deprecated, kept for reference
- **DO NOT** add new features to SimpleXML implementation

### Line Attribution Algorithm

The plugin associates source lines with methods by analyzing line numbers from JaCoCo. This is critical for accurate coverage mapping but has limitations with Kover (method line numbers missing).

### Package Path Handling

- Converts package notation: `com.example.foo` → `com/example/foo`
- Strips root packages when configured: `com.example.Foo` → `Foo` (if removing `com.example`)
- Handles Kotlin files without package directories

### Coverage Calculation

- **Line Coverage**: `covered / (covered + missed)`
- **Branch Coverage**: Same formula on branch counters
- **Complexity**: Uses cyclomatic complexity from JaCoCo

## Common Development Tasks

### Adding a New Configuration Property

1. Add abstract property to `JacocoToCoberturaTask`:
   ```kotlin
   @get:Input
   abstract val myProperty: Property<String>
   ```

2. Set default in plugin's `apply()`:
   ```kotlin
   coberturaTask.configure {
       myProperty.convention("defaultValue")
   }
   ```

3. Use in `convert()`:
   ```kotlin
   val myValue = myProperty.getOrElse("fallback")
   ```

4. Document in README.md configuration table
5. Add tests in `JacocoToCoberturaTaskTest.kt`

### Modifying XML Output Format

1. Update `CoberturaModels.kt` data classes
2. Modify `J2CJackson.transformData()` conversion logic
3. Update `J2CJackson.writeCoberturaData()` if needed
4. Run approval tests: `./gradlew test`
5. Review `.approved` file changes (or regenerate by deleting them)
6. Commit new baselines if changes are intentional

### Adding Test Fixtures

1. Add JaCoCo XML to `src/test/resources/jacoco-sample*.xml`
2. Create corresponding test in `JacocoJKTest.kt`
3. Run test to generate `.approved` baseline
4. Verify output correctness
5. Commit both fixture and baseline

### Updating Dependencies

1. Edit `gradle/libs.versions.toml`
2. Update version numbers
3. Run `./gradlew build` to verify compatibility
4. Run `./gradlew test` to ensure tests pass
5. Update CHANGELOG.md

## Troubleshooting Guide

### Common Issues

**Issue**: "File does not exist" error
- **Cause**: JaCoCo report not generated before conversion
- **Solution**: Add `dependsOn(tasks.jacocoTestReport)` or use `finalizedBy`

**Issue**: Multiple JacocoReport tasks found
- **Cause**: Plugin can't auto-detect which report to use
- **Solution**: Manually configure `inputFile` and `sourceDirectories`

**Issue**: Approval tests failing
- **Cause**: XML output changed (intentionally or bug)
- **Solution**:
  - If intentional: Delete `.approved` files, rerun, commit new baselines
  - If bug: Fix transformation logic in `J2CJackson.kt`

**Issue**: GitLab 10MB file limit exceeded
- **Cause**: Large monorepo with extensive coverage
- **Solution**: Set `splitByPackage.set(true)` to split by package

**Issue**: Incorrect file paths in Cobertura
- **Cause**: Root package includes directory structure
- **Solution**: Set `rootPackageToRemove.set("com.example")` to strip prefix

**Issue**: Kover compatibility issues
- **Cause**: Kover XML format differs from JaCoCo (missing method line numbers)
- **Solution**: Accept limitations or switch to JaCoCo plugin

## Version History Highlights

### v2.0.0 (Current)
- Migrated to Jackson for XML processing
- Fixed DTD compliance issues
- Gradle version catalogs

### v1.3.0
- Added `rootPackageToRemove` configuration
- Fixed filename path separators
- Gradle 8.12 upgrade

### v1.2.0
- Configuration modernization (breaking changes)
- Code cleanup

### v1.1.0
- Added `splitByPackage` for large reports
- Kotlin Multiplatform support

### v1.0.0
- Initial stable release
- File-based configuration (breaking change)

## Key Files Reference

### Must-Read Files

1. `src/main/kotlin/JacocoToCoberturaPlugin.kt` - Entry point, understand plugin lifecycle
2. `src/main/kotlin/J2CJackson.kt` - Core transformation logic
3. `src/main/kotlin/models/JacocoModels.kt` - JaCoCo data structure
4. `src/main/kotlin/models/CoberturaModels.kt` - Cobertura data structure
5. `build.gradle.kts` - Build configuration and publishing
6. `gradle/libs.versions.toml` - Dependency versions
7. `README.md` - User-facing documentation
8. `CHANGELOG.md` - Breaking changes and migration guides

### Can Modify Freely

- Test files in `src/test/kotlin/`
- Test resources in `src/test/resources/`
- Documentation files (README.md, CHANGELOG.md)

### Modify With Caution

- `JacocoToCoberturaPlugin.kt` - Breaking changes affect users
- `J2CJackson.kt` - XML output changes require approval test updates
- Model files - Changes affect data structure and compatibility
- `build.gradle.kts` - Affects build and publishing

### Do Not Modify

- `J2CSimpleXML.kt` - Legacy, deprecated, will be removed
- `models_simplexml.kt` - Legacy, deprecated
- Gradle wrapper files (unless upgrading Gradle)
- `.approved` files (unless intentionally updating baselines)

## Git Workflow Notes

**Branch Strategy**:
- Development on feature branches prefixed with `claude/`
- Branch names end with session ID for tracking
- Main branch for releases

**Commit Guidelines**:
- Clear, concise messages explaining "why" not "what"
- Reference issue numbers when applicable
- Group related changes in single commits

**Push Requirements**:
- Always use `git push -u origin <branch-name>`
- Branch must start with `claude/` and match session ID
- Retry on network failures (up to 4 times with exponential backoff)

## Best Practices for AI Assistants

1. **Read Before Modifying**: Always read files before suggesting changes
2. **Test After Changes**: Run `./gradlew test` after modifications
3. **Approval Test Updates**: Only update when output changes are intentional
4. **Documentation**: Update README.md and CHANGELOG.md for user-facing changes
5. **Backward Compatibility**: Maintain compatibility unless version bump justifies breaking changes
6. **Error Messages**: Provide actionable, user-friendly error messages
7. **Logging**: Use appropriate log levels (debug, lifecycle, error)
8. **Null Safety**: Leverage Kotlin's null safety, avoid `!!` when possible
9. **Immutability**: Prefer immutable data structures (data classes, val)
10. **Testing**: Add tests for new features and bug fixes

## Additional Resources

- **Gradle Plugin Development**: https://docs.gradle.org/current/userguide/custom_plugins.html
- **Jackson XML**: https://github.com/FasterXML/jackson-dataformat-xml
- **JaCoCo Format**: https://www.jacoco.org/jacoco/trunk/doc/
- **Cobertura Format**: https://github.com/cobertura/cobertura
- **GitLab Coverage**: https://docs.gitlab.com/ee/ci/testing/test_coverage_visualization.html
- **Kotlin Style Guide**: https://kotlinlang.org/docs/coding-conventions.html

---

**Last Updated**: 2026-01-17
**Plugin Version**: 2.0.0
**Maintainer**: razvn (https://github.com/razvn)
