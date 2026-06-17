package fr.inria.corese.core.next.query.impl.parser.semantic.validator;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.api.validation.QueryValidationResult;
import fr.inria.corese.core.next.query.api.validation.QueryValidator;
import fr.inria.corese.core.next.query.impl.parser.semantic.rule.FilterArgumentsValidationRule;
import fr.inria.corese.core.next.query.impl.parser.semantic.rule.OrderByScopeValidationRule;
import fr.inria.corese.core.next.query.impl.parser.semantic.rule.SelectProjectionScopeValidationRule;
import fr.inria.corese.core.next.query.impl.parser.semantic.rule.SemanticValidationRule;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal semantic validator for SPARQL query ASTs produced by the
 * {@code next} parser.
 */
public final class SparqlQuerySemanticValidator implements QueryValidator<QueryAst> {

    private final List<SemanticValidationRule> rules;

    public SparqlQuerySemanticValidator() {
        this(List.of(
                new SelectProjectionScopeValidationRule(),
                new OrderByScopeValidationRule(),
                new FilterArgumentsValidationRule()));
    }

    /** Package-private for tests or future composition of rule sets. */
    SparqlQuerySemanticValidator(List<SemanticValidationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public QueryValidationResult validate(QueryAst input) {
        if (input == null) {
            return new QueryValidationResult(List.of());
        }

        List<QueryDiagnostic> diagnostics = new ArrayList<>();
        for (SemanticValidationRule rule : rules) {
            diagnostics.addAll(rule.validate(input));
        }
        return new QueryValidationResult(diagnostics);
    }
}
