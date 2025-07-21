package fr.inria.corese.core.next.impl.inmemory;

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
 * InMemoryModel is a model that stores RDF data in memory.
 * DRAFT
 */
public class InMemoryModel extends AbstractModel {

    private static final long serialVersionUID = 1L;

    @Override
    public Iterable<Statement> getStatements(Resource subj, IRI pred, Value obj, Resource... contexts) {
        return null;
    }

    @Override
    public Set<Resource> contexts() {
        return null;
    }

    @Override
    public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'contains'");
    }

    @Override
    public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    @Override
    public Set<Namespace> getNamespaces() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getNamespaces'");
    }

    @Override
    public void setNamespace(Namespace namespace) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setNamespace'");
    }

    @Override
    public Optional<Namespace> removeNamespace(String prefix) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeNamespace'");
    }

    @Override
    public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'filter'");
    }

    @Override
    public Iterator<Statement> iterator() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'iterator'");
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'size'");
    }

    @Override
    public void removeTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
            Resource... contexts) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeTermIteration'");
    }
}
