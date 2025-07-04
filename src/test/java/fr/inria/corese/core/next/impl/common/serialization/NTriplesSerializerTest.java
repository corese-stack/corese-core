package fr.inria.corese.core.next.impl.common.serialization;

import fr.inria.corese.core.next.api.*;
import fr.inria.corese.core.next.impl.common.serialization.config.NTriplesConfig;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class NTriplesSerializerTest {

    private Model model;
    private NTriplesConfig config;
    private NTriplesSerializer nTriplesSerializer;

    private Resource mockExPerson;
    private IRI mockExName;
    private IRI mockExKnows;

    private final String lexJohn = "John Doe";
    private final String hello = "Hello";

    private Literal mockLiteralJohn;
    private Literal mockLiteralHelloEn;
    private Resource mockBNode1;
    private Resource mockBNode2;

    @BeforeEach
    void setUp() {
        model = mock(Model.class);
        config = NTriplesConfig.defaultConfig();
        nTriplesSerializer = new NTriplesSerializer(model, config);

        mockExPerson = createIRI("http://example.org/Person");
        mockExName = createIRI("http://example.org/name");
        mockExKnows = createIRI("http://example.org/knows");

        mockLiteralJohn = createLiteral(lexJohn, null, null);
        mockLiteralHelloEn = createLiteral(hello, null, "en");

        mockBNode1 = createBlankNode("b1");
        mockBNode2 = createBlankNode("b2");
    }

    @Test
    @DisplayName("Le constructeur devrait lever NullPointerException pour un modèle nul")
    void constructorShouldThrowForNullModel() {
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(null), "Model cannot be null");
    }

    @Test
    @DisplayName("Le constructeur devrait lever NullPointerException pour une configuration nulle")
    void constructorShouldThrowForNullConfig() {
        assertThrows(NullPointerException.class, () -> new NTriplesSerializer(model, null), "Configuration cannot be null");
    }

    @Test
    @DisplayName("L'écriture devrait sérialiser une déclaration simple correctement (graphe par défaut)")
    void writeShouldSerializeSimpleStatement() throws SerializationException {
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);

        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("L'écriture devrait sérialiser une déclaration avec contexte mais l'ignorer (N-Triples)")
    void writeShouldSerializeStatementWithContext() throws SerializationException {
        IRI mockContext = createIRI("http://example.org/ctx");
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn,
                mockContext
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);

        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("L'écriture devrait gérer les nœuds vierges avec le préfixe N-Triples par défaut (_:)")
    void writeShouldHandleBlankNodes() throws SerializationException {
        Statement stmt = createStatement(
                mockBNode1,
                mockExKnows,
                mockBNode2
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);

        String expected = String.format("_:%s <%s> _:%s",
                mockBNode1.stringValue(),
                mockExKnows.stringValue(),
                mockBNode2.stringValue()) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @Test
    @DisplayName("L'écriture devrait lever SerializationException en cas d'erreur E/S")
    void writeShouldThrowOnIOException() throws IOException {
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer faultyWriter = mock(Writer.class);

        doThrow(new IOException("Simulated IO error during write")).when(faultyWriter).write(anyString());
        doThrow(new IOException("Simulated IO error (char array)")).when(faultyWriter).write(any(char[].class), anyInt(), anyInt());
        doThrow(new IOException("Simulated IO error (close)")).when(faultyWriter).close();

        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(faultyWriter));

        assertEquals("N-Triples serialization failed [Format: N-Triples]", thrown.getMessage());
    }


    @Test
    @DisplayName("L'écriture devrait lever SerializationException pour une valeur de sujet nulle dans le mode strict")
    void writeShouldThrowOnNullSubjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(null);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));

        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
    }

    @Test
    @DisplayName("L'écriture devrait lever SerializationException pour une valeur de prédicat nulle dans le mode strict")
    void writeShouldThrowOnNullPredicateValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(null);
        when(stmt.getObject()).thenReturn(mockLiteralJohn);
        when(stmt.getContext()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));
        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
    }

    @Test
    @DisplayName("L'écriture devrait lever SerializationException pour une valeur d'objet nulle dans le mode strict")
    void writeShouldThrowOnNullObjectValue() {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(mockExPerson);
        when(stmt.getPredicate()).thenReturn(mockExName);
        when(stmt.getObject()).thenReturn(null);
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        SerializationException thrown = assertThrows(SerializationException.class, () -> nTriplesSerializer.write(writer));
        assertEquals("Invalid N-Triples data: Value cannot be null in N-Triples format when strictMode is enabled. [Format: N-Triples]", thrown.getMessage());
    }


    @Test
    @DisplayName("Devrait gérer correctement le contexte nul (graphe par défaut)")
    void writeShouldHandleNullContext() throws SerializationException {
        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                mockLiteralJohn,
                null
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);


        String expected = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                escapeNTriplesString(lexJohn)) + " .\n";

        assertEquals(expected, writer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "simple literal",
            "literal with \"quotes\"",
            "literal with \\ backslash",
            "literal with \n newline",
            "literal with \t tab",
            "literal with \r carriage return",
            "literal with \u0001 (SOH)",
            "literal with \u007F (DEL)"
    })
    @DisplayName("L'écriture devrait gérer diverses valeurs littérales avec un échappement approprié (y compris Unicode)")
    void writeShouldHandleVariousLiterals(String literalValue) throws SerializationException {
        Literal literalMock = createLiteral(literalValue, null, null);

        Statement stmt = createStatement(
                mockExPerson,
                mockExName,
                literalMock
        );
        when(model.iterator()).thenReturn(new MockStatementIterator(stmt));

        StringWriter writer = new StringWriter();
        nTriplesSerializer.write(writer);


        String expectedEscapedLiteral = escapeNTriplesString(literalValue);
        String expectedOutput = String.format("<%s> <%s> \"%s\"",
                mockExPerson.stringValue(),
                mockExName.stringValue(),
                expectedEscapedLiteral) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


    @Test
    @DisplayName("Devrait gérer les littéraux avec des balises de langue")
    void shouldHandleLiteralsWithLanguageTags() throws SerializationException {
        Statement stmt = createStatement(mockExPerson, createIRI("http://example.org/greeting"), mockLiteralHelloEn);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        Writer writer = new StringWriter();

        NTriplesSerializer serializer = new NTriplesSerializer(currentTestModel, NTriplesConfig.defaultConfig());
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"@%s",
                mockExPerson.stringValue(),
                createIRI("http://example.org/greeting").stringValue(),
                escapeNTriplesString(hello),
                mockLiteralHelloEn.getLanguage().get()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


    @Test
    @DisplayName("Devrait gérer les littéraux avec des types de données personnalisés")
    void shouldHandleLiteralsWithCustomDatatypes() throws SerializationException {
        IRI customDatatype = createIRI("http://example.org/myDataType");
        Literal customLiteral = createLiteral("123", customDatatype, null);

        Statement stmt = createStatement(mockExPerson, createIRI("http://example.org/value"), customLiteral);

        Model currentTestModel = mock(Model.class);
        when(currentTestModel.iterator()).thenReturn(new MockStatementIterator(stmt));

        NTriplesSerializer serializer = new NTriplesSerializer(currentTestModel, NTriplesConfig.defaultConfig());

        StringWriter writer = new StringWriter();
        serializer.write(writer);

        String expectedOutput = String.format("<%s> <%s> \"%s\"^^<%s>",
                mockExPerson.stringValue(),
                createIRI("http://example.org/value").stringValue(),
                escapeNTriplesString("123"),
                customDatatype.stringValue()) + " .\n";

        assertEquals(expectedOutput, writer.toString());
    }


    /**
     * Crée un objet Literal mocké.
     * Important : le `lexicalForm` est la *valeur de chaîne brute* du littéral,
     * sans les guillemets spécifiques à N-Triples, les balises de langue ou les URI de type de données.
     * La classe `nTriplesSerializer` est chargée d'ajouter ceux-ci.
     *
     * @param lexicalForm La valeur de chaîne brute du littéral (par exemple, "hello", "123").
     * @param dataTypeIRI L'IRI du type de données du littéral (par exemple, XSD.INTEGER.getIRI()), ou null pour simple/avec balise de langue.
     * @param langTag     La balise de langue (par exemple, "en"), ou null si non balisé par la langue.
     * @return Une instance de Literal mockée.
     */
    private Literal createLiteral(String lexicalForm, IRI dataTypeIRI, String langTag) {
        Literal literal = mock(Literal.class);
        when(literal.isLiteral()).thenReturn(true);
        when(literal.isResource()).thenReturn(false);
        when(literal.stringValue()).thenReturn(lexicalForm);

        if (langTag != null && !langTag.isEmpty()) {
            when(literal.getLanguage()).thenReturn(Optional.of(langTag));
            when(literal.getDatatype()).thenReturn(RDF.langString.getIRI());
        } else {
            when(literal.getLanguage()).thenReturn(Optional.empty());
            when(literal.getDatatype()).thenReturn(dataTypeIRI);
        }
        return literal;
    }

    /**
     * Échappe une chaîne selon les règles d'échappement des littéraux N-Triples.
     * Cette aide est utilisée dans les tests pour construire les chaînes de sortie *attendues*.
     * Elle imite le comportement de la méthode interne `escapeLiteral` de `nTriplesSerializer`,
     * en considérant que `ntriplesConfig().escapeUnicode()` est `true`.
     *
     * @param s La chaîne à échapper.
     * @return La chaîne échappée.
     */
    private String escapeNTriplesString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c <= 0x1F || c == 0x7F) {
                        sb.append(String.format("\\u%04X", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static class MockStatementIterator implements Iterator<Statement> {
        private final Statement[] statements;
        private int index = 0;

        MockStatementIterator(Statement... statements) {
            this.statements = statements;
        }

        @Override
        public boolean hasNext() {
            return index < statements.length;
        }

        @Override
        public Statement next() {
            return statements[index++];
        }
    }

    private Statement createStatement(Resource subject, IRI predicate, Value object) {
        return createStatement(subject, predicate, object, null);
    }

    private Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(subject);
        when(stmt.getPredicate()).thenReturn(predicate);
        when(stmt.getObject()).thenReturn(object);
        when(stmt.getContext()).thenReturn(context);
        return stmt;
    }

    private Resource createBlankNode(String id) {
        Resource blankNode = mock(Resource.class);
        when(blankNode.isResource()).thenReturn(true);
        when(blankNode.isBNode()).thenReturn(true);
        when(blankNode.isIRI()).thenReturn(false);
        when(blankNode.stringValue()).thenReturn(id);
        return blankNode;
    }

    private IRI createIRI(String uri) {
        IRI iri = mock(IRI.class);
        when(iri.isResource()).thenReturn(true);
        when(iri.isIRI()).thenReturn(true);
        when(iri.isBNode()).thenReturn(false);
        when(iri.stringValue()).thenReturn(uri);
        return iri;
    }
}
