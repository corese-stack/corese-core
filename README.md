# Corese-Core

[![License: CECILL-C](https://img.shields.io/badge/License-CECILL--C-blue.svg)](https://cecill.info/licences/Licence_CeCILL-C_V1-en.html) [![Discussions](https://img.shields.io/badge/Discussions-GitHub-blue)](https://github.com/orgs/corese-stack/discussions)

Corese-Core is the central Java library of the Corese platform, designed to implement and extend the Semantic Web standards. It enables creating, manipulating, parsing, serializing, querying, reasoning, and validating RDF data.

Corese-Core implements key W3C standards such as [RDF](https://www.w3.org/RDF/), [RDFS](https://www.w3.org/2001/sw/wiki/RDFS), [SPARQL 1.1 Query & Update](https://www.w3.org/2001/sw/wiki/SPARQL), [OWL RL](https://www.w3.org/2005/rules/wiki/OWLRL), and [SHACL](https://www.w3.org/TR/shacl/). It also includes extensions like [STTL SPARQL](https://files.inria.fr/corese/doc/sttl.html), [SPARQL Rule](https://files.inria.fr/corese/doc/rule.html), and [LDScript](https://files.inria.fr/corese/doc/ldscript.html).

## Features

- RDF data manipulation and reasoning.
- SPARQL query processing with advanced features.
- Support for semantic web standards (RDF, RDFS, OWL RL, SHACL).
- Extensions for rules and custom scripting (STTL, SPARQL Rule, LDScript).

## Getting Started

### Download and Install

To integrate Corese-Core into your project, you can use Maven, Gradle, or download the latest JAR file directly.

**Maven:**

Add this dependency to your `pom.xml` file:

``` xml
<dependency>
    <groupId>fr.inria.corese</groupId>
    <artifactId>corese-core</artifactId>
    <version>4.6.4-SNAPSHOT</version>
</dependency>
```

**Gradle:**

Include the following in your `build.gradle` file:

``` gradle
implementation 'fr.inria.corese:corese-core:4.6.4-SNAPSHOT'
```

**JAR:**

You can download the latest version of the **Corese-Core** JAR from the [releases page](https://github.com/corese-stack/corese-core/releases).

## Documentation

Explore the available documentation on Corese-Core pages: [Corese-Core Documentation](https://corese-stack.github.io/corese-core/).

## Contributions and Community

We value contributions and feedback from the community! Here’s how you can engage:

- **Discussions:** For questions, ideas, or general discussion, join our [discussion forum](https://github.com/orgs/corese-stack/discussions).
- **Issue Tracker:** Report issues or suggest new features via our [issue tracker](https://github.com/corese-stack/corese-core/issues).
- **Pull Requests:** Contributions are welcome! Feel free to submit your code through [pull requests](https://github.com/corese-stack/corese-core/pulls).

## Useful Links

- **Mailing List:** <corese-users@inria.fr>
- **Join the Mailing List:** Send an email to <corese-users-request@inria.fr> with the subject: `subscribe`
