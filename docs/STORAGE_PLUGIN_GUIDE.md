# Storage Backend Plugin Guide

## Overview

Corese uses a **plugin system** for storage backends, 
allowing you to extend the framework with custom storage implementations without modifying the core codebase.

**Currently available backends:**
- **`graph`** - Production-ready Corese Graph backend (priority: 100)
- **`memory`** - In-memory HashMap storage for testing (priority: 50)
- **`demo`** - Example external plugin (priority: 30)
- **Custom plugins** - Create your own

---

## Quick Start

### Using Built-in Backends

The simplest way to work with different storage backends is through `ModelFactory`:

```java
import fr.inria.corese.core.next.data.factory.ModelFactory;
import fr.inria.corese.core.next.data.api.Model;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;

// Create factory
ValueFactory valueFactory = new CoreseAdaptedValueFactory();
ModelFactory factory = new ModelFactory(valueFactory);

// Use built-in backends
Model memoryModel = factory.createMemoryModel();  // Fast in-memory storage
Model graphModel = factory.createGraphModel();    // Production Graph backend
```

### Using External Plugins

```java
import fr.inria.corese.core.next.storage.api.plugin.ExternalPluginLoader;
import java.io.File;

// 1. Load external plugin JAR
File pluginJar = new File("plugins/demo-storage-plugin.jar");
ExternalPluginLoader.loadPluginsFromJar(pluginJar);

// 2. Use it like built-in plugins
Model demoModel = factory.createModel("demo");
```

---

## Using External Plugins

### Loading Plugin JARs

#### Single JAR

```java
File jar = new File("plugins/custom-storage-plugin.jar");
ExternalPluginLoader.loadPluginsFromJar(jar);
```

#### From Classpath Resources

```java
URL resourceUrl = getClass().getClassLoader()
    .getResource("storage-plugin/demo-storage-plugin.jar");
File jar = new File(resourceUrl.toURI());
ExternalPluginLoader.loadPluginsFromJar(jar);
```

### Directory Structure

**For Development/Testing:**
```
corese-core/
└── src/test/resources/
    └── storage-plugin/
        ├── demo-storage-plugin.jar
        └── test-plugins.jar
```

---

## Creating Your Own Plugin

### Step 1: Project Structure

Create a new Java project with the following structure:

```
custom-plugin/
├── build.gradle
└── src/main/
    ├── java/com/example/plugin/
    │   ├── CustomStoragePlugin.java
    │   └── CustomStorageManager.java
    └── resources/META-INF/services/
        └── fr.inria.corese.core.next.storage.api.plugin.StoragePlugin
```


### Step 2: Implement StorageManager

```java
public class CustomStorageManager implements StorageManager {
    
}
```

### Step 3: Create StoragePlugin

```java
public class CustomStoragePlugin implements StoragePlugin {
}
```

### Step 4: Register via Service Provider Interface (SPI)

Create file:
```
src/main/resources/META-INF/services/fr.inria.corese.core.next.storage.api.plugin.StoragePlugin
```

Content (fully qualified class name):
```
com.example.plugin.CustomStoragePlugin
```

### Step 5: Build Plugin JAR

**Gradle:**
```bash
./gradlew build
# Output: build/libs/custom-storage-plugin-1.0.0.jar
```

### Step 6: Use Your Plugin

```java
// Load plugin
File jar = new File("plugins/custom-storage-plugin-1.0.0.jar");
ExternalPluginLoader.loadPluginsFromJar(jar);

// Configure
StorageConfig config = StorageConfig.builder()
    .property("type", "custom")
    .property("url", "jdbc:postgresql://localhost/mydb")
    .property("username", "postgres")
    .property("password", "secret")
    .build();

// Create model
ModelFactory factory = new ModelFactory(valueFactory);
Model customModel = factory.createModel(config);

```

---

## Plugin Architecture

### Plugin Discovery Flow

```
1. Application calls ExternalPluginLoader.loadPluginsFromJar(jar)
   ↓
2. ExternalPluginLoader reads META-INF/services/StoragePlugin
   ↓
3. Plugin class loaded via custom ClassLoader
   ↓
4. Plugin registered with StoragePluginManager
   ↓
5. Plugin available via factory.createModel("plugin-name")
```

---

## Additional Resources

- **Source Code**: See `GraphStoragePlugin` and `MemoryStoragePlugin` for reference implementations
- **Demo Plugin**: Check `demo-storage-plugin` project for a complete working example

---
