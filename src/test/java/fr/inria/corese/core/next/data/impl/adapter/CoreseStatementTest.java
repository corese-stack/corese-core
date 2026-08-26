package fr.inria.corese.core.next.data.impl.adapter;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.impl.adapter.node.CoreseIRI;
import fr.inria.corese.core.next.data.impl.adapter.literal.CoreseInteger;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoreseStatementTest {
    private Resource subject;
    private IRI predicate;
    private Value object;
    private Resource context;
    private Edge edge;

    @BeforeEach
    void setUp() {
        subject = new CoreseIRI("http://corese.com/subject");
        predicate = new CoreseIRI("http://corese.com/predicate");
        object = new CoreseInteger(1);
        context = new CoreseIRI("http://corese.com/context");

        IDatatype graphVal = DatatypeMap.createResource("http://corese.com/context");
        IDatatype predVal = DatatypeMap.createResource("http://corese.com/predicate");
        IDatatype subjVal = DatatypeMap.createResource("http://corese.com/subject");
        IDatatype objVal = DatatypeMap.create(1);

        Node graphNode = mock(Node.class);
        when(graphNode.getDatatypeValue()).thenReturn(graphVal);
        when(graphNode.getValue()).thenReturn(graphVal);

        Node predicateNode = mock(Node.class);
        when(predicateNode.getDatatypeValue()).thenReturn(predVal);
        when(predicateNode.getValue()).thenReturn(predVal);

        Node subjectNode = mock(Node.class);
        when(subjectNode.getDatatypeValue()).thenReturn(subjVal);
        when(subjectNode.getValue()).thenReturn(subjVal);

        Node objectNode = mock(Node.class);
        when(objectNode.getDatatypeValue()).thenReturn(objVal);
        when(objectNode.getValue()).thenReturn(objVal);

        edge = mock(Edge.class);
        when(edge.getGraph()).thenReturn(graphNode);
        when(edge.getGraphNode()).thenReturn(graphNode);
        when(edge.getSubjectNode()).thenReturn(subjectNode);
        when(edge.getPropertyNode()).thenReturn(predicateNode);
        when(edge.getObjectNode()).thenReturn(objectNode);
    }

    @Test
    void testCoreseStatementWithContext() {
        CoreseStatement statement = new CoreseStatement(subject, predicate, object, context);

        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(object, statement.getObject());
        assertEquals(context, statement.getContext());
    }

    @Test
    void testCoreseStatementWithoutContext() {
        CoreseStatement statement = new CoreseStatement(subject, predicate, object, null);

        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(object, statement.getObject());
        assertNull(statement.getContext());
    }

    @Test
    void testCoreseStatementFromEdgeWithNullEdge() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CoreseStatement(null);
        });
    }

    @Test
    void testCoreseStatementFromEdge() {
        CoreseStatement statement = new CoreseStatement(edge);

        assertNotNull(statement.getSubject());
        assertEquals(subject.stringValue(), statement.getSubject().stringValue());
        assertNotNull(statement.getPredicate());
        assertEquals(predicate.stringValue(), statement.getPredicate().stringValue());
        assertNotNull(statement.getObject());
        assertEquals(object.stringValue(), statement.getObject().stringValue());
        assertNotNull(statement.getContext());
        assertEquals(context.stringValue(), statement.getContext().stringValue());
    }
}
