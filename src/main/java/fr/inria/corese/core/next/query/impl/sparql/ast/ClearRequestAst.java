package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * Represents the CLEAR operation as defined in the <a href="https://www.w3.org/TR/2013/REC-sparql11-update-20130321/#clear">SPARQL 1.1 recommendation<a/>.
 * @param graphRef targeted graph to be cleared
 * @param silent Determine if the resolution of the query must be resolved silently or not.
 */
public record ClearRequestAst(GraphRefAst graphRef, boolean silent) implements UpdateRequestUnitAst {


    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.graphRef.accept(visitor);
    }
}
