package fr.inria.corese.core.next.storage.api.model;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StatementPattern.
 */
@DisplayName("StatementPattern Tests")
class StatementPatternTest {

    @Test
    @DisplayName("Should build pattern with all components using builder")
    void testBuilder() {
        Resource subject = Mockito.mock(Resource.class);
        IRI predicate = Mockito.mock(IRI.class);
        Value object = Mockito.mock(Value.class);
        Resource ctx1 = Mockito.mock(Resource.class);
        Resource ctx2 = Mockito.mock(Resource.class);

        StatementPattern pattern = StatementPattern.builder()
                .subject(subject)
                .predicate(predicate)
                .object(object)
                .contexts(ctx1, ctx2)
                .build();

        assertEquals(subject, pattern.getSubject());
        assertEquals(predicate, pattern.getPredicate());
        assertEquals(object, pattern.getObject());
        assertEquals(2, pattern.getContexts().length);
    }

    @Test
    @DisplayName("Should create match-all pattern with all wildcards")
    void testMatchAll() {
        StatementPattern pattern = StatementPattern.matchAll();

        assertNull(pattern.getSubject());
        assertNull(pattern.getPredicate());
        assertNull(pattern.getObject());
        assertEquals(0, pattern.getContexts().length);
        assertTrue(pattern.matchesAll());
    }

    @Test
    @DisplayName("Should create pattern using factory method of()")
    void testFactoryMethod() {
        Resource subject = Mockito.mock(Resource.class);
        IRI predicate = Mockito.mock(IRI.class);
        Value object = Mockito.mock(Value.class);
        Resource ctx = Mockito.mock(Resource.class);

        StatementPattern pattern = StatementPattern.of(subject, predicate, object, ctx);

        assertEquals(subject, pattern.getSubject());
        assertEquals(predicate, pattern.getPredicate());
        assertEquals(object, pattern.getObject());
        assertEquals(1, pattern.getContexts().length);
        assertFalse(pattern.matchesAll());
    }

    @Test
    @DisplayName("Should correctly identify wildcard components")
    void testWildcardCheckers() {
        StatementPattern allWildcards = StatementPattern.matchAll();

        assertTrue(allWildcards.isSubjectWildcard());
        assertTrue(allWildcards.isPredicateWildcard());
        assertTrue(allWildcards.isObjectWildcard());
        assertTrue(allWildcards.isContextWildcard());

        Resource subject = Mockito.mock(Resource.class);
        StatementPattern withSubject = StatementPattern.builder()
                .subject(subject)
                .build();

        assertFalse(withSubject.isSubjectWildcard());
        assertTrue(withSubject.isPredicateWildcard());
        assertTrue(withSubject.isObjectWildcard());
        assertTrue(withSubject.isContextWildcard());
    }

    @Test
    @DisplayName("Should implement equals() and hashCode() correctly")
    void testEquality() {
        Resource subject = Mockito.mock(Resource.class);
        IRI predicate = Mockito.mock(IRI.class);

        StatementPattern pattern1 = StatementPattern.builder()
                .subject(subject)
                .predicate(predicate)
                .build();

        StatementPattern pattern2 = StatementPattern.builder()
                .subject(subject)
                .predicate(predicate)
                .build();

        assertEquals(pattern1, pattern2);
        assertEquals(pattern1.hashCode(), pattern2.hashCode());
    }

    @Test
    @DisplayName("Should include class name in toString() output")
    void testToString() {
        StatementPattern pattern = StatementPattern.matchAll();
        String str = pattern.toString();

        assertTrue(str.contains("StatementPattern"));
    }

    @Test
    @DisplayName("Should ensure contexts array is immutable")
    void testContextsImmutability() {
        Resource ctx1 = Mockito.mock(Resource.class);
        Resource ctx2 = Mockito.mock(Resource.class);

        StatementPattern pattern = StatementPattern.builder()
                .contexts(ctx1, ctx2)
                .build();

        Resource[] contexts = pattern.getContexts();
        contexts[0] = null;

        assertNotNull(pattern.getContexts()[0]);
    }
}
