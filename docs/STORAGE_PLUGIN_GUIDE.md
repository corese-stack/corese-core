# Storage Backend Plugin Guide

Corese discovers RDF storage backends through Java's standard
`ServiceLoader` mechanism. Most applications should use `Repositories`; the
storage API is intended for backend authors and advanced embedding scenarios.

## Using a backend

The default entry point creates an open in-memory backend:

```java
try (StorageManager storage = Storages.create()) {
    storage.mutations().add(statement);
}
```

Select another provider through its type:

```java
StorageConfig config = StorageConfig.builder()
        .type("custom")
        .property("url", "jdbc:postgresql://localhost/corese")
        .build();

try (StorageManager storage = Storages.create(config)) {
    // Use storage.queries(), mutations(), metadata(), or transactions().
}
```

`Storages.create(config)` initializes the selected backend. The caller owns the
returned manager and must close it. Applications that need SPARQL execution
should normally call `Repositories.create(config)` instead; the repository then
owns and closes the backend.

## Implementing a provider

A provider implements two contracts:

- `StorageManager` exposes query, mutation, metadata, transaction, and lifecycle
  capabilities;
- `StoragePlugin` identifies the provider, checks configurations, and creates an
  initialized manager.

`StoragePlugin.create` must return a non-null manager in the `RUNNING` lifecycle
state. Backend failures should use `StorageException` and its stable
`ErrorCode`. If transactions are unavailable, the transaction capability must
report `supportsTransactions() == false` and an empty isolation-level set.

Register the implementation in:

```text
src/main/resources/META-INF/services/fr.inria.corese.core.next.storage.api.plugin.StoragePlugin
```

The file contains the fully qualified provider class name:

```text
com.example.storage.CustomStoragePlugin
```

Package the service descriptor and implementation in the provider JAR, then put
that JAR on the application classpath or module path. Corese does not maintain a
global mutable plugin registry and does not load arbitrary JAR files at runtime.

## Selection rules

The configured `type` is normally matched by `StoragePlugin.supports`. When
multiple providers support a configuration, Corese selects the highest
priority; equal priorities are ordered by plugin name. Names must be unique and
non-blank.

The built-in providers are:

- `memory`: the default in-memory backend for tests and small datasets;
- `graph`: a compatibility adapter around the legacy Corese `Graph` backend,
  requiring `graph` and `valueFactory` configuration properties.

See `GraphStoragePlugin` and `MemoryStoragePlugin` for complete provider
implementations, and `docs/source/next_storage.md` for the public storage
contract.
