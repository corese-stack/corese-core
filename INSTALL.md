# Installation Guide for Corese-Core

This document provides step-by-step instructions to install and build **Corese-Core** from source.

## Prerequisites

Before installing Corese-Core, make sure you have the following installed:

- **Java 21** or higher  
  → Check with: `java -version`
- **Git** (to clone the repository)
- **Gradle 8+** (optional, recommended)  
  → If not installed, the Gradle Wrapper (`./gradlew`) will be used.
- **Internet access** (to fetch dependencies)

## Clone the Repository

```bash
git clone https://github.com/corese-stack/corese-core.git
cd corese-core
```

## Build Corese-Core

You can build the project using the Gradle wrapper:

```bash
./gradlew build
```

This will:

- Compile all modules
- Run tests
- Generate the main JAR file (in `build/libs/`)
- Publish the library to the local Maven repository if needed

If you're only interested in building without tests:

```bash
./gradlew assemble
```

## 🧪 Run Tests

```bash
./gradlew test
```

You can view the test reports in:

```text
corese-core/build/reports/tests/test/index.html
```

## 📦 Publish to Local Maven (optional)

To publish Corese-Core locally for use in other modules (like `corese-gui`, `corese-server`, etc.):

```bash
./gradlew publishToMavenLocal
```

The artifact will be installed under:

```text
~/.m2/repository/fr/inria/corese/corese-core/
```

## 🧼 Clean Build

```bash
./gradlew clean
```

---

## 🆘 Troubleshooting

- ❌ *Gradle not found?* → Use `./gradlew` instead of `gradle`
- ❌ *Java version too low?* → Corese requires Java 21+. You can install it via SDKMAN, Homebrew, or your package manager.
- ❌ *Tests failing due to RDF line endings or hashes?* → Make sure to normalize line endings (`\n`) and verify data hashes if you're running tests on Windows.
