package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.data.api.model.Statement;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.term.SimpleLiteral;
import fr.inria.corese.core.next.data.api.term.Value;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Test dataset generator used across storage unit and integration tests.
 */
public final class StorageBenchmark {

    private static final IRI[] PROPERTIES = {
            iri("type"), iri("group"), iri("name"), iri("knows"), iri("score"), iri("tag")
    };

    private StorageBenchmark() {
        // Utility class
    }

    private static IRI iri(String local) {
        return new SimpleIRI("urn:bench:" + local);
    }

    /**
     * Generates a deterministic synthetic dataset of statements.
     *
     * @param people number of entities (6 statements per entity)
     * @param graphs number of named graphs (0 for default graph)
     * @param skew   whether to introduce skew in friend relations
     * @return an iterable providing the statements on demand
     */
    public static Iterable<Statement> data(int people, int graphs, boolean skew) {
        return () -> new Iterator<>() {
            int position;

            @Override
            public boolean hasNext() {
                return position < people * 6;
            }

            @Override
            public Statement next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                int person = position / 6;
                int slot = position++ % 6;
                Resource subject = iri("s" + person);
                int friend = skew && person % 5 != 0 ? person % Math.max(1, people / 100) : (person + 1) % people;
                Value object = switch (slot) {
                    case 0 -> iri("Person");
                    case 1 -> iri("g" + person % 100);
                    case 2 -> new SimpleLiteral("name" + person);
                    case 3 -> iri("s" + friend);
                    case 4 -> new SimpleLiteral(Integer.toString(person % 100), new SimpleIRI("http://www.w3.org/2001/XMLSchema#integer"));
                    default -> iri("tag" + person % 32);
                };
                Resource context = graphs == 0 || person % 100 < 50 ? null : iri("graph" + person % graphs);
                return Values.factory().createStatement(subject, PROPERTIES[slot], object, context);
            }
        };
    }
}
