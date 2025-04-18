package fr.inria.corese.core.next.impl.inmemory;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.base.model.AbstractModel;

import java.util.Set;

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
}
