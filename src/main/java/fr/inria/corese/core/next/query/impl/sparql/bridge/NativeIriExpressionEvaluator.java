package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.DatatypeAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IriExpressionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.IriFunctionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.UuidAst;

import java.util.UUID;

/** Evaluates expressions whose result is an IRI. */
final class NativeIriExpressionEvaluator {

    private NativeIriExpressionEvaluator() {
    }

    static DatatypeValue evaluate(IriExpressionAst expression, NativeEvaluationContext context) {
        return switch (expression) {
            case DatatypeAst datatype -> datatype(context.required(datatype.argument()), context);
            case IriFunctionAst iri -> context.values().createIRI(
                    context.termResolver().resolveIri(
                            context.required(iri.argument()).stringValue()));
            case UuidAst ignored -> context.values().createIRI("urn:uuid:" + UUID.randomUUID());
            default -> throw new UnsupportedQueryFeatureException(
                    "IRI expression is not supported yet: "
                            + expression.getClass().getSimpleName());
        };
    }

    private static DatatypeValue datatype(
            DatatypeValue value,
            NativeEvaluationContext context) {
        if (!(value instanceof Literal literal)) {
            throw new QueryEvaluationException("DATATYPE expects an RDF literal");
        }
        return context.values().createIRI(literal.getDatatype().stringValue());
    }
}
