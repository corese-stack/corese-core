package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

import java.util.ArrayList;
import java.util.List;

/**
 * VALUES clause. Cumulated mappings of all the VALUES clauses declared in a query
 */
public record ValuesAst(List<ValueMappingAst> mappings) implements VisitableAst {

    public ValuesAst {
        if(mappings == null) {
            mappings = new ArrayList<>();
        }
    }

    public static ValuesAst none() {
        return new ValuesAst(new ArrayList<>());
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.mappings.forEach(valueMappingAst -> {
            valueMappingAst.accept(visitor);
        });
    }
}
