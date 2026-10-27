package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.SimpleStatement;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.SimpleBNode;
import fr.inria.corese.core.next.data.api.term.SimpleDate;
import fr.inria.corese.core.next.data.api.term.SimpleDateTime;
import fr.inria.corese.core.next.data.api.term.SimpleDecimal;
import fr.inria.corese.core.next.data.api.term.SimpleDouble;
import fr.inria.corese.core.next.data.api.term.SimpleDuration;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.term.SimpleInteger;
import fr.inria.corese.core.next.data.api.term.SimpleLiteral;
import fr.inria.corese.core.next.data.api.term.SimpleTime;
import fr.inria.corese.core.next.data.api.term.SimpleTriple;
import fr.inria.corese.core.next.data.api.term.Triple;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.Objects;

/**
 * Defensive RDF term and statement capture utilities for in-memory graph storage.
 *
 * <p>
 * Ensures that incoming RDF terms, statements, and search patterns are converted to immutable, safe internal
 * representations, and enables term sharing across statements without altering RDF semantics.
 * </p>
 */
final class StatementCapture {

    private StatementCapture() {
        // Utility class
    }

    /**
     * Converts an arbitrary RDF value into an immutable, thread-safe internal representation.
     *
     * @param value non-null RDF value to capture
     * @return the captured immutable RDF value
     * @throws IllegalArgumentException if the RDF term implementation is unsupported
     * @throws NullPointerException     if {@code value} is null
     */
    static Value immutable(final Value value) {
        Objects.requireNonNull(value, "value");
        if (value instanceof Literal literal && !(literal.getDatatype() instanceof SimpleIRI)) {
            final IRI datatype = (IRI) immutable(literal.getDatatype());
            if (value instanceof SimpleLiteral) {
                return literal.getLanguage().map(language -> (Value) new SimpleLiteral(literal.getLabel(), language))
                        .orElseGet(() -> new SimpleLiteral(literal.getLabel(), datatype));
            }
            return Values.factory().createLiteral(literal.getLabel(), datatype);
        }
        if (value instanceof SimpleIRI || value instanceof SimpleBNode || value instanceof SimpleLiteral
                || value instanceof SimpleInteger || value instanceof SimpleDecimal || value instanceof SimpleDouble
                || value instanceof SimpleDate || value instanceof SimpleDateTime || value instanceof SimpleTime
                || value instanceof SimpleDuration) {
            return value;
        }
        if (value instanceof IRI iri) {
            return new SimpleIRI(iri.stringValue());
        }
        if (value instanceof BNode blank) {
            return new SimpleBNode(blank.getID());
        }
        if (value instanceof Literal literal) {
            return literal.getLanguage().map(language -> (Value) new SimpleLiteral(literal.getLabel(), language))
                    .orElseGet(() -> new SimpleLiteral(literal.getLabel(), (IRI) immutable(literal.getDatatype())));
        }
        if (value instanceof Triple triple) {
            return new SimpleTriple((Resource) immutable(triple.subject()), (IRI) immutable(triple.predicate()),
                    immutable(triple.object()));
        }
        throw new IllegalArgumentException("Unsupported RDF term: " + value.getClass().getName());
    }

    /**
     * Converts an arbitrary statement into an immutable representation with captured components.
     *
     * @param statement non-null statement to capture
     * @return an immutable captured statement
     * @throws NullPointerException if {@code statement} is null
     */
    static Statement immutable(final Statement statement) {
        Objects.requireNonNull(statement, "statement");
        final Resource context = statement.getContext();
        final Resource subject = (Resource) immutable(statement.getSubject());
        final IRI predicate = (IRI) immutable(statement.getPredicate());
        final Value object = immutable(statement.getObject());
        final Resource graph = context == null ? null : (Resource) immutable(context);
        if (statement.getClass() == SimpleStatement.class && subject == statement.getSubject()
                && predicate == statement.getPredicate() && object == statement.getObject() && graph == context) {
            return statement;
        }
        return new SimpleStatement(subject, predicate, object, graph);
    }

