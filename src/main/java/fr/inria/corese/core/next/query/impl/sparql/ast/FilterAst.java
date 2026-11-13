package fr.inria.corese.core.next.query.impl.sparql.ast;


import fr.inria.corese.core.next.query.impl.sparql.ast.visitor.AstVisitor;

/**
 * Filter clause in a SPARQL query.
 *
 * @param operator the boolean expression to evaluate
 * @param line     1-based source line of the FILTER keyword (-1 if unknown)
 * @param column   0-based character position of the FILTER keyword (-1 if unknown)
 */
public record FilterAst(TermAst operator, int line, int column) implements PatternAst {
    public FilterAst(TermAst operator) {
        this(operator, -1, -1);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.operator.accept(visitor);
    }
}
