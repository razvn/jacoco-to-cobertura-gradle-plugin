<h2 class="github">Changelog</h2>

This list is not intended to be all-encompassing - it will document major and breaking changes with their rationale when appropriate:

### v3.0.0
- Split mode [Breaking]: per-package reports are now written as `<package>.xml` inside a `<output file base>-packages/` directory next to the output file. Before, each report sat next to the output file as `<output file base>-<package>.xml`. Point GitLab CI at the new paths.
- When `splitByPackage` is on, the combined report is also written to `outputFile`, so the task can be up-to-date and use the build cache. Package files left behind by earlier runs are removed.
- Requires Gradle 9 or newer, running on JDK 21 or newer (the plugin ships Java 21 bytecode)
- Removed the legacy SimpleXML implementation; all conversion uses Jackson
- The plugin no longer adds the Kotlin Gradle plugin to consumers' build classpath
- Dependency updates: Gradle 9.9.1, plugin-publish 2.2.1, Kotlin 2.4.21, Jackson 2.23, JUnit 6

### v2.0.0
- Replace XML library with Jackson for better performance(?) and maintainability
- Fixed a number of issues with the XML generated that now is DTD compliant
- Update to gradle version catalogs


### v1.3.2
- fix #11: report.dtd not found

### v1.3.1
- Config option `rootPackageToRemove` is optional H/T @eliezio

### v1.3.0
- Upgrade to gradle 8.12 & lib updates H/T @eliezio
- Added new config option `rootPackageToRemove` to strip the root package from the filename name in the cobertura report. (useful for kotlin root classe that are not in a package directory)
- Fix: filename incorrect path separator (from `.` to `/`)

### v1.2.0
- Cleaned-up and modernized. H/T @bddckr
- configuration change [Breaking]: see readme for details

### v1.1.2
- Remove debugging prints forgot
 
### v1.1.1
- Add verbose option that prints some output when set to true otherwise if everything runs ok no output is produced
- Creates the output directories needed (if possible).
- Add support for Kotlin Android sourceSet. H/T @fadookie

### v1.1.0
- Add split by package property to split in multiple xml files (to avoid Gitlab 10MB limit per file). H/T @DrewCarlson
- Fix: replace getByName with findByName for avoiding exception in KMM project. H/T @DrewCarlson

### v1.0.2
- Fix: support sources from kotlin multiplatform projects

### v1.0.1
- Fix: multiple sourceinfo entries support in the jacoco report
 
### v1.0.0
- Initial major release
- configuration change  [Breaking]: `intputFile` and `outputFile` now take a File as param instead of string path

### v0.9.1
- Initial preview release
