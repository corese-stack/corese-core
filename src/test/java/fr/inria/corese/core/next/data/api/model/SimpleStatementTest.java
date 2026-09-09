package fr.inria.corese.core.next.data.api.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import fr.inria.corese.core.next.data.api.term.*;
import fr.inria.corese.core.next.data.spi.model.AbstractStatement;

class SimpleStatementTest {
    private final Resource subject = new SimpleBNode("s");
    private final IRI predicate = new SimpleIRI("urn:p");
    private final Value object = new SimpleLiteral("o");
    private final Resource context = new SimpleIRI("urn:g");

    @Test
    void constructorsAndGetters() {
        Statement triple = new SimpleStatement(subject, predicate, object);
        assertSame(subject, triple.getSubject());
        assertSame(predicate, triple.getPredicate());
        assertSame(object, triple.getObject());
        assertNull(triple.getContext());
        Statement quad = new SimpleStatement(subject, predicate, object, context);
        assertSame(subject, quad.getSubject());
        assertSame(predicate, quad.getPredicate());
        assertSame(object, quad.getObject());
        assertSame(context, quad.getContext());
        assertEquals(triple, new SimpleStatement(subject, predicate, object, null));
    }

    @Test
    void equalsAndHashCodeContract() {
        Statement first = new SimpleStatement(subject, predicate, object, context);
        Statement second = new SimpleStatement(new SimpleBNode("s"), new SimpleIRI("urn:p"), new SimpleLiteral("o"), new SimpleIRI("urn:g"));
        Statement otherImplementation = new AbstractStatement() {
            @Override public Resource getSubject() { return subject; }
            @Override public IRI getPredicate() { return predicate; }
            @Override public Value getObject() { return object; }
            @Override public Resource getContext() { return context; }
        };
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, otherImplementation);
        assertEquals(first, otherImplementation);
        assertEquals(otherImplementation, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(first.hashCode(), otherImplementation.hashCode());
        assertEquals(Objects.hash(subject, predicate, object, context), first.hashCode());
        assertEquals(1, new HashSet<>(List.of(first, second, otherImplementation)).size());
        assertNotEquals((Object) null, first);
        assertNotEquals((Object) "statement", first);
        assertNotEquals(first, new SimpleStatement(context, predicate, object, context));
        assertNotEquals(first, new SimpleStatement(subject, new SimpleIRI("urn:other"), object, context));
        assertNotEquals(first, new SimpleStatement(subject, predicate, new SimpleLiteral("other"), context));
        assertNotEquals(first, new SimpleStatement(subject, predicate, object));
        assertNotEquals(first, new SimpleStatement(subject, predicate, object, subject));
    }

    @Test
    void rejectsNullComponents() {
        assertEquals("subject", assertThrows(NullPointerException.class, () -> new SimpleStatement(null, predicate, object)).getMessage());
        assertEquals("predicate", assertThrows(NullPointerException.class, () -> new SimpleStatement(subject, null, object)).getMessage());
        assertEquals("object", assertThrows(NullPointerException.class, () -> new SimpleStatement(subject, predicate, null)).getMessage());
        assertThrows(NullPointerException.class, () -> new SimpleStatement(null, predicate, object, context));
        assertThrows(NullPointerException.class, () -> new SimpleStatement(subject, null, object, context));
        assertThrows(NullPointerException.class, () -> new SimpleStatement(subject, predicate, null, context));
    }

    @Test
    void serializationRoundTrip() throws Exception {
        Statement original = new SimpleStatement(subject, predicate, object, context);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            Object restored = input.readObject();
            assertEquals(original, restored);
            assertEquals(original.hashCode(), restored.hashCode());
        }
    }
}
