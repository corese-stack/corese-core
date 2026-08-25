/**
 * Public SPARQL result I/O contracts.
 *
 * <p>{@link fr.inria.corese.core.next.query.api.io.format.ResultFormat}
 * enumerates the supported serialization formats for SPARQL SELECT and ASK
 * results (CSV, TSV, JSON, XML). Serializer contracts live in
 * {@code io.serializer}; reusable option facets live in
 * {@code io.serializer.option}.
 * The recommended high-level entry point is {@link
 * fr.inria.corese.core.next.io.CoreseIO}.</p>
 */
package fr.inria.corese.core.next.query.api.io;
