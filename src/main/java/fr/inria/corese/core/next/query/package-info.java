/**
 * Unified SPARQL query, update, and repository management facade for Corese v5.
 *
 * <h2>Entry Points</h2>
 * <p>{@link fr.inria.corese.core.next.query.Repositories} serves as the root factory for creating and
 * executing operations against RDF repositories:</p>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.query.Repositories#create()}: Instantiates an in-memory repository
 *       backed by the native Copy-On-Write index engine.</li>
 *   <li>{@link fr.inria.corese.core.next.query.Repositories#createFrom(java.nio.file.Path)}: Instantiates and
 *       pre-loads an RDF graph or dataset directly from a file.</li>
 *   <li>{@link fr.inria.corese.core.next.query.Repositories#withMemory(java.util.function.Consumer)}:
 *       Executes a scoped action against an auto-closed repository.</li>
 * </ul>
 *
 * <h2>Architecture &amp; Lifecycle</h2>
 * <p>The query engine is organized into four complementary layers:</p>
 * <ul>
 *   <li><strong>Facade Layer:</strong> {@code Repositories} and convenience shortcuts on {@code Repository}.</li>
 *   <li><strong>API Contracts ({@code api}):</strong> Pure, implementation-independent interfaces for repositories,
 *       connections, prepared queries, and results.</li>
 *   <li><strong>Parser &amp; AST ({@code impl.sparql}):</strong> High-performance ANTLR4-based SPARQL 1.1 parser
 *       and abstract syntax tree.</li>
 *   <li><strong>Execution Pipeline ({@code impl.engine}):</strong> Cost-based optimizer (CBO), streaming cursors,
 *       and join evaluation engine.</li>
 * </ul>
 */
package fr.inria.corese.core.next.query;
