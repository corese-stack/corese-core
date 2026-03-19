package fr.inria.corese.core.next.query.impl.parser.listener;

import fr.inria.corese.core.next.impl.parser.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.parser.SparqlAstBuilder;

/**
 * SPARQL 1.0 parser feature that handles {@code DESCRIBE} queries.
 *
 * <p>Grammar rule covered:
 * <pre>
 *   describeQuery
 *     : DESCRIBE ( varOrIRIref+ | '*' ) whereClause?
 * </pre>
 *
 *
 * <p>The WHERE clause (optional) is built by {@link BgpFeature} as usual.
 */
public class DescribeQueryFeature extends AbstractSparqlFeature {
    /**
     * Constructs bound to the given AST builder.
     *
     * @param builder the {@link SparqlAstBuilder} that will receive DESCRIBE lifecycle events
     */
    public DescribeQueryFeature(SparqlAstBuilder builder) {
        super(builder);
    }

    /**
     * Called when the parser enters.
     * Sets the query type to DESCRIBE.
     *
     * @param ctx the ANTLR parse context for {@code describeQuery}
     */
    @Override
    public void enterDescribeQuery(SparqlParser.DescribeQueryContext ctx) {
        builder().enterDescribeQuery();
    }

    /**
     * Called when the parser exits.
     * Extracts the resource list ({@code varOrIRIref+} or {@code *}) and
     * registers each resource with the builder.
     * {@code DESCRIBE *} produces no resources (empty list → {@code isDescribeAll() = true}).
     *
     * @param ctx the ANTLR parse context for {@code describeQuery}
     */
    @Override
    public void exitDescribeQuery(SparqlParser.DescribeQueryContext ctx) {
        // DESCRIBE * → empty resource list
        if (ctx.STAR() != null) {
            builder().exitDescribeQuery();
            return;
        }

        for (SparqlParser.VarOrIRIrefContext ref : ctx.varOrIRIref()) {
            String txt = ref.getText();
            if (txt.startsWith("?") || txt.startsWith("$")) {
                builder().addDescribeResource(builder().var(txt));
            } else {
                builder().addDescribeResource(builder().iri(txt));
            }
        }
        builder().exitDescribeQuery();
    }
}