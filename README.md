<!-- markdownlint-disable MD033 -->
<!-- markdownlint-disable MD041 -->

<p align="center">
    <a href="https://project.inria.fr/corese/">
        <img src="docs/source/_static/logo/corese-core.svg" width="200" alt="Corese-Core-logo">
    </a>
    <br>
    <strong>Java library for the Semantic Web of Linked Data</strong>
</p>

[![License: CECILL-C](https://img.shields.io/badge/License-CECILL--C-blue.svg)](https://cecill.info/licences/Licence_CeCILL-C_V1-en.html) [![Discussions](https://img.shields.io/badge/Discussions-GitHub-blue)](https://github.com/orgs/corese-stack/discussions)

## Features

- Manipulate RDF graphs (parse, serialize, transform)
- Execute SPARQL 1.1 queries and updates
- Reason with RDFS and OWL RL
- Validate RDF graphs using SHACL
- Transform RDF using STTL (SPARQL Template Transformation Language)
- Apply logic-based rules with SPARQL Rules
- Extend functionality and scripting with LDScript

## Getting Started

Integrate Corese-Core into your Java project using your preferred build tool.

### Maven

```xml
<dependency>
  <groupId>fr.inria.corese</groupId>
  <artifactId>corese-core</artifactId>
  <version>4.6.4-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'fr.inria.corese:corese-core:4.6.4-SNAPSHOT'
```

### Manual JAR

Download the latest `.jar` file from:

<a href='https://github.com/corese-stack/corese-core/releases'>
    <img width='140' alt='Get it on GitHub' src='docs/source/_static/logo/badge_github.svg'/>
</a>
<a href='https://central.sonatype.com/artifact/fr.inria.corese/corese-core'>
    <img width='140' alt='Get it on Maven Central' src='docs/source/_static/logo/badge_maven.svg'/>
</a>

## Documentation

- [Corese-Core api documentation](https://corese-stack.github.io/corese-core/v4.6.3/java_api/library_root.html)

**W3C Standards:**

- [RDF 1.1 Primer](https://www.w3.org/TR/rdf11-primer/)
- [RDFS (RDF Schema)](https://www.w3.org/TR/rdf-schema/)
- [OWL 2 RL](https://www.w3.org/TR/owl2-profiles/#OWL_2_RL)
- [SPARQL 1.1 Query Language](https://www.w3.org/TR/sparql11-query/)
- [SPARQL 1.1 Update](https://www.w3.org/TR/sparql11-update/)
- [SHACL (Shapes Constraint Language)](https://www.w3.org/TR/shacl/)

**Corese Extensions:**

- [STTL Documentation](https://files.inria.fr/corese/doc/sttl.html)
- [SPARQL Rule Engine](https://files.inria.fr/corese/doc/rule.html)
- [LDScript Reference](https://files.inria.fr/corese/doc/ldscript.html)

## Contributing

We welcome contributions! Here’s how to get involved:

- [GitHub Discussions](https://github.com/orgs/corese-stack/discussions)
- [Issue Tracker](https://github.com/corese-stack/corese-core/issues)
- [Pull Requests](https://github.com/corese-stack/corese-core/pulls)

## Useful Links

- [Corese Website](https://corese-stack.github.io/corese-core)
- Mailing List: <corese-users@inria.fr>
- Subscribe: Send an email to <corese-users-request@inria.fr> with the subject: `subscribe`
