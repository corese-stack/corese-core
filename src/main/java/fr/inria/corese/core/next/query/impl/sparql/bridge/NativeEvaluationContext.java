package fr.inria.corese.core.next.query.impl.sparql.bridge;


import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.QueryTypeErrorException;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.exception.IncorrectFormatException;
import fr.inria.corese.core.next.data.api.exception.InvalidDatatypeException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.query.impl.engine.spi.Evaluator;
import fr.inria.corese.core.next.query.impl.engine.spi.Producer;
import fr.inria.corese.core.next.query.impl.engine.eval.SparqlException;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.time.OffsetDateTime;
import java.util.Objects;

/** Query-scoped collaborators and value checks shared by native expression evaluators. */
final class NativeEvaluationContext {

    private final ValueFactory values = Values.factory();
    private final Evaluator evaluator;
    private final Environment environment;
    private final Producer producer;
    private final WhereCompiler whereCompiler;

    NativeEvaluationContext(
            Evaluator evaluator,
            Environment environment,
            Producer producer,
            WhereCompiler whereCompiler) {
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
        this.environment = environment;
        this.producer = producer;
        this.whereCompiler = whereCompiler;
    }

    DatatypeValue evaluate(TermAst expression) {
        try {
            return NativeExpressionEvaluator.evaluateExpression(expression, this);
        } catch (QueryEvaluationException | UnsupportedQueryFeatureException failure) {
            throw failure;
        } catch (IllegalArgumentException | ArithmeticException | IncorrectOperationException
                 | IncorrectFormatException | InvalidDatatypeException failure) {
            throw new QueryTypeErrorException(
                    "Failed to evaluate SPARQL expression " + expression.getName(), failure);
        }
    }

    DatatypeValue variable(String name) {
        if (environment == null) {
            return null;
        }
        Node node = environment.getNode(name);
        return node == null ? null : node.getDatatypeValue();
    }

    DatatypeValue constant(TermAst expression) {
        return termResolver().toNode(expression).getDatatypeValue();
    }

    DatatypeValue required(TermAst expression) {
        return required(evaluate(expression));
    }

    DatatypeValue required(DatatypeValue value) {
        if (value == null) {
            throw new QueryTypeErrorException("Expression references an unbound variable");
        }
        return value;
    }

    Literal literal(TermAst expression) {
        DatatypeValue value = required(expression);
        if (value instanceof Literal literal) {
            return literal;
        }
        throw new QueryTypeErrorException("Expected an RDF literal");
    }

    Literal stringLiteral(TermAst expression) {
        Literal literal = literal(expression);
        if (literal.getCoreDatatype() == XSDDatatype.STRING
                || literal.getCoreDatatype() == RDFDatatype.LANGSTRING) {
            return literal;
        }
        throw new QueryTypeErrorException("Expected a string RDF literal");
    }

    Literal simpleString(TermAst expression) {
        Literal literal = stringLiteral(expression);
        if (literal.getLanguage().isPresent()) {
            throw new QueryTypeErrorException("Expected an untagged string literal");
        }
        return literal;
    }

    boolean effectiveBooleanValue(TermAst expression) {
        return effectiveBooleanValue(required(expression));
    }

    boolean effectiveBooleanValue(DatatypeValue value) {
        DatatypeValue boundValue = required(value);
        if (!(boundValue instanceof Literal literal)) {
            throw new QueryTypeErrorException("RDF term has no SPARQL effective boolean value");
        }
        if (literal.getCoreDatatype() == XSDDatatype.BOOLEAN) {
            String lexical = NativeNumericValues.whitespaceCollapsed(literal.getLabel());
            return lexical.equals("true") || lexical.equals("1");
        }
        if (literal.isNumber()) {
            try {
                return NativeNumericValues.booleanValue(literal);
            } catch (QueryTypeErrorException invalidLexicalForm) {
                return false;
            }
        }
        if (literal.getCoreDatatype() == XSDDatatype.STRING
                || literal.getCoreDatatype() == RDFDatatype.LANGSTRING) {
            return !literal.getLabel().isEmpty();
        }
        throw new QueryTypeErrorException("RDF literal has no SPARQL effective boolean value");
    }

    boolean exists(GroupGraphPatternAst pattern) {
        if (environment == null || environment.getEval() == null || whereCompiler == null) {
            throw new QueryEvaluationException("EXISTS requires an active query evaluation context");
        }
        try {
            return environment.getEval().exists(
                    producer, environment.getGraphNode(), whereCompiler.compile(pattern));
        } catch (SparqlException exception) {
            throw new QueryEvaluationException("Failed to evaluate EXISTS graph pattern", exception);
        }
    }

    DatatypeValue blankNode(TermAst label) {
        if (label == null) return values.createBNode();
        String key = simpleString(label).getLabel();
        if (environment == null || environment.getMap() == null) {
            throw new QueryEvaluationException("BNODE(label) requires a solution evaluation context");
        }
        return environment.getMap().computeIfAbsent(key, ignored -> values.createBNode());
    }

    OffsetDateTime queryEvaluationTime() {
        return evaluator.getQueryEvaluationTime();
    }

    ValueFactory values() {
        return values;
    }

    SparqlTermResolver termResolver() {
        return whereCompiler == null
                ? new SparqlTermResolver(null)
                : whereCompiler.termResolver();
    }
}
