# RDF data API

The data API represents RDF terms, statements, datasets, and prefix metadata.
Its stable baseline is RDF 1.1. It also includes an intentionally narrow preview
of RDF 1.2 triple terms, without claiming full RDF 1.2 conformance yet.

## Creating values

Use the public `Values` entry point when no repository is available:

```java
ValueFactory values = Values.factory();

IRI subject = values.createIRI("urn:subject");
IRI predicate = values.createIRI("urn:predicate");
Literal label = values.createLiteral("hello", "en");
Statement statement = values.createStatement(subject, predicate, label);
```

`Values.factory()` returns a shared thread-safe factory. Code working through a
repository should use `repository.getValueFactory()` or
`connection.getValueFactory()` instead, allowing a storage implementation to
provide compatible value types.

Typed literals are created from their declared datatype IRI:

```java
Literal count = values.createLiteral("42", XSDDatatype.INTEGER.getIRI());
```

Corese derives its internal datatype handling from that IRI. The API does not
allow a separate, contradictory internal datatype classification to be supplied.
An ill-typed lexical form remains a valid RDF literal term but its typed value
accessors fail.

## Creating models

`Models` creates standalone, mutable, insertion-ordered in-memory models:

```java
Model model = Models.create();
model.add(statement);

Model copy = Models.create(model);
```

Models deliberately do not promise Java object serialization. Persist and
exchange them with an RDF syntax through `CoreseIO`.

## Graph contexts

A `Statement` has one optional context. A `null` context represents the default
graph; a non-null context identifies a named graph. Passing several contexts to
`Model.add` creates one statement per selected graph.

For matching, filtering, removing, and clearing:

- no contexts, or a null context array, means every graph;
- an explicit `(Resource) null` selects only the default graph;
- one or more resources select those named graphs.

The `subjects()`, `predicates()`, `objects()`, and `contexts()` methods return
live set views. Removing a term removes all statements that use it. Adding an
isolated term is unsupported because it cannot define an RDF statement.

## Prefix metadata

Prefix declarations are serialization metadata rather than RDF statements.
Create an empty mapping with `Prefixes.create()`, or start with the common RDF,
RDFS, XSD, OWL, and FOAF declarations using
`Prefixes.createWithDefaults()`:

```java
PrefixMapping prefixes = Prefixes.create();
prefixes.setPrefix("ex", "https://example.org/");

String iri = prefixes.expandPrefix("ex:resource");
PrefixMapping independentCopy = prefixes.copy();
```

Collections returned by a prefix mapping are immutable snapshots. Models keep
their own namespace metadata through `setNamespace` and `getNamespaces`.

## RDF 1.2 triple terms

`ValueFactory.createTriple` creates a triple term following the current RDF 1.2
Candidate Recommendation. A triple term is a `Value` and can occur in the
object position of another triple. It is not a `Resource`, so it cannot be used
as a statement subject or graph name.

Other RDF 1.2 additions, notably directional language-tagged strings and RDF
version announcements, are outside the current public contract and will be
handled with the later RDF/SPARQL 1.2 work.
