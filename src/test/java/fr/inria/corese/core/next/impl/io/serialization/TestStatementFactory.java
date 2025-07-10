package fr.inria.corese.core.next.impl.io.serialization;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Literal;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A factory class to create mocked RDF components (Statements, IRIs, Literals, Blank Nodes)
 * for use in unit tests. This centralizes the creation logic and reduces duplication
 * across various serializer test classes.
 */
public class TestStatementFactory {

    /**
     * Creates a mocked Statement object with a subject, predicate, and object.
     * The context (named graph) is set to null.
     *
     * @param subject   The mocked Resource representing the subject.
     * @param predicate The mocked IRI representing the predicate.
     * @param object    The mocked Value representing the object (IRI, Literal, or Blank Node).
     * @return A mocked Statement instance.
     */
    public Statement createStatement(Resource subject, IRI predicate, Value object) {
        return createStatement(subject, predicate, object, null);
    }

    /**
     * Creates a mocked Statement object with a subject, predicate, object, and an optional context.
     *
     * @param subject   The mocked Resource representing the subject.
     * @param predicate The mocked IRI representing the predicate.
     * @param object    The mocked Value representing the object (IRI, Literal, or Blank Node).
     * @param context   The mocked Resource representing the context (named graph), or null if none.
     * @return A mocked Statement instance.
     */
    public Statement createStatement(Resource subject, IRI predicate, Value object, Resource context) {
        Statement stmt = mock(Statement.class);
        when(stmt.getSubject()).thenReturn(subject);
        when(stmt.getPredicate()).thenReturn(predicate);
        when(stmt.getObject()).thenReturn(object);
        when(stmt.getContext()).thenReturn(context);
        return stmt;
    }

    /**
     * Creates a mocked Blank Node (Resource) object with a given ID.
     *
     * @param id The string identifier for the blank node (e.g., "b1").
     * @return A mocked Resource instance representing a blank node.
     */
    public Resource createBlankNode(String id) {
        Resource blankNode = mock(Resource.class);
        when(blankNode.isResource()).thenReturn(true);
        when(blankNode.isBNode()).thenReturn(true);
        when(blankNode.isIRI()).thenReturn(false);
        when(blankNode.stringValue()).thenReturn(id);
        return blankNode;
    }

    /**
     * Creates a mocked IRI object with a given URI string.
     *
     * @param uri The string URI for the IRI (e.g., "http://example.org/Person").
     * @return A mocked IRI instance.
     */
    public IRI createIRI(String uri) {
        IRI iri = mock(IRI.class);
        when(iri.isResource()).thenReturn(true);
        when(iri.isIRI()).thenReturn(true);
        when(iri.isBNode()).thenReturn(false);
        when(iri.stringValue()).thenReturn(uri);
        return iri;
    }

    /**
     * Creates a mocked Literal object.
     *
     * @param lexicalForm The raw string value of the literal (e.g., "hello", "123").
     * @param dataTypeIRI The IRI of the literal's datatype (e.g., XSD.INTEGER.getIRI()), or null for plain/lang-tagged.
     * @param langTag     The language tag (e.g., "en"), or null if not language-tagged.
     * @return A mocked Literal instance.
     */
    public Literal createLiteral(String lexicalForm, IRI dataTypeIRI, String langTag) {
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
}
