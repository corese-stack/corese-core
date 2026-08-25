package fr.inria.corese.core.next.data.api.term;

import java.io.Serial;
import java.util.Objects;

/**
 * Immutable default implementation of an RDF 1.2 triple term.
 */
public record SimpleTriple(Resource subject, IRI predicate, Value object) implements Triple {

    @Serial
    private static final long serialVersionUID = 7702422296929530634L;

    /**
     * Creates a triple term.
     *
     * @param subject   the triple subject
     * @param predicate the triple predicate
     * @param object    the triple object
     */
    public SimpleTriple(Resource subject, IRI predicate, Value object) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.object = Objects.requireNonNull(object, "object");
    }

    @Override
    public String stringValue() {
        return toString();
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof Triple triple
                && subject.equals(triple.subject())
                && predicate.equals(triple.predicate())
                && object.equals(triple.object()));
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public String toString() {
        return "<<(" + subject + " " + predicate + " " + object + ")>>";
    }
}
