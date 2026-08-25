# Storage provider API

Most applications should use `Repositories` and never access a storage manager
directly. The storage API is a service-provider interface for backend authors and
advanced embedding scenarios; it is not a second query API.

## Creating standalone storage

`Storages` is the only general-purpose creation entry point:

```java
try (StorageManager storage = Storages.create()) {
    Statement statement = Values.factory().createStatement(subject, predicate, object);
    storage.mutations().add(statement);

    try (Stream<Statement> matches = storage.queries().find(StatementPattern.matchAll())) {
        matches.forEach(System.out::println);
    }
}
```

`Storages.create()` selects the in-memory backend. Pass a `StorageConfig` to
select and configure another registered plugin. The returned manager is open
and owned by the caller. Closing it is idempotent.

When a repository is needed, prefer `Repositories.create(config)`: the
repository owns the backend and closes it automatically.

## Provider contract

A backend implements `StorageManager` and exposes five focused capabilities:

- `queries()` streams matching statements and provides optimized count and
  containment checks;
- `mutations()` performs set-like additions and removals;
- `metadata()` exposes structural summaries and statistics;
- `transactions()` advertises and starts supported transactions;
- `lifecycle()` controls initialization and shutdown.

Duplicate insertion and removal of an absent statement are ordinary set
operations. Single mutations return whether data changed; patterned and batch
mutations return the number of changed statements. Backend failures throw a
`StorageException` with a stable `ErrorCode`.

`MutationOperations.addAll` and `removeAll` have correct default
implementations. A native backend can override them with an optimized batch
path without changing the public contract.

## Statement patterns and graphs

In a `StatementPattern`, null subject, predicate, or object values are
wildcards. Context selection follows the same rules as the RDF `Model` API:

- no contexts or a null array means every graph;
- an explicit `(Resource) null` selects the default graph;
- resources select named graphs;
- context order and duplicates have no semantic effect.

The stream returned by `queries().find(pattern)` must be closed. Repository
results manage that stream through their own `close()` contract.

## Transactions

Call `transactions().supportsTransactions()` before beginning a transaction.
The current memory and legacy Graph adapters report no transaction support.
A future native backend may advertise its supported isolation levels and return
an `AutoCloseable` transaction handle. Closing an active transaction rolls it
back.
