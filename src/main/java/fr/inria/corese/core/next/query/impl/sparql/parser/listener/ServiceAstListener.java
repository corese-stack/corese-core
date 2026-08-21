package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

/**
 * SPARQL 1.1 parser feature that handles the {@code SERVICE} graph pattern.
 *
 * <p>Grammar hook:
 * <pre>
 * serviceGraphPattern : SERVICE SILENT? varOrIri groupGraphPattern ;
 * </pre>
 *
 * <p>On {@code enterServiceGraphPattern}, the endpoint IRI (or variable) and the
 * {@code SILENT} flag are captured and passed to
 * {@link SparqlAstBuilder#enterService(TermAst, boolean)}.
 * The inner {@code groupGraphPattern} is handled by {@link BgpAstListener} via the
 * shared {@link SparqlAstBuilder#enterGroup()} / {@link SparqlAstBuilder#exitGroup()}
 * mechanism.  When that group is closed, the builder recognises it as the body of a
 * SERVICE clause and wraps it in a
 * {@link fr.inria.corese.core.next.query.impl.sparql.ast.ServiceAst}.
 */
public class ServiceAstListener extends AbstractSparqlAstListener {

    /**
     * Constructs a {@code ServiceAstListener} bound to the given AST builder.
     *
     * @param builder the {@link SparqlAstBuilder} that will receive SERVICE lifecycle events
     */
    public ServiceAstListener(SparqlAstBuilder builder) {
        super(builder);
    }

    /**
     * Called when the parser enters a {@code serviceGraphPattern}.
     * Extracts the endpoint and the {@code SILENT} flag, then delegates to
     * {@link SparqlAstBuilder#enterService(TermAst, boolean)}.
     *
     * @param ctx the ANTLR parse context for {@code serviceGraphPattern}
     */
    @Override
    public void enterServiceGraphPattern(SparqlParser.ServiceGraphPatternContext ctx) {
        boolean silent = ctx.SILENT() != null;
        TermAst endpoint = builder().termFromVarOrIriRef(ctx.varOrIri());
        builder().enterService(endpoint, silent);
    }

    /**
     * Called when the parser exits a {@code serviceGraphPattern}.
     * Delegates to {@link SparqlAstBuilder#exitService()}.
     *
     * @param ctx the ANTLR parse context for {@code serviceGraphPattern}
     */
    @Override
    public void exitServiceGraphPattern(SparqlParser.ServiceGraphPatternContext ctx) {
        builder().exitService();
    }
}
