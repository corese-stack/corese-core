/**
 * Unified I/O and serialization facade for RDF graphs, datasets, and SPARQL query results in Corese v5.
 *
 * <h2>CoreseIO Facade</h2>
 * <p>{@link fr.inria.corese.core.next.io.CoreseIO} provides standard static utility methods for:</p>
 * <ul>
 *   <li><strong>Reading RDF:</strong> Parsing files, input streams, and readers into {@link fr.inria.corese.core.next.data.api.model.Model}
 *       instances across standard formats (Turtle, TriG, N-Triples, N-Quads, RDF/XML, JSON-LD, RDFa).</li>
 *   <li><strong>Writing RDF:</strong> Serializing models and statements to disk or streams with configurable formatting options.</li>
 *   <li><strong>Exporting Query Results:</strong> Formatting SPARQL SELECT/ASK results into standard XML, JSON, CSV, and TSV formats.</li>
 * </ul>
 *
 * <h2>Stream and Resource Management</h2>
 * <p>Methods accepting or returning streams assume standard caller ownership rules: callers remain
 * responsible for closing input streams and writers passed into {@code CoreseIO}. Methods returning
 * internal stream views must be wrapped in {@code try-with-resources} blocks.</p>
 */
package fr.inria.corese.core.next.io;
