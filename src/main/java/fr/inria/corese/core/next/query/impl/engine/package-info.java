/**
 * Corese Next internal query execution engine and solver components.
 *
 * <p>This package and its subpackages encapsulate the runtime execution machinery
 * (historically KGRAM) responsible for executing compiled query patterns against
 * statement stores.</p>
 *
 * <h2>Architectural Invariants &amp; Development Guardrails</h2>
 * <ul>
 *   <li><b>Zero AST Leakage</b>: No direct dependencies exist from execution engine
 *   components ({@code eval}, {@code pattern}, {@code solution}, {@code memory})
 *   to ANTLR or parser AST classes. The engine executes compiled logical operators only.</li>
 *   <li><b>Storage Isolation</b>: Query evaluation and triple access must strictly
 *   transit through the {@link fr.inria.corese.core.next.query.impl.engine.spi.Producer}
 *   and Storage SPI abstractions.</li>
 *   <li><b>No Bloat in Core Engine Classes</b>: Feature additions must avoid appending
 *   large chunks of code directly into central classes such as {@code Eval} (~72 KB)
 *   or {@code Exp} (~42 KB). Specific semantics should be isolated into dedicated helper
 *   components or evaluators when this improves separation of responsibilities, rather than
 *   being appended directly to the core engine classes.</li>
 *   <li><b>Thread Isolation</b>: Execution contexts and solution mappings must remain
 *   isolated without global mutable state.</li>
 *   <li><b>W3C Zero-Regression</b>: All engine developments, feature additions, and
 *   refactorings must strictly preserve the green non-regression baseline established
 *   in {@code corese-w3c}.</li>
 * </ul>
 */
package fr.inria.corese.core.next.query.impl.engine;
