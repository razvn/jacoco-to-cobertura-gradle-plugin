<h2 class="github">Changelog</h2>

This list is not intended to be all-encompassing - it will document major and breaking changes with their rationale when appropriate:

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
