package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.data.impl.common.prefix.PrefixHandler;
import fr.inria.corese.core.next.data.impl.io.common.IOConstants;

import java.util.List;

/**
 * Abstract Syntax Tree (AST) representation of a SPARQL {@code DESCRIBE} query.
 * DESCRIBE (var|uri)* WHERE { pattern } or DESCRIBE (var|uri)*.
 *
 * <p>Examples:</p>
 *
 * <pre>{@code
 * DESCRIBE <http://example.org/>
 * }</pre>
 *
 * <pre>{@code
 * PREFIX foaf: <http://xmlns.com/foaf/0.1/>
 *
 * DESCRIBE ?x
 * WHERE {
 *   ?x foaf:mbox <mailto:alice@org>
 * }
 * }</pre>
 */
public record DescribeQueryAst(DatasetClauseAst datasetClause, List<TermAst> described, GroupGraphPatternAst whereClause, PrefixHandler prefixHandler) implements QueryAst {
    public DescribeQueryAst {
        described = described != null ? List.copyOf(described) : List.of();
        if (whereClause == null) {
            whereClause = new GroupGraphPatternAst(List.of());
        }
        if(datasetClause == null) {
            datasetClause = DatasetClauseAst.none();
        }
        if(prefixHandler == null) {
            prefixHandler = new PrefixHandler();
            prefixHandler.setDefaultNamespace(IOConstants.getDefaultBaseURI());
        }
    }

    /**
     * constructor with default prefix handler
     */
    public DescribeQueryAst(DatasetClauseAst datasetClause, List<TermAst> described, GroupGraphPatternAst whereClause) {
        this(datasetClause, described, whereClause, null);
    }

    /**
     * Returns {@code true} if this is a {@code DESCRIBE *} query
     * (no explicit resources specified).
     */
    public boolean isDescribeAll() {
        return described.isEmpty();
    }
}
