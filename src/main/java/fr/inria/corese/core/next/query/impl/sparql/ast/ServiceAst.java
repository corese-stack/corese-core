package fr.inria.corese.core.next.query.impl.sparql.ast;

/**
 * AST node representing a SPARQL {@code SERVICE} graph pattern.
 *
 * <pre>
 * SERVICE SILENT? &lt;endpoint&gt; { ... }
 * </pre>
 *
 * @param endpoint the remote service endpoint, either an IRI or a variable
 * @param silent   whether the {@code SILENT} keyword was present
 * @param pattern  the graph pattern to evaluate at the remote endpoint
 */
public record ServiceAst(TermAst endpoint, boolean silent, GroupGraphPatternAst pattern) implements PatternAst {
}
