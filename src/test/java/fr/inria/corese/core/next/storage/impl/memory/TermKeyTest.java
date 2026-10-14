package fr.inria.corese.core.next.storage.impl.memory;

import fr.inria.corese.core.next.data.api.term.SimpleIRI;
import fr.inria.corese.core.next.data.api.term.SimpleLiteral;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TermKeyTest {
    @Test void nativeIRIHashMatchesFullStringAcrossSplitsUnicodeAndLongNames() {
        Random random = new Random(813719);
        for (int length : new int[]{0, 1, 7, 31, 32, 33, 63, 256, 2000}) {
            StringBuilder local = new StringBuilder();
            for (int i = 0; i < length; i++) local.append((char) ('a' + random.nextInt(26)));
            // A fragment avoids the unrelated namespace parser's long-path regex.
            String full = "urn:hash:#" + local;
            for (int split : new int[]{4, 10, full.length() - 1, full.length()}) {
                var iri = new SimpleIRI(full.substring(0, split), full.substring(split));
                assertEquals(full.hashCode(), new TermKey(iri).hashCode(), "length=" + length + ", split=" + split);
            }
        }
        String unicode = "urn:hash:#é😀";
        for (int split : new int[]{4, 10, unicode.length() - 1, unicode.length()}) {
            var iri = new SimpleIRI(unicode.substring(0, split), unicode.substring(split));
            assertEquals(unicode.hashCode(), new TermKey(iri).hashCode());
        }
    }

    @Test void equalIRIsWithDifferentViewsRemainLookupCompatibleAndCollisionsStayDistinct() {
        var first = new TermKey(new SimpleIRI("urn:", "view:item"));
        var second = new TermKey(new SimpleIRI("urn:view:", "item"));
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        HashMap<TermKey, String> map = new HashMap<>();
        map.put(first, "same IRI");
        assertEquals("same IRI", map.get(second));
        var collisionA = new TermKey(new SimpleIRI("urn:", "Aa"));
        var collisionB = new TermKey(new SimpleIRI("urn:", "BB"));
        assertEquals(collisionA.hashCode(), collisionB.hashCode());
        assertNotEquals(collisionA, collisionB);
        map.put(collisionA, "A");
        map.put(collisionB, "B");
        assertEquals("A", map.get(new TermKey(new SimpleIRI("urn:Aa"))));
        assertEquals("B", map.get(new TermKey(new SimpleIRI("urn:BB"))));
    }

    @Test void literalDatatypeViewsShareKeysWhileLexicalFormsRemainDistinct() {
        var datatypeA = new SimpleIRI("urn:", "datatype:item");
        var datatypeB = new SimpleIRI("urn:datatype:", "item");
        var first = new TermKey(new SimpleLiteral("01", datatypeA));
        var second = new TermKey(new SimpleLiteral("01", datatypeB));
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        int expected = 31 * (31 * "01".hashCode() + datatypeA.stringValue().hashCode())
                + java.util.Optional.empty().hashCode();
        assertEquals(expected, first.hashCode());
        assertNotEquals(first, new TermKey(new SimpleLiteral("1", datatypeA)));
    }
}
