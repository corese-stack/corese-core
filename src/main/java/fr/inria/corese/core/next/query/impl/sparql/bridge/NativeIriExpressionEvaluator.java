package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
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
            case IriFunctionAst iri -> iri(iri, context);
            case UuidAst ignored -> context.values().createIRI("urn:uuid:" + UUID.randomUUID());
            default -> throw new UnsupportedQueryFeatureException(
                    "IRI expression is not supported yet: "
                            + expression.getClass().getSimpleName());
        };
    }

    private static DatatypeValue iri(IriFunctionAst expression, NativeEvaluationContext context) {
        DatatypeValue value = context.required(expression.argument());
        if (value.isIRI()) return value;
        if (!(value instanceof Literal literal)
                || literal.getCoreDatatype() != XSDDatatype.STRING) {
            throw new QueryTypeErrorException("IRI expects an IRI or an untagged string literal");
        }
        String lexical = literal.getLabel();
        return context.values().createIRI(context.termResolver().resolveRelativeIri(lexical));
    }

    private static DatatypeValue datatype(
            DatatypeValue value,
            NativeEvaluationContext context) {
        if (!(value instanceof Literal literal)) {
            throw new QueryTypeErrorException("DATATYPE expects an RDF literal");
        }
        return context.values().createIRI(literal.getDatatype().stringValue());
    }
}
