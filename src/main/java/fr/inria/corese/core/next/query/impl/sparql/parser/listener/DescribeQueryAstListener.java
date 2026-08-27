package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlAstBuilder;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlQueryAstBuilder;

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
 * <p>The WHERE clause (optional) is built by {@link BgpAstListener} as usual.
 */
public class DescribeQueryAstListener extends AbstractSparqlAstListener implements QueryAstListener {
    /**
     * Constructs bound to the given AST builder.
     *
     * @param builder the {@link SparqlAstBuilder} that will receive DESCRIBE lifecycle events
     */
    public DescribeQueryAstListener(SparqlQueryAstBuilder builder) {
        super(builder);
    }

    public SparqlQueryAstBuilder queryBuilder() {
        return (SparqlQueryAstBuilder) builder();
    }

    /**
     * Called when the parser enters.
     * Sets the query type to DESCRIBE.
     *
     * @param ctx the ANTLR parse context for {@code describeQuery}
     */
    @Override
    public void enterDescribeQuery(SparqlParser.DescribeQueryContext ctx) {
        queryBuilder().enterDescribeQuery();
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
            queryBuilder().exitDescribeQuery();
            return;
        }

        for (SparqlParser.VarOrIriContext ref : ctx.varOrIri()) {
            String txt = ref.getText();
            if (txt.startsWith("?") || txt.startsWith("$")) {
                queryBuilder().addDescribeResource(builder().variable(txt));
            } else {
                queryBuilder().addDescribeResource(builder().iri(txt));
            }
        }
        queryBuilder().exitDescribeQuery();
    }
}
