package fr.inria.corese.core.next.query.impl.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;

import java.util.List;

/**
 * Internal semantic validation rule applied to a parsed query AST.
 */
public interface SemanticValidationRule {

    List<QueryDiagnostic> validate(QueryAst queryAst);
}
