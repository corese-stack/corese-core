package fr.inria.corese.core.next.query.impl.sparql.parser.listener;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.parser.SparqlUpdateAstBuilder;

/** Reuses SELECT group construction for the update WHERE clause. */
public final class ModifyUpdateAstListener extends AbstractSparqlUpdateAstListener {
    public ModifyUpdateAstListener(SparqlUpdateAstBuilder builder) {
        super(builder);
    }

    @Override
    public void enterModify(SparqlParser.ModifyContext context) {
        updateBuilder().enterSelectQuery();
        updateBuilder().enterWhereClause();
    }

    @Override
    public void exitModify(SparqlParser.ModifyContext context) {
        updateBuilder().exitSelectQuery();
        updateBuilder().addRequest(updateBuilder().modifyToAst(context));
    }
}
