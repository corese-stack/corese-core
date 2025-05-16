package fr.inria.corese.core.next.impl.temp;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Model;
import fr.inria.corese.core.next.api.Namespace;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.base.model.AbstractModel;

/**
 * A model wrapper that blocks access to all statements, allowing only namespace
 * operations.
 * Typically used as a view when all statements are filtered out.
 */
public class EmptyModel extends AbstractModel {

    // --- Fields ---

    private static final long serialVersionUID = 3123007631452759092L;

    private final Model model;
    private final Set<Statement> emptySet = Collections.emptySet();

    // --- Constructors ---

    public EmptyModel(Model model) {
        this.model = model;
    }

    // --- Public Methods ---

    // -- Namespace operations delegate to the underlying model --

    @Override
    public Optional<Namespace> getNamespace(String prefix) {
        return model.getNamespace(prefix);
    }

    @Override
    public Set<Namespace> getNamespaces() {
        return model.getNamespaces();
    }

    @Override
    public Namespace setNamespace(String prefix, String name) {
        return model.setNamespace(prefix, name);
    }

    @Override
    public void setNamespace(Namespace namespace) {
        model.setNamespace(namespace);
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        return model.removeNamespace(prefix);
    }

    // -- Statement operations: read-only and always empty --

    @Override
    public Iterator<Statement> iterator() {
        return emptySet.iterator();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean add(Resource subject, IRI predicate, Value object, Resource... contexts) {
        throw new UnsupportedOperationException(
                "Cannot add statement: this model is read-only (all statements are filtered out)");
    }

    @Override
    public boolean contains(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return false;
    }

    @Override
    public Model filter(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return this;
    }

    @Override
    public boolean remove(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return false;
    }

    @Override
    public void removeTermIteration(Iterator<Statement> iter, Resource subject, IRI predicate, Value object,
            Resource... contexts) {
        // Intentionally does nothing
    }
}