/**
 * Prepared-query implementations for the public SPARQL API.
 *
 * <p>Each class implements one of the public query contracts:</p>
 * <ul>
 *   <li>{@link fr.inria.corese.core.next.query.impl.query.CoreseTupleQuery} — SELECT</li>
 *   <li>{@link fr.inria.corese.core.next.query.impl.query.CoreseBooleanQuery} — ASK</li>
 *   <li>{@link fr.inria.corese.core.next.query.impl.query.CoreseGraphQuery} — CONSTRUCT / DESCRIBE</li>
 *   <li>{@link fr.inria.corese.core.next.query.impl.query.CoreseUpdate} — SPARQL UPDATE (INSERT DATA / DELETE DATA)</li>
 * </ul>
 *
 * <p>Prepared query classes extend
 * {@link fr.inria.corese.core.next.query.impl.query.AbstractCoreseQuery},
 * which manages initial bindings, dataset override, timeout, and inference flag.
 * None of these classes expose the parser, AST, bridge, or KGRAM engine.</p>
 */
package fr.inria.corese.core.next.query.impl.query;
