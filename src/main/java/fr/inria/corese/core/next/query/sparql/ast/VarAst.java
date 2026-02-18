package fr.inria.corese.core.next.query.sparql.ast;

/**
 * Variable in a triple pattern (e.g. ?s, $x).
 */
public record VarAst(String name) implements TermAst {
    public VarAst {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Variable name is null or blank");
        }
    }
}
