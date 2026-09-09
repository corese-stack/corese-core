package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

/**
 * SPARQL {@code VALUES} table.
 *
 * <p>The same table representation is used for inline data in a group graph
 * pattern and for the optional query-level values clause. {@link #variables()}
 * retains the header of an empty table, which cannot be recovered from its
 * mappings alone.</p>
 */
public record ValuesAst(List<VarAst> variables, List<ValueMappingAst> mappings, boolean present) implements PatternAst {

    public ValuesAst {
        variables = variables == null ? inferVariables(mappings) : List.copyOf(variables);
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
    }

    public ValuesAst(List<ValueMappingAst> mappings) {
        this(inferVariables(mappings), mappings, true);
    }

    public ValuesAst(List<VarAst> variables, List<ValueMappingAst> mappings) {
        this(variables, mappings, true);
    }

    public static ValuesAst none() {
        return new ValuesAst(List.of(), List.of(), false);
    }

    private static List<VarAst> inferVariables(List<ValueMappingAst> mappings) {
        if (mappings == null) {
            return List.of();
        }
        Set<VarAst> variables = new LinkedHashSet<>();
        for (ValueMappingAst mapping : mappings) {
            for (VarAst variable : mapping.values().keySet()) {
                if (variable != null) {
                    variables.add(variable);
                }
            }
        }
        return List.copyOf(variables);
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.mappings.forEach(valueMappingAst -> valueMappingAst.accept(visitor));
    }
}
