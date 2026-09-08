package fr.inria.corese.core.next.data.api.model;

import java.io.Serial;
import java.util.Objects;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.spi.model.AbstractStatement;

/** Immutable statement with an optional graph context. */
public class SimpleStatement extends AbstractStatement {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Resource subject;
    private final IRI predicate;
    private final Value object;
    private final Resource context;

    public SimpleStatement(Resource subject, IRI predicate, Value object) {
        this(subject, predicate, object, null);
    }

    public SimpleStatement(Resource subject, IRI predicate, Value object, Resource context) {
        this.subject = Objects.requireNonNull(subject, "subject");
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.object = Objects.requireNonNull(object, "object");
        this.context = context;
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
    public Resource getContext() {
        return context;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Statement statement)) {
            return false;
        }
        return subject.equals(statement.getSubject())
                && predicate.equals(statement.getPredicate())
                && object.equals(statement.getObject())
                && Objects.equals(context, statement.getContext());
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object, context);
    }
}
