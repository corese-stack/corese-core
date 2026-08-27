package fr.inria.corese.core.next.data.impl.model;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.namespace.Namespace;
import fr.inria.corese.core.next.data.spi.model.AbstractModel;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Small insertion-ordered in-memory model used by the public I/O facade.
 * Its runtime value factory is intentionally retained as part of the live model
 * state even when a factory implementation is not Java-serializable.
 */
@SuppressWarnings("java:S2160")
public final class LinkedHashModel extends AbstractModel {

    private final ValueFactory valueFactory;
    private final Set<Statement> statements = new LinkedHashSet<>();
    private final Set<Namespace> namespaces = new LinkedHashSet<>();

    public LinkedHashModel(ValueFactory valueFactory) {
        this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
    }

    public LinkedHashModel(ValueFactory valueFactory, Iterable<Statement> source) {
        this(valueFactory);
        Objects.requireNonNull(source, "source").forEach(statements::add);
    }

    @Override
    public Set<Namespace> getNamespaces() {
        return Set.copyOf(namespaces);
    }

    @Override
    public void setNamespace(Namespace namespace) {
        Objects.requireNonNull(namespace, "namespace");
        removeNamespace(namespace.getPrefix());
        namespaces.add(namespace);
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        Optional<Namespace> existing = getNamespace(prefix);
        existing.ifPresent(namespaces::remove);
        return existing;
    }

    @Override
    public boolean add(Resource subject, IRI predicate, Value object, Resource... contexts) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(predicate, "predicate");
        Objects.requireNonNull(object, "object");
        if (contexts == null || contexts.length == 0) {
            return statements.add(valueFactory.createStatement(subject, predicate, object));
        }
        boolean changed = false;
        for (Resource context : contexts) {
            changed |= statements.add(valueFactory.createStatement(subject, predicate, object, context));
        }
        return changed;
    }

    @Override
    public boolean contains(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return statements.stream().anyMatch(statement -> matches(statement, subject, predicate, object, contexts));
    }

    @Override
    public boolean remove(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return statements.removeIf(statement -> matches(statement, subject, predicate, object, contexts));
    }

    @Override
    public Model filter(Resource subject, IRI predicate, Value object, Resource... contexts) {
        LinkedHashModel filtered = new LinkedHashModel(valueFactory);
        namespaces.forEach(filtered::setNamespace);
        statements.stream()
                .filter(statement -> matches(statement, subject, predicate, object, contexts))
                .forEach(filtered.statements::add);
        return filtered;
    }

    @Override
    public Iterator<Statement> iterator() {
        return statements.iterator();
    }

    @Override
    public int size() {
        return statements.size();
    }

    @Override
    public void removeTermIteration(
            Iterator<Statement> iterator,
            Resource subject,
            IRI predicate,
            Value object,
            Resource... contexts) {
        while (iterator.hasNext()) {
            if (matches(iterator.next(), subject, predicate, object, contexts)) {
                iterator.remove();
            }
        }
    }

    private static boolean matches(
            Statement statement,
            Resource subject,
            IRI predicate,
            Value object,
            Resource... contexts) {
        return (subject == null || subject.equals(statement.getSubject()))
                && (predicate == null || predicate.equals(statement.getPredicate()))
                && (object == null || object.equals(statement.getObject()))
                && matchesContext(statement.getContext(), contexts);
    }

    private static boolean matchesContext(Resource context, Resource... contexts) {
        if (contexts == null || contexts.length == 0) {
            return true;
        }
        for (Resource candidate : contexts) {
            if (Objects.equals(candidate, context)) {
                return true;
            }
        }
        return false;
    }
}
