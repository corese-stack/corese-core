package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.Values;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.model.NodeImpl;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CoreseBindingSetTest {
    @Test
    void publicConstructorDoesNotExposeTheMutableMappingKeySet() {
        Mapping mapping = boundMapping();
        var bindings = new CoreseBindingSet(mapping);
        assertThrows(UnsupportedOperationException.class, () -> bindings.getBindingNames().clear());
        assertEquals(Set.of("x"), bindings.getBindingNames());
        assertEquals("value", bindings.getValue("x").stringValue());
        assertNotNull(mapping.getValue("x"));
    }

    @Test
    void unboundProjectionVariablesAreExcludedWithoutExposingASeparateMutableSet() {
        var bindings = new CoreseBindingSet(boundMapping(), Set.of("x", "missing"));
        assertEquals(Set.of("x"), bindings.getBindingNames());
        assertFalse(bindings.hasBinding("missing"));
        assertNull(bindings.getValue("missing"));
        assertThrows(UnsupportedOperationException.class, () -> bindings.getBindingNames().add("missing"));
        var iterator = bindings.iterator();
        assertEquals("x", iterator.next().name());
        assertFalse(iterator.hasNext());
    }

    @Test
    void fullyBoundInternalRowsCanShareTheImmutableProjection() {
        Set<String> projection = Set.of("x");
        var bindings = new CoreseBindingSet(boundMapping(), projection);
        assertSame(projection, bindings.getBindingNames(), "The per-row fast path must not copy the projection");
        assertThrows(UnsupportedOperationException.class, () -> bindings.getBindingNames().clear());
    }

    private static Mapping boundMapping() {
        return Mapping.create(new Node[]{NodeImpl.forVariable("x")},
                new Node[]{NodeImpl.forValue(Values.factory().createLiteral("value"))});
    }
}
