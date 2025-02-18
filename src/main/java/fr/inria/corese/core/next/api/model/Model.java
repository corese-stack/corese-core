package fr.inria.corese.core.next.api.model;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

public interface Model extends Set<Statement>, Serializable {
    Model unmodifiable();

    Namespace setNamespace(String prefix, String name);

    void setNamespace(Namespace namespace);

    Optional<Namespace> removeNamespace(String prefix);

    boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts);

    boolean add(Resource subj, IRI pred, Value obj, Resource... contexts);

    boolean clear(Resource... context);

    boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts);

    default Iterable<Statement> getStatements(Resource subject, IRI predicate, Value object,
                                              Resource... contexts) {
        return () -> filter(subject, predicate, object, contexts).iterator();
    }

    Model filter(Resource subj, IRI pred, Value obj, Resource... contexts);

    Set<Resource> subjects();

    Set<IRI> predicates();

    Set<Value> objects();

    default Set<Resource> contexts() {
//        Set<Resource> subjects = stream().map(st -> st.getContext()).collect(Collectors.toSet());
        Set<Resource> subjects = null;
        return subjects;
    }
}
