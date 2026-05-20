package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * VALUES clause. Cumulated mappings of all the VALUES clauses declared in a query
 */
public record ValuesAst(List<ValueMappingAst> mappings) {

    public ValuesAst {
        if(mappings == null) {
            mappings = new ArrayList<>();
        }
    }

    public static ValuesAst none() {
        return new ValuesAst(new ArrayList<>());
    }
}
