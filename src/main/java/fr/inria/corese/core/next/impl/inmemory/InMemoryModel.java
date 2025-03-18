package fr.inria.corese.core.next.impl.inmemory;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Resource;
import fr.inria.corese.core.next.api.model.Statement;
import fr.inria.corese.core.next.api.model.Value;
import fr.inria.corese.core.next.api.model.base.AbstractModel;

import java.util.Set;

public class InMemoryModel extends AbstractModel {
    @Override
    public Iterable<Statement> getStatements(Resource subject, IRI predicate, Value object, Resource... contexts) {
        return super.getStatements(subject, predicate, object, contexts);
    }

    @Override
    public Set<Resource> contexts() {
        return super.contexts();
    }
}
