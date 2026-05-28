package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

/**
 * Function {@code NOW()} in SPARQL 1.1 — §17.4.5.1.
 * Returns the current query execution time as an {@code xsd:dateTime}.
 * This function takes no arguments.
 */
public class NowAst implements XsdDateTimeExpressionAst {

    @Override
    public String getName() {
        return "NOW";
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
    }
}
