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

## RDF data access

A connection exposes the same RDF terms and statements as the data API. Files
are parsed with `CoreseIO`, then their model can be added directly:

```java
try (Repository repository = Repositories.create();
     RepositoryConnection connection = repository.getConnection()) {
    Model model = CoreseIO.read(Path.of("data.ttl"), RDFFormat.TURTLE);
    connection.add(model);

    ValueFactory values = connection.getValueFactory();
    boolean present = connection.hasStatement(
            values.createIRI("urn:subject"),
            values.createIRI("urn:predicate"),
            null);
}
```

`CoreseIO.read` currently builds an in-memory `Model` before insertion. This is
the simple path for ordinary files; direct parser-to-repository streaming will
be added when the parser exposes a public statement sink.

Statements can also be created and modified without SPARQL:

```java
ValueFactory values = connection.getValueFactory();
IRI subject = values.createIRI("urn:subject");
IRI predicate = values.createIRI("urn:predicate");
IRI object = values.createIRI("urn:object");
IRI graph = values.createIRI("urn:graph");

connection.add(subject, predicate, object);        // default graph
connection.add(subject, predicate, object, graph); // named graph
connection.remove(subject, predicate, object, graph);
```

For `getStatements`, `hasStatement`, `remove`, `size`, and `clear`, omitting the
contexts means every graph. Passing an explicit `(Resource) null` selects only
the default graph. For `add`, omitting the context adds to the default graph.
With several contexts, `add` creates one statement in each selected graph.

`getStatements` is progressive and its result must be closed before its
connection. It can be exported directly through the same I/O facade:

```java
try (StatementResult statements =
         connection.getStatements(null, null, null)) {
    CoreseIO.write(statements, RDFFormat.NQUADS, outputStream);
}
```

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

Native SELECT results stream basic patterns, filters, BIND, VALUES, pure grouped
joins, supported UNION and GRAPH scopes, and simple OPTIONAL patterns. They keep
only the current bindings and row, rather than accumulating all result mappings.
Aggregates, paths, services, subqueries, MINUS, and joins requiring a
buffered strategy retain full evaluation. A scoped branch also keeps full evaluation
when repeatedly rescanning it would introduce quadratic work. DISTINCT retains its
required set of previously seen projections.

For eligible ordered SELECTs with a finite LIMIT, online Top-K selection retains
at most LIMIT + OFFSET mappings instead of buffering every solution. It uses the
Corese comparator and preserves arrival order for ties. All candidates are still
evaluated before results are delivered; DISTINCT additionally retains its seen
set. Snapshot ownership, interruption and deadlines cover both scanning and final
ranking. Queries without LIMIT, unsupported input algebra, and bounds exceeding
the supported integer capacity keep the classic full evaluator.

Consume a progressive result on the thread that called `evaluate()`, and always
close it with try-with-resources, including after `stream().limit(...)`. Its independent
read snapshot includes transaction-local writes visible at evaluation, and stays stable
across later writes, commits and rollbacks. Exhaustion, SPARQL LIMIT, failure and close
release the snapshot without finishing the source transaction. Cross-thread close can
cancel a cursor. A configured timeout includes client idle time; expiration releases
an idle snapshot without interrupting unrelated client code. Lazy failures are reported
by `hasNext()` or `next()`.

Backends without independent read snapshots use full evaluation. The repository-level
`select` shortcut remains materialized as described above.

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

Prepared queries retain an immutable compilation image after their first successful
planning pass. The first evaluation uses the compiler's fresh objects after a detached image has
been captured; subsequent evaluations create independent runtime operators, variable
indexes, VALUES rows, expression adapters and EXISTS caches. Execution never uses
the cached image itself. Changing bindings or the timeout reuses this plan. Changing the
ordered dataset or the session-visible storage version rebuilds it using current
cardinalities. Effective mutations, including writes inside a transaction, change
that version; no-op mutations do not. A snapshot transaction retains its version
across other sessions' commits. Its own commit preserves an unchanged visible plan,
while rollback restores the version of the data then visible outside the transaction.
Backends returning an unavailable data version are replanned on every evaluation.
A version change during planning prevents publication of that cache entry; concurrent
writes can still age estimates during execution, without changing query semantics.

