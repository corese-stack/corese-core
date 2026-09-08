package fr.inria.corese.core.next.data.api.term;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;

class SimpleLiteralTest {

    @Test
    void createsStringLiteral() {
        SimpleLiteral literal = new SimpleLiteral("hello");

        assertEquals("hello", literal.getLabel());
        assertEquals("hello", literal.stringValue());
        assertEquals(XSDDatatype.STRING.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.STRING, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
        assertTrue(literal.isLiteral());
    }

    @Test
    void createsLanguageTaggedLiteral() {
        SimpleLiteral literal = new SimpleLiteral("hello", "EN-GB");

        assertEquals("hello", literal.getLabel());
        assertEquals(Optional.of("en-gb"), literal.getLanguage());
        assertEquals(RDFDatatype.LANGSTRING.getIRI(), literal.getDatatype());
        assertEquals(RDFDatatype.LANGSTRING, literal.getCoreDatatype());
    }

    @Test
    void languageTagEqualityIgnoresCase() {
        SimpleLiteral upper = new SimpleLiteral("hello", "EN");
        SimpleLiteral lower = new SimpleLiteral("hello", "en");

        assertEquals(upper, lower);
        assertEquals(lower, upper);
        assertEquals(upper.hashCode(), lower.hashCode());
        assertNotEquals(upper, new SimpleLiteral("hello", "fr"));
    }

    @Test
    void createsCustomTypedLiteral() {
        IRI datatype = new SimpleIRI("http://example.org/custom");
        SimpleLiteral literal = new SimpleLiteral("value", datatype);

        assertEquals("value", literal.getLabel());
        assertEquals(datatype, literal.getDatatype());
        assertEquals(CoreDatatype.NONE, literal.getCoreDatatype());
        assertTrue(literal.getLanguage().isEmpty());
    }

    @Test
    void defaultsNullDatatypeToString() {
        assertEquals(new SimpleLiteral("value"), new SimpleLiteral("value", (IRI) null));
        SimpleLiteral literal = new SimpleLiteral("value", null, null);

        assertEquals(XSDDatatype.STRING.getIRI(), literal.getDatatype());
        assertEquals(XSDDatatype.STRING, literal.getCoreDatatype());
        assertEquals(XSDDatatype.STRING, new SimpleLiteral("value", (IRI) null).getCoreDatatype());
    }

    @Test
    void acceptsExplicitCoreDatatypeAndResolvesNullCoreDatatype() {
        SimpleLiteral explicit = new SimpleLiteral("true", XSDDatatype.BOOLEAN.getIRI(), XSDDatatype.BOOLEAN);
        SimpleLiteral resolved = new SimpleLiteral("true", XSDDatatype.BOOLEAN.getIRI(), null);

        assertEquals(XSDDatatype.BOOLEAN, explicit.getCoreDatatype());
        assertEquals(XSDDatatype.BOOLEAN, resolved.getCoreDatatype());
        assertEquals(explicit, resolved);
        assertTrue(explicit.getLanguage().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"true, true", "1, true", "false, false", "0, false"})
    void parsesBooleanLiterals(String label, boolean expected) {
        SimpleLiteral literal = new SimpleLiteral(label, XSDDatatype.BOOLEAN.getIRI());

        assertEquals(XSDDatatype.BOOLEAN, literal.getCoreDatatype());
        assertEquals(expected, literal.booleanValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "TRUE", "False", "2", " true ", "invalid"})
    void rejectsInvalidBooleanLiterals(String label) {
        SimpleLiteral literal = new SimpleLiteral(label, XSDDatatype.BOOLEAN.getIRI());

        assertThrows(IncorrectOperationException.class, literal::booleanValue);
    }

    @Test
    void rejectsBooleanConversionForOtherDatatypes() {
        SimpleLiteral literal = new SimpleLiteral("true");

        assertThrows(IncorrectOperationException.class, literal::booleanValue);
    }

    @Test
    void equalityAndHashCodeFollowTermIdentity() {
        SimpleLiteral first = new SimpleLiteral("hello");
        SimpleLiteral second = new SimpleLiteral("hello", XSDDatatype.STRING.getIRI());
        SimpleLiteral third = new SimpleLiteral("hello", XSDDatatype.STRING.getIRI(), XSDDatatype.STRING);

        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(second.hashCode(), third.hashCode());
        assertEquals(first.hashCode(), first.hashCode());
        assertNotEquals(first, new SimpleLiteral("other"));
        assertNotEquals(first, new SimpleLiteral("hello", new SimpleIRI("http://example.org/custom")));
        assertNotEquals(first, new SimpleLiteral("hello", "en"));
        assertNotEquals(null, first);
        assertNotEquals("hello", first);
        assertNotEquals(new SimpleLiteral("true", XSDDatatype.BOOLEAN.getIRI()),
                new SimpleLiteral("1", XSDDatatype.BOOLEAN.getIRI()));
    }

    @Test
    void rejectsNullLabelInEveryConstructor() {
        assertThrows(NullPointerException.class, () -> new SimpleLiteral(null));
        assertThrows(NullPointerException.class, () -> new SimpleLiteral(null, "en"));
        IRI stringDatatype = XSDDatatype.STRING.getIRI();
        assertThrows(NullPointerException.class, () -> new SimpleLiteral(null, stringDatatype));
        CoreDatatype stringCoreDatatype = XSDDatatype.STRING;
        assertThrows(NullPointerException.class,
                () -> new SimpleLiteral(null, stringDatatype, stringCoreDatatype));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void rejectsNullOrBlankLanguage(String language) {
        assertThrows(IllegalArgumentException.class, () -> new SimpleLiteral("hello", language));
    }

    @Test
    void rejectsCoreDatatypeMutation() {
        SimpleLiteral literal = new SimpleLiteral("hello");

        IncorrectOperationException exception = assertThrows(IncorrectOperationException.class,
                () -> literal.setCoreDatatype(XSDDatatype.BOOLEAN));

        assertEquals("SimpleLiteral is immutable", exception.getMessage());
        assertEquals(XSDDatatype.STRING, literal.getCoreDatatype());
    }
}
