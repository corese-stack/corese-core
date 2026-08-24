package fr.inria.corese.core.next.data.api.term;

import java.io.Serial;
import java.util.Objects;

/**
 * Immutable default implementation of an RDF 1.2 triple term.
 */
public final class SimpleTriple implements Triple {

    @Serial
    private static final long serialVersionUID = 7702422296929530634L;

    private final Resource subject;
    private final IRI predicate;
    private final Value object;

    /**
     * Creates a triple term.
     *
     * @param subject the triple subject
     * @param predicate the triple predicate
     * @param object the triple object
     */
    public SimpleTriple(Resource subject, IRI predicate, Value object) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.object = Objects.requireNonNull(object, "object");
    }

    @Override
    public Resource getSubject() {
        return subject;
    }

    @Override
    public IRI getPredicate() {
        return predicate;
    }

    @Override
    public Value getObject() {
        return object;
    }

    @Override
    public String stringValue() {
        return toString();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof Triple triple
                && subject.equals(triple.getSubject())
                && predicate.equals(triple.getPredicate())
                && object.equals(triple.getObject()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object);
    }

    @Override
    public String toString() {
        return "<<(" + subject + " " + predicate + " " + object + ")>>";
    }
}