    /**
     * Converts a statement pattern into an immutable representation with captured components.
     *
     * @param pattern non-null search pattern to capture
     * @return an immutable statement pattern
     * @throws NullPointerException if {@code pattern} is null
     */
    static StatementPattern immutable(final StatementPattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        final Resource[] contexts = pattern.getContexts();
        for (int i = 0; i < contexts.length; i++) {
            if (contexts[i] != null) {
                contexts[i] = (Resource) immutable(contexts[i]);
            }
        }
        return StatementPattern.of(pattern.getSubject() == null ? null : (Resource) immutable(pattern.getSubject()),
                pattern.getPredicate() == null ? null : (IRI) immutable(pattern.getPredicate()),
                pattern.getObject() == null ? null : immutable(pattern.getObject()), contexts);
    }

    /**
     * Reuses compatible canonical RDF values without changing lexical identity or implementation type.
     *
     * @param statement statement whose RDF identities have been captured
     * @param subject   canonical subject key
     * @param predicate canonical predicate key
     * @param object    canonical object key
     * @param context   canonical graph identifier; null denotes the default graph
     * @return the original statement when all values are already shared, otherwise a statement using compatible
     *         canonical values
     */
    static Statement sharedStatement(final Statement statement, final TermKey subject, final TermKey predicate,
            final TermKey object, final Resource context) {
        final Resource s = (Resource) sharedValue(statement.getSubject(), subject.value());
        final IRI p = (IRI) sharedValue(statement.getPredicate(), predicate.value());
        final Value o = sharedValue(statement.getObject(), object.value());
        final Resource g = context == null ? null : (Resource) sharedValue(statement.getContext(), context);
        return s == statement.getSubject() && p == statement.getPredicate() && o == statement.getObject()
                && g == statement.getContext() ? statement : new SimpleStatement(s, p, o, g);
    }

    /**
     * Reuses a compatible immutable scalar value while preserving its implementation and datatype.
     *
     * @param original  original captured RDF value
     * @param canonical compatible canonical RDF value
     * @return the compatible canonical value, or the original value when pooling is unsuitable
     */
    private static Value sharedValue(final Value original, final Value canonical) {
        if (original == canonical || original.getClass() != canonical.getClass()) {
            return original;
        }
        if (original instanceof Literal literal && canonical instanceof Literal other
                && !sameLiteralImplementation(literal, other)) {
            return original;
        }
        if (original instanceof SimpleIRI iri && canonical instanceof SimpleIRI other) {
            return iri.getNamespace().equals(other.getNamespace()) && iri.getLocalName().equals(other.getLocalName())
                    ? canonical
                    : original;
        }
        if (original instanceof SimpleLiteral literal && canonical instanceof SimpleLiteral other) {
            return literal.getCoreDatatype() == other.getCoreDatatype() ? canonical : original;
        }
        // Calendar-backed terms and nested triples are deliberately not pooled.
        return original instanceof SimpleBNode || original instanceof SimpleInteger || original instanceof SimpleDecimal
                || original instanceof SimpleDouble || original instanceof SimpleDuration ? canonical : original;
    }

    /**
     * Checks literal datatype implementation compatibility using the existing comparison order.
     *
     * @param literal original captured literal
     * @param other   value or search object to compare with the stored key
     * @return true when datatype implementation and native components permit sharing
     */
    private static boolean sameLiteralImplementation(final Literal literal, final Literal other) {
        if (literal.getCoreDatatype() != other.getCoreDatatype()) {
            return false;
        }
        final IRI datatype = literal.getDatatype();
        final IRI canonicalDatatype = other.getDatatype();

        return datatype == canonicalDatatype || (datatype.getClass() == canonicalDatatype.getClass()
                && datatype.getNamespace().equals(canonicalDatatype.getNamespace())
                && datatype.getLocalName().equals(canonicalDatatype.getLocalName()));
    }
}
