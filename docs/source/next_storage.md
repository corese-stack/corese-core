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
The `memory` backend advertises `SERIALIZABLE` and returns an `AutoCloseable`
transaction handle. Closing an active transaction rolls it back.

## Native memory backend

The `memory` backend is selected by default by `Storages.create()` or explicitly:

```java
StorageManager storage = Storages.create(
    StorageConfig.builder().type("memory").build());
```

This implementation uses SPO, POS and OSP hash indexes in each graph partition.
Bound RDF terms select index prefixes; values are compared by RDF identity, including
literal lexical form, datatype and language. Prefix iteration is reproducible for
an identical insertion history. It supplies neither sorted value ranges nor a
SPARQL result order without `ORDER BY`. Named graphs survive `clear` and the
removal of their last statement until `dropGraph`.

`find` captures partition snapshot references under a shared monitor and returns
a lazy stream over that snapshot without allocating an intermediate collection.
`count` and `contains` traverse the selected index without materializing the matches.
Searches across every graph visit all partitions.

`addAll` consumes its iterable once, deduplicates quads in private staging, builds
all permutations and publishes the touched partitions together. An ordinary source
or finalization exception leaves the batch unpublished. Earlier writes in an
explicit transaction survive a failed batch. Other sessions see the batch only
after publication, or after transaction commit. A session cannot perform another
write while it consumes a batch source. Bulk construction needs temporary memory;
a smaller retained index does not necessarily imply a smaller load peak.

Sessions share committed data and own independent transactions, even on the same
thread. Only `SERIALIZABLE` is supported. Beginning a transaction copies the graph
directory and shares its partitions (time and memory proportional to graph count).
The first write to a shared partition copies that graph's index containers;
other graphs remain shared. Autocommit writers also copy partitions still used by
snapshots. Statements and cached immutable keys remain shared. Commit verifies a
global version and publishes the private state atomically.
Any intervening committed write causes a conservative conflict, including writes
to disjoint graphs. Roll back the still-active transaction before retrying.
Transaction reads and writes must run on the creating thread. Autocommit protects
each operation; use an explicit transaction for a consistent multi-operation view.

Closing a session rolls back its transaction. Closing the owner shuts down the
shared backend. The storage architecture and benchmarking synthesis are documented in
`corese-docs` under `Architecture/corese-storage-manager/memory-storage.md`.

The isolation guarantees cover storage operations on RDF term identities.
Corese's built-in temporal literals expose their mutable calendar reference;
this backend preserves that API behavior. Do not mutate those calendars while
terms are in use: doing so can change temporal evaluation without a storage write.
Index snapshots share terms and do not provide deep isolation of calendar objects.
