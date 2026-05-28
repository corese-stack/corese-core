package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

/**
 * Function {@code RAND()} in SPARQL 1.1.
 */
public record RandAst() implements NumericExpressionAst {

    @Override
    public String getName() {
        return "RAND";
    }
}
