# RDF and SPARQL result I/O

`CoreseIO` is the recommended public entry point for reading RDF and exporting
RDF graphs or SPARQL query results. The short methods cover common use cases;
typed option objects and the factory accessors cover advanced use cases.

Use `Values.factory()` and `Models.create()` to construct RDF values and models
outside an I/O operation. `CoreseIO` is intentionally limited to parsing and
serialization concerns.

## Read and write RDF

```java
Model model = CoreseIO.read(input, RDFFormat.TURTLE);
CoreseIO.write(model, RDFFormat.NTRIPLES, output);
```

`Reader`, `InputStream`, and `Writer` arguments remain owned by the caller.
Path-based methods own and close the file handles they create. The convenient
`read` methods build an insertion-ordered, in-memory model. For a custom or
persistent target, pass the target model and value factory explicitly:

```java
CoreseIO.read(input, RDFFormat.TURTLE, targetModel, valueFactory);
```

Shared parsing and serialization options are public and format-neutral:

```java
RDFParserOptions parsing = RDFParserOptions.builder()
        .baseIRI("https://example.org/")
        .build();

RDFSerializerOptions serialization = RDFSerializerOptions.builder()
        .prettyPrint(true)
        .stableBlankNodeIds(true)
        .build();
```

## Export SPARQL results

Existing results can be exported directly:

```java
try (TupleQueryResult result = query.evaluate()) {
    CoreseIO.write(result, ResultFormat.JSON, output);
}

try (GraphQueryResult result = graphQuery.evaluate()) {
    CoreseIO.write(result, RDFFormat.NQUADS, output);
}
```

The repository helpers own the connection and result they create, while the
destination remains caller-owned:

```java
CoreseIO.writeSelect(repository, selectQuery, ResultFormat.JSON, output);
CoreseIO.writeAsk(repository, askQuery, ResultFormat.XML, output);
CoreseIO.writeGraph(repository, constructQuery, RDFFormat.TURTLE, output);
```

Use `ResultSerializerOptions` for JSON/XML links, CSV/TSV line endings, or XML
output properties. Unsupported format/option combinations are rejected instead
of being silently ignored.

## Memory and resource behavior

| Operation | Behavior |
| --- | --- |
| `CoreseIO.read(...)` | Materializes RDF in a new in-memory model |
| `CoreseIO.writeToString(...)` | Materializes the complete text in memory |
| SELECT JSON, XML, CSV, or TSV export | Consumes rows progressively |
| Graph N-Triples or N-Quads export | Consumes statements progressively |
| Turtle, TriG, RDF/XML, JSON-LD, or canonical RDF export from a one-shot result | May materialize statements for graph-wide analysis |

Serializers consume a query result once but never close a caller-owned result or
destination. Applications should use try-with-resources for results they
evaluate themselves. Closing a stream returned by `TupleQueryResult.stream()`
or `GraphQueryResult.stream()` closes its underlying result.

The supported RDF and SPARQL result formats can be discovered by name,
extension, or media type through `RDFFormat` and `ResultFormat`.
