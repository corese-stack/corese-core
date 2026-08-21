package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.rule;

/**
 * Named SPARQL clauses used in scope-validation diagnostics.
 */
enum ScopeClause {
    SELECT_PROJECTION("SELECT projection"),
    GROUP_BY("GROUP BY"),
    HAVING("HAVING"),
    ORDER_BY("ORDER BY");

    private final String label;

    ScopeClause(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }
}
