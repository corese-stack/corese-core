/**
 * SPARQL 1.1 Update orchestration between the parser/bridge and storage SPI.
 *
 * <p>WHERE evaluation reuses the native query pipeline. Quad templates are
 * materialized before deleting or inserting, and blank nodes are fresh for
 * each INSERT DATA operation or template solution. Graph management preserves
 * empty named graphs and distinguishes CLEAR from DROP.</p>
 *
 * <p>Requests use backend transactions when available. An update inside an
 * existing repository transaction retains caller ownership and restores its
 * own savepoint on failure. SILENT operations also restore an operation
 * savepoint before continuing. The legacy, nontransactional backend uses
 * compensating restoration, which does not provide concurrent isolation.</p>
 */
package fr.inria.corese.core.next.query.impl.sparql.update;