## Execution diagnostics

Call `evaluateWithDiagnostics()` on a prepared SELECT, ASK, CONSTRUCT or DESCRIBE
query to execute it once and inspect immutable snapshots of its progress:

```java
try (RepositoryConnection connection = repository.getConnection()) {
    var execution = connection.prepareTupleQuery(
            "SELECT ?s { ?s <urn:type> <urn:Person> . ?s <urn:name> \"Alice\" }")
            .evaluateWithDiagnostics();
    try (TupleQueryResult result = execution.result()) {
        result.forEach(System.out::println);
    }
    System.out.print(execution.profile().format());
}
```

The profile reports AST-to-runtime compilation, plan completion (ordering,
statistics and node indexing), execution-frame creation, and evaluation durations.
`planReused()` identifies a cache hit: compilation and planning times are then zero,
while execution-frame creation remains measured. The initial compilation duration
includes capture of the private compilation image. Parsing, client idle time
and graph template materialization are excluded. Timings include
instrumentation overhead. The ordered patterns retain their operator locations;
this is a structural plan, not a chronological trace across UNION or OPTIONAL.
Runtime path lookups and dynamically compiled EXISTS patterns are marked as
auxiliary, with identical auxiliary descriptions aggregated.

`profile.delivery()` describes the strategy actually used: `STREAMING` or
`MATERIALIZED`. Its `fallbackReason()` reports the first guard selecting full
evaluation, such as `ORDER_BY`, `AGGREGATION`, `PROPERTY_PATH` or
`SNAPSHOT_UNAVAILABLE`; `NONE` means a progressive cursor was opened. This is
the selected guard, not a complete list of blocking operators.

For SELECT, `delivery().firstRowStatus()` starts as `UNOBSERVED` until a row is
available (`AVAILABLE`) or successful evaluation establishes an empty result
after OFFSET and LIMIT (`EMPTY`). Materialized SELECT results have already reached
one of these terminal observations when evaluation returns. Closing a cursor
without advancing it, or failing before the first row, leaves it `UNOBSERVED`.
For ASK, CONSTRUCT and DESCRIBE, the status is `NOT_APPLICABLE` and the fallback
reason is `QUERY_FORM`.

`firstRowNanos()` is non-negative only for `AVAILABLE`, and otherwise returns
`-1`. It accumulates active wall-clock time from the profiled evaluation until a
row is ready: before evaluation returns for materialized results, or on the first
successful `hasNext()` or `next()` for progressive results. Compilation, planning
and cursor setup are included; client pauses and initial query preparation are excluded. Once
a row is ready, this duration remains fixed even after further reading or closure.

For each pattern, the profile records the estimate already requested by the
planner (`-1` when unavailable), producer iterator openings, and candidate edges
returned to the engine after graph deduplication and before RDF matching.
Candidates are summed across invocations: they are neither result-row counts nor
internal index visits. Estimates precede runtime bindings and may overestimate
merged-graph cardinalities. Diagnostics do not request additional counts or scans.

Ordinary `evaluate()` calls use the uninstrumented producer and cursors.
Diagnostics preserve dataset, bindings, timeout and resource ownership. The caller
must close tuple and graph results. For progressive SELECT, `execution.profile()`
reports work performed so far; consume or close the result before reading the final
profile. Every returned profile remains immutable. Lazy failures retain their usual
exceptions and release the cursor's resources.

## Transactions

Call `supportsTransactions()` before `begin()`. `isActive()` reports the state
of the transaction owned by the connection. `commit()` and `rollback()` require
an active transaction, and closing a connection rolls one back automatically.
The native in-memory backend supports snapshot transactions with optimistic
conflict detection at commit.

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
