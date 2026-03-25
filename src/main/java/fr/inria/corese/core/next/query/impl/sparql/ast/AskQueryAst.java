package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.data.impl.common.prefix.PrefixHandler;

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
public record AskQueryAst(DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause, PrefixHandler prefixHandler) implements QueryAst {
    /**
     * constructor with default prefix handler
     */
    public AskQueryAst(DatasetClauseAst datasetClause, GroupGraphPatternAst whereClause) {
        this(datasetClause, whereClause, null);
    }

    public AskQueryAst {
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
        if(datasetClause == null) {
            datasetClause = DatasetClauseAst.none();
        }
        if (prefixHandler == null) {
            prefixHandler = new PrefixHandler(true);
        }
    }
}