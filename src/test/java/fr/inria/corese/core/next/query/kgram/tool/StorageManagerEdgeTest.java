package fr.inria.corese.core.next.query.kgram.tool;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Literal;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageManagerEdgeTest {

    private final ValueFactory valueFactory = new CoreseAdaptedValueFactory();

    @Test
    void exposesStorageStatementAsKgramEdge() {
        IRI subject = valueFactory.createIRI("http://example.org/alice");
        IRI predicate = valueFactory.createIRI("http://example.org/name");
        Literal object = valueFactory.createLiteral("Alice", "en");
        IRI graph = valueFactory.createIRI("http://example.org/graph");
        Statement statement = valueFactory.createStatement(subject, predicate, object, graph);

        StorageManagerEdge edge = new StorageManagerEdge(statement);

        assertSame(statement, edge.getSourceStatement());
        assertEquals(subject.stringValue(), edge.getNode(0).getLabel());
        assertEquals(predicate.stringValue(), edge.getProperty().getLabel());
        assertEquals(predicate.stringValue(), edge.getEdgeLabel());
        assertEquals("Alice", edge.getNode(1).getLabel());
        assertEquals("en", edge.getNode(1).getDatatypeValue().getLang());
        assertEquals(graph.stringValue(), edge.getGraph().getLabel());
        assertTrue(edge.contains(edge.getNode(0)));
    }

    @Test
    void convertsTypedLiteralThroughSharedKgramValueHelper() {
        IRI integerDatatype = valueFactory.createIRI("http://www.w3.org/2001/XMLSchema#integer");
        Node node = StorageManagerKgramValues.node(valueFactory.createLiteral("42", integerDatatype));

        assertEquals("42", node.getLabel());
        assertEquals(integerDatatype.stringValue(), node.getDatatypeValue().getDatatypeURI());
    }
}
