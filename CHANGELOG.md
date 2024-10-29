# Corese Changelog

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
