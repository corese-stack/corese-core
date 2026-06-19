package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent one solution mapping for VALUES, in the order it is written. A set of values for variables with null standing for UNDEF.
 * @param values
 */
public record ValueMappingAst(Map<VarAst, TermAst> values) implements VisitableAst {

    public ValueMappingAst {
        if(values == null) {
            values = new HashMap<>();
        }
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.values.forEach((varAst, termAst) -> {
            if(varAst != null) {
                varAst.accept(visitor);
            }
            if(termAst != null) {
                termAst.accept(visitor);
            }
        });
    }
}
