# Repository API

`Repositories` is the public entry point for creating Corese repositories. A
repository returned by this class is already open and ready to execute SPARQL.
No implementation import or explicit initialization is required.

## Common usage

```java
try (Repository repository = Repositories.create()) {
    repository.update("INSERT DATA { <urn:s> <urn:p> <urn:o> }");

    boolean present = repository.ask("ASK { <urn:s> <urn:p> <urn:o> }");

    try (TupleQueryResult result = repository.select(
            "SELECT ?s WHERE { ?s <urn:p> <urn:o> }")) {
        result.forEach(System.out::println);
    }
}
```

`Repositories.create()` selects the standard in-memory backend. The repository
owns that backend and closes it when the repository is closed. `close()` is
idempotent, and closing a repository invalidates all its existing connections.

The `select` and `construct` shortcuts materialize their complete result before
returning it. They are convenient and connection-safe, but their memory usage
grows with the result size. `ask` and `update` do not materialize a result.

## Progressive and configured queries

Use a connection when results must be consumed progressively or when an
operation needs bindings, a dataset, or execution options:

```java
try (Repository repository = Repositories.create();
     RepositoryConnection connection = repository.getConnection();
     TupleQueryResult result = connection
             .prepareTupleQuery(QueryLanguage.SPARQL,
                     "SELECT ?s WHERE { ?s ?p ?o }")
             .evaluate()) {
    while (result.hasNext()) {
        System.out.println(result.next());
    }
}
```

Closing a connection never closes its repository. Closing the repository makes
all its connections unusable.

## Custom storage

A storage plugin can be selected with a public `StorageConfig`:

```java
StorageConfig config = StorageConfig.builder()
        .type("my-storage-plugin")
        .property("path", dataPath)
        .build();

try (Repository repository = Repositories.create(config)) {
    // query the configured backend
}
```

Advanced integrations can provide a `StorageManager` directly. Ownership is
transferred to the repository: an uninitialized manager is initialized, an
already-running manager is adopted, and closing the repository shuts it down.

```java
try (Repository repository = Repositories.create(storageManager, config)) {
    // use the custom manager through the repository API
}
```
