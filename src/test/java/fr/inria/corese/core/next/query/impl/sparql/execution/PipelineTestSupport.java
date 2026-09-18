package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.factory.ValueFactory;
import fr.inria.corese.core.next.query.api.result.Binding;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.BeforeEach;

import java.util.Iterator;
import java.util.List;
import java.util.Set;


/** Shared dataset fixture for native pipeline integration tests. */
abstract class PipelineTestSupport {
    static final String ALICE = "http://example.org/alice";
    static final String BOB = "http://example.org/bob";
    static final String KNOWS = "http://example.org/knows";
    static final String NAME = "http://example.org/name";

    ValueFactory valueFactory;
    MemoryStorageManager storage;
    NextSparqlPipelineExecutor executor;

    @BeforeEach
    void setUp() {
        valueFactory = Values.factory();
        storage = MemoryStorageManager.builder().build();
        executor = new NextSparqlPipelineExecutor(storage);

        insert(iri(ALICE), iri(KNOWS), iri(BOB));
    }

    void insert(Resource subject, IRI predicate, Value object) {
        storage.mutations().add(valueFactory.createStatement(subject, predicate, object));
    }

    void insertInGraph(Resource subject, IRI predicate, Value object, Resource context) {
        storage.mutations().add(
                valueFactory.createStatement(subject, predicate, object, context));
    }

    IRI iri(String iri) {
        return valueFactory.createIRI(iri);
    }

    /**
     * Creates a one-entry {@link BindingSet} binding {@code varName} to {@code value}.
     */
    BindingSet singleBinding(String varName, Value value) {
        Binding b = new Binding() {
            @Override public String name()  { return varName; }
            @Override public Value value() { return value; }
        };
        return new BindingSet() {
            @Override public Set<String>      getBindingNames()         { return Set.of(varName); }
            @Override public boolean          hasBinding(String name)   { return varName.equals(name); }
            @Override public Value            getValue(String name)     { return varName.equals(name) ? value : null; }
            @Override public Iterator<Binding> iterator()               { return List.of(b).iterator(); }
        };
    }
}
