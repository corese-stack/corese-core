package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent one solution mapping for VALUES, in the order it is written. A set of values for variables with null standing for UNDEF.
 * @param values
 */
public record ValueMappingAst(Map<VarAst, TermAst> values) {

    public ValueMappingAst {
        if(values == null) {
            values = new HashMap<>();
        }
    }
}
