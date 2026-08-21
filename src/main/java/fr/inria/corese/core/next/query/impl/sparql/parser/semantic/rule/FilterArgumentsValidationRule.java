package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.rule;

import fr.inria.corese.core.next.query.api.validation.QueryDiagnostic;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AbstractAstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;

import java.util.ArrayList;
import java.util.List;

import static fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.SemanticValidationUtils.*;

/**
 * Validates that FILTER and HAVING expressions are compatible with SPARQL
 * effective boolean value evaluation.
 */
public final class FilterArgumentsValidationRule extends AbstractSemanticValidationRule {

    @Override
    protected String getDiagnosticSource() {
        return FilterArgumentsValidationRule.class.getSimpleName();
    }

    @Override
    public List<QueryDiagnostic> validate(QueryAst queryAst) {
        FilterArgumentsValidationVisitor visitor = new FilterArgumentsValidationVisitor();
        queryAst.accept(visitor);
        return visitor.getResult();
    }

    private class FilterArgumentsValidationVisitor extends AbstractAstVisitor {
        private final List<QueryDiagnostic> result = new ArrayList<>();

        public List<QueryDiagnostic> getResult() {
            return result;
        }

        @Override
        public void visit(PatternAst patternAst) {
            if (patternAst instanceof FilterAst(TermAst operator) && !isPotentialBooleanCompatible(operator)) {
                result.add(buildIncorrectTypeDiagnostic(operator.getName(), "FILTER", "boolean"));
            }
        }

        @Override
        public void visit(HavingAst havingAst) {
            for (TermAst condition : havingAst.conditions()) {
                if (!isPotentialBooleanCompatible(condition)) {
                    result.add(buildIncorrectTypeDiagnostic(condition.getName(), "HAVING", "boolean"));
                }
            }
        }
    }
}
