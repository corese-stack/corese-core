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

The `select` and `graph` shortcuts materialize their complete result before
returning it. They are convenient and connection-safe, but their memory usage
grows with the result size. `ask` and `update` do not materialize a result.

## Progressive and configured queries

Use a connection when results must be consumed progressively or when an
operation needs bindings, a dataset, or execution options:

```java
try (Repository repository = Repositories.create();
     RepositoryConnection connection = repository.getConnection();
     TupleQueryResult result = connection
             .prepareTupleQuery("SELECT ?s WHERE { ?s ?p ?o }")
             .evaluate()) {
    while (result.hasNext()) {
        System.out.println(result.next());
    }
}
```

Closing a connection never closes its repository. Closing the repository makes
all its connections unusable.

SPARQL is implicit in this API because it is the only query language supported:
there is no language enum to repeat at every preparation call. Preparation
checks both the syntax and the expected query form. A SELECT string passed to
`prepareBooleanQuery`, for example, is rejected immediately.

## Query options

Prepared queries expose only options that the next execution pipeline applies:

```java
Dataset dataset = Dataset.builder()
        .defaultGraph(repository.getValueFactory().createIRI("urn:default"))
        .namedGraph(repository.getValueFactory().createIRI("urn:named"))
        .build();

try (RepositoryConnection connection = repository.getConnection();
     TupleQueryResult result = connection
             .prepareTupleQuery("SELECT ?o { ?s <urn:p> ?o }")
             .setBinding("s", repository.getValueFactory().createIRI("urn:subject"))
             .setDataset(dataset)
             .setTimeout(Duration.ofSeconds(5))
             .evaluate()) {
    result.forEach(System.out::println);
}
```

Binding names never include `?` or `$`. `Duration.ZERO` disables the timeout.
The immutable `Dataset` replaces every `FROM` and `FROM NAMED` clause in the
query text; `setDataset(null)` removes this override. A dataset set on the query
overrides the connection-level dataset.

Prepared updates intentionally expose only `execute()` today. Bindings,
datasets, inference switches, and timeouts are not advertised for updates until
the execution pipeline can honor their complete semantics.

## Transactions

Call `supportsTransactions()` before `begin()`. `isActive()` reports the state
of the transaction owned by the connection. `commit()` and `rollback()` require
an active transaction, and closing a connection rolls one back automatically.
The standard in-memory backend currently reports transactions as unsupported.

## SPARQL feature coverage

The public contract follows SPARQL query forms and dataset semantics, but the
next evaluator is still being completed feature by feature. A valid construct
that is not implemented yet fails explicitly with
`UnsupportedQueryFeatureException`; it is not silently delegated to the legacy
pipeline. SPARQL UPDATE currently executes `INSERT DATA` and `DELETE DATA`.

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
