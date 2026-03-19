package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

/**
 * SPARQL 1.0 parser feature that handles the {@code UNION} graph pattern.
 *
 */
public class UnionFeature extends AbstractSparqlFeature {

    /**
     * Constructs a {@code UnionFeature} bound to the given AST builder.
     *
     * @param builder the {@link SparqlAstBuilder} that will receive UNION lifecycle events
     */
    public UnionFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    /**
     * Called when the parser enters a {@code GroupOrUnionGraphPattern}.
     * Delegates to {@link SparqlAstBuilder#enterUnion()} to open a branch collector.
     *
     * @param ctx the ANTLR parse context for {@code groupOrUnionGraphPattern}
     */
    @Override
    public void enterGroupOrUnionGraphPattern(SparqlParser.GroupOrUnionGraphPatternContext ctx) {
        builder().enterUnion();
    }

    /**
     * Called when the parser exits a {@code GroupGraphPattern}.
     * If this group is a direct child of a {@code GroupOrUnionGraphPattern},
     * delegates to {@link SparqlAstBuilder#collectUnionBranch()} to register it as a UNION branch.
     *
     * @param ctx the ANTLR parse context for {@code groupGraphPattern}
     */
    @Override
    public void exitGroupGraphPattern(SparqlParser.GroupGraphPatternContext ctx) {
        if (ctx.getParent() instanceof SparqlParser.GroupOrUnionGraphPatternContext) {
            builder().collectUnionBranch();
        }
    }

    /**
     * Called when the parser exits a {@code GroupOrUnionGraphPattern}.
     * Delegates to {@link SparqlAstBuilder#exitUnion()} to fold all collected branches
     * into a {@link fr.inria.corese.core.next.query.impl.sparql.ast.UnionAst} and attach
     * it to the enclosing group.
     *
     * @param ctx the ANTLR parse context for {@code groupOrUnionGraphPattern}
     */
    @Override
    public void exitGroupOrUnionGraphPattern(SparqlParser.GroupOrUnionGraphPatternContext ctx) {
        builder().exitUnion();
    }
}