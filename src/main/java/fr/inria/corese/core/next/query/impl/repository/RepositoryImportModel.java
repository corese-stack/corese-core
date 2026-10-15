package fr.inria.corese.core.next.query.impl.repository;

import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.data.api.model.Model;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.namespace.Namespace;
import fr.inria.corese.core.next.data.api.term.BNode;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.spi.model.AbstractModel;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Parser-local model view that scopes source blank nodes and redirects writes to
 * optional target contexts. No statement collection or blank-node dictionary is
 * retained. Reads and namespace operations delegate to the live backing model.
 * Instances are confined to the thread and lifetime of one import.
 */
@SuppressWarnings("java:S2160") // Inherit RDF set equality; adapter configuration is not part of statement equality.
final class RepositoryImportModel extends AbstractModel {
    private final Model target;
    private final ValueFactory factory;
    private final Resource[] contexts;
    private final String blankNodePrefix;

    /**
     * Creates a view for one import.
     *
     * @param target live model over the importing connection's storage
     * @param factory RDF value factory
     * @param contexts non-null target contexts; empty preserves document contexts
     */
    RepositoryImportModel(Model target, ValueFactory factory, Resource[] contexts) {
        this.target = target;
        this.factory = factory;
        this.contexts = contexts.clone();
        String scope = factory.createBNode().getID();
        this.blankNodePrefix = scope.length() + "-" + scope + "-";
    }

    @Override
    public boolean add(Resource subject, IRI predicate, Value object, Resource... sourceContexts) {
        Resource scopedSubject = scope(subject);
        Value scopedObject = object instanceof BNode blank ? scope(blank) : object;
        if (contexts.length != 0) {
            return target.add(scopedSubject, predicate, scopedObject, contexts);
        }
        if (sourceContexts == null || sourceContexts.length == 0) {
            return target.add(scopedSubject, predicate, scopedObject);
        }
        boolean changed = false;
        for (Resource context : sourceContexts) {
            changed |= target.add(scopedSubject, predicate, scopedObject, scope(context));
        }
        return changed;
    }

    private Resource scope(Resource resource) {
        return resource instanceof BNode blank
                ? factory.createBNode(blankNodePrefix + blank.getID()) : resource;
    }

    @Override
    public boolean contains(Resource subject, IRI predicate, Value object, Resource... selected) {
        return target.contains(subject, predicate, object, selected);
    }

    @Override
    public boolean remove(Resource subject, IRI predicate, Value object, Resource... selected) {
        return target.remove(subject, predicate, object, selected);
    }

    @Override
    public Model filter(Resource subject, IRI predicate, Value object, Resource... selected) {
        return target.filter(subject, predicate, object, selected);
    }

    @Override
    public void removeTermIteration(Iterator<Statement> iterator, Resource subject, IRI predicate,
            Value object, Resource... selected) {
        target.remove(subject, predicate, object, selected);
    }

    @Override
    public Iterator<Statement> iterator() {
        return target.iterator();
    }

    @Override
    public int size() {
        return target.size();
    }

    @Override
    public Set<Namespace> getNamespaces() {
        return target.getNamespaces();
    }

    @Override
    public void setNamespace(Namespace namespace) {
        target.setNamespace(namespace);
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        return target.removeNamespace(prefix);
    }
}
