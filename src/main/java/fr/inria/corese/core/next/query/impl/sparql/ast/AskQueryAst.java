package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code ASK} query.
 * ASK WHERE { pattern } returns a boolean.
 *
 * <pre>{@code
 * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
 *
 * ASK {
 *   ?x foaf:name  "Alice" ;
 *      foaf:mbox  <mailto:alice@work.example>
 * }
 * }</pre>
 */
public record AskQueryAst(DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause,
                          SolutionModifierAst solutionModifier, QueryPrologueAst prologue) implements QueryAst {

    public AskQueryAst(DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause) {
        this(datasetClause, whereClause, null, null);
    }

    public AskQueryAst {
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
        if (datasetClause == null) {
            datasetClause = DatasetClauseAst.none();
        }
        if (prologue == null) {
            prologue = QueryPrologueAst.empty();
        }
        if (solutionModifier == null) {
            solutionModifier = SolutionModifierAst.empty();
        }
    }
}