<!-- markdownlint-disable MD024 -->
# Corese Changelog

## Version 4.6.4 –

### Changed

- Removed ShEx implementation.
- Removed LUBM benchmark.
- Removed Java Platform Module System (JPMS) support.
- Cleaned up unused resource files.
- Updated and corrected documentation:
  - Fixed typos in the Getting Started guide.
  - Add imports to the Getting Started guide.
- Improved GitHub Actions log messages for the documentation workflow (`run-name` is now clearer).
- Added new GitHub Actions workflow to automatically publish a development pre-release (`dev-prerelease`) on pushes to the `develop` branch.
- Added `slf4j-simple` as a development-only runtime dependency.

### Fixed

- QNAME parsing error with local names starting with digits and hyphens (e.g. `prefix:2-Systeme-ZBS...`). ([#133](https://github.com/.../issues/133))

## Version 4.6.3 – 2025-02-20

### Added

- `DataManager` support for RDFa parsing, enabling alternative storage solutions.
- Ensured RDFa triples can be stored in the same backends as other RDF formats.

### Changed

- Improved SPARQL query result display in the Getting Started guide.
- Fixed syntax errors in the Mermaid diagram for RDF relationships in the Getting Started guide.
- Added `com.sun.activation:jakarta.activation:2.0.1` dependency to resolve missing MIME data handling warning.
- Ensured compatibility with Java 11+ and Jakarta EE.

### Fixed

- OWL imports processing across all RDF formats:
  - **RDFXML**: Previously ignored the `owl:imports` option.
  - **TTL, TRIG, NT, NQ**: Previously caused `NullPointerException` when auto-import was active.
  - **JSON-LD, RDFa**: Previously did not process `owl:imports` even when enabled.

## Version 4.6.2 – 2025-02-20

### Fixed

- Fixed JSON-LD serializer errors on Windows.
- Fixed non-functional unit tests.

### Changed

- Updated and corrected documentation:
  - Fixed broken links.
  - Updated graph import/export formats.

## Version 4.6.1 – 2024-12-12

### Added

- Code cleanup and refactoring to improve readability and maintainability.
  - Reworking of the Property class as a proper singleton.
  - HTTPHeaders class created to centralize repeated definitions of HTTP headers.
- Elasticsearch's integration.
  - EdgeChangeListener class created to listen to edge changes and deletion.
- Add SLF4J dependency to the project.
  
### Fixed

- Fixed possibility of apparition of XML comment into JSON results.
- Fixed missing SLF4J dependency.

### Changed

- Cleaned documentation website.

## Version 4.6.0 – 2024-10-28

### Added

- Added support for the RDFC-1.0 algorithm for canonicalizing RDF datasets (see W3C Standard [RDF Canonicalization](https://www.w3.org/TR/rdf-canon/)).

### Fixed

- Removed repeated `println` statements during reasoning (see [issue #174](https://github.com/Wimmics/corese/issues/174)).
- Fixed a bug where `NOW()` returned multiple values in a single query (see [issue #168](https://github.com/Wimmics/corese/issues/168)).
- Fixed an issue where executing a subquery with `GROUP BY` and passing variable bindings (e.g., `?x`) led to incorrect results when combined with triple patterns outside the subquery. Updated `queryNodeList` to prevent passing the group-by variable as a binding, ensuring consistent query results.

## Version 4.5.0 – 2023-12-14

### Added

- Improved RDF serializers (see [issue #142](https://github.com/Wimmics/corese/issues/142)).

### Fixed

- Fixed Trig serialization to escape special characters (see [issue #151](https://github.com/Wimmics/corese/issues/151)).
- Fixed federated queries with `PREFIX` statements failing under certain conditions (see [issue #140](https://github.com/Wimmics/corese/issues/140)).

## Version 4.4.0 – 2023-03-30

### Added

- Integrated storage systems:
  - Jena TDB1.
  - Corese Graph.
  - RDF4J Model.
  - [More information available here](https://github.com/Wimmics/corese/blob/master/docs/storage/Configuring%20and%20Connecting%20to%20Different%20Storage%20Systems%20in%20Corese.md).
- Beta support for RDF\* and SPARQL\* ([Community Group Report 17 December 2021](https://w3c.github.io/rdf-star/cg-spec/2021-12-17.html)).

### Changed

- Updated Jetty server library to version `11.0.8`.
- Performed code clean-up, corrections, and added comments for improved readability and maintenance.

### Fixed

- Fixed an encoding error when loading a file whose path contains a space in Corese-GUI.
- Fixed encoding error on Windows when exporting graphs from Corese-GUI.
- Fixed SPARQL engine bug where it was impossible to load a named graph that contains a non-empty RDF list.
- Fixed issue with `rdf:` prefix not found when sending a federated query to Fuseki (see [issue #114](https://github.com/Wimmics/corese/issues/114)).
- Fixed non-standard JSON format on query timeout (see [issue #113](https://github.com/Wimmics/corese/issues/113)).
- Fixed inconsistent status of the OWL and Rules checkboxes in Corese-GUI that were not updated during reload (see [issue #110](https://github.com/Wimmics/corese/issues/110)).
- Fixed the rule engine that was implementing optimizations incompatible with the `owl:propertyChainAxiom` rule (see [issue #110](https://github.com/Wimmics/corese/issues/110)).
