# Gradle Repository Configuration for SNAPSHOT Dependencies

## Overview

This document explains the critical importance of including `mavenLocal()` in the Gradle repository configuration to access the `druid-parser:1.2.28-SNAPSHOT` dependency.

## Why mavenLocal() is Required

### SNAPSHOT Dependencies
- **SNAPSHOT versions** are development/integration builds published to local repositories
- They are **NOT available in Maven Central**
- They must be **installed locally** or accessed via local Maven repository

### druid-parser:1.2.28-SNAPSHOT
- This is a SNAPSHOT version of the druid-parser library
- Located in `/Users/shunyun/workspace/java/druid/core`
- Must be built and installed to local Maven repository first

## Correct Repository Configuration

### Option 1: With mavenLocal() (Recommended)
```gradle
repositories {
    // Check local Maven repository first for SNAPSHOT dependencies
    mavenLocal()
    // Fall back to central repository for release dependencies
    mavenCentral()
}
```

### Option 2: With custom local repository path (Alternative)
```gradle
repositories {
    // Direct path to local Maven repository
    maven {
        url = uri("$System.env.HOME/.m2/repository")
    }
    mavenCentral()
}
```

**Note**: Option 1 (mavenLocal()) is preferred as it's the standard Gradle way to access the local Maven repository and is more portable across different environments.

## Where to Configure

### 1. In build.gradle (Project Level)
```gradle
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'com.aidvps:druid-parser:1.2.28-SNAPSHOT'
}
```

### 2. In settings.gradle (All Projects)
```gradle
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

### 3. In init.gradle (User Level - ~/.gradle/)
```gradle
allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
```

## Verification

### Check if dependency is available
```bash
# List installed SNAPSHOT versions
ls -la ~/.m2/repository/com/aidvps/druid-parser/

# Expected output: directories with 1.2.28-SNAPSHOT version
```

### Test Gradle build
```bash
# Clean build to verify dependency resolution
./gradlew clean build

# If dependency is missing, you will see:
# Could not find com.aidvps:druid-parser:1.2.28-SNAPSHOT
```

## Installing druid-parser to Local Maven

If the SNAPSHOT is not yet installed:

```bash
cd /Users/shunyun/workspace/java/druid/core
mvn clean install -DskipTests

# This installs to ~/.m2/repository/
# Gradle will then find it via mavenLocal()
```

## Implementation in Project Files

The following project files have been updated with the correct repository configuration:

### ✅ plan.md
```gradle
repositories {
    mavenLocal()
    mavenCentral()
}
```

### ✅ build.gradle.example
```gradle
repositories {
    mavenLocal()
    mavenCentral()
}
```

### ✅ quickstart.md
- Included in installation instructions
- Added warning about SNAPSHOT dependency requirement

### ✅ research.md
- Documented repository configuration requirement
- Explained why mavenLocal() is essential

### ✅ agent-context.md
- Added repository configuration to Build System section

## Common Issues

### Issue 1: Could not find SNAPSHOT version
**Error**: `Could not find com.aidvps:druid-parser:1.2.28-SNAPSHOT`

**Solution**:
1. Verify mavenLocal() is first in repository list
2. Install druid-parser to local Maven: `mvn clean install`
3. Check if installed: `ls ~/.m2/repository/com/aidvps/druid-parser/`

### Issue 2: Gradle not checking local repository
**Solution**: Add explicit `mavenLocal()` call
```gradle
repositories {
    mavenLocal()  // Explicit call (not just maven {})
    mavenCentral()
}
```

### Issue 3: Different Gradle version behavior
**Gradle 6+**: `mavenLocal()` automatically checks local Maven repo
**Gradle 5.x**: May need explicit configuration

## Best Practices

1. **Always include mavenLocal()** for SNAPSHOT dependencies
2. **Order matters**: `mavenLocal()` should be first
3. **Cache locally**: SNAPSHOTs change frequently, use local cache
4. **Document requirement**: Make it clear in README and documentation
5. **CI/CD**: Ensure CI also has access to SNAPSHOT dependencies

## Summary

| Configuration | Status |
|---------------|--------|
| mavenLocal() included | ✅ Required for SNAPSHOT |
| mavenCentral() included | ✅ For release dependencies |
| Correct order | ✅ mavenLocal() first |
| Documented | ✅ Across all files |

The `mavenLocal()` repository is **essential** for accessing `druid-parser:1.2.28-SNAPSHOT` and must be configured in all Gradle builds using this library.
