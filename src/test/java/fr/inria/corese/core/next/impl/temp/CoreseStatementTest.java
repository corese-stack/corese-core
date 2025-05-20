package fr.inria.corese.core.next.impl.temp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.inria.corese.core.NodeImpl;
import fr.inria.corese.core.edge.EdgeImpl;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.impl.temp.literal.CoreseInteger;
import fr.inria.corese.core.sparql.datatype.DatatypeMap;

public class CoreseStatementTest {
    private Resource subject;
    private IRI predicate;
    private Value object;
    private Resource context;
    private Edge edge;
    private Node graphNode;
    private Node predicateNode;
    private Node subjectNode;
    private Node objectNode;

    @BeforeEach
    public void setUp() {
        subject = new CoreseIRI("http://corese.com/subject");
        predicate = new CoreseIRI("http://corese.com/predicate");
        object = new CoreseInteger(1);
        context = new CoreseIRI("http://corese.com/context");

        graphNode = NodeImpl.create(DatatypeMap.createResource("http://corese.com/context"));
        predicateNode = NodeImpl.create(DatatypeMap.createResource("http://corese.com/predicate"));
        subjectNode = NodeImpl.create(DatatypeMap.createResource("http://corese.com/subject"));
        objectNode = NodeImpl.create(DatatypeMap.create(1));
        edge = EdgeImpl.create(graphNode, subjectNode, predicateNode, objectNode);
    }

    @Test
    public void testCoreseStatementWithContext() {
        CoreseStatement statement = new CoreseStatement(subject, predicate, object, context);

        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(object, statement.getObject());
        assertEquals(context, statement.getContext());
    }

    @Test
    public void testCoreseStatementWithoutContext() {
        CoreseStatement statement = new CoreseStatement(subject, predicate, object, null);

        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(object, statement.getObject());
        assertNull(statement.getContext());
    }

    @Test
    public void testCoreseStatementFromEdgeWithNullEdge() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CoreseStatement(null);
        });
    }

    @Test
    public void testCoreseStatementFromEdge() {
        CoreseStatement statement = new CoreseStatement(edge);

        assertEquals(subject, statement.getSubject());
        assertEquals(predicate, statement.getPredicate());
        assertEquals(object.stringValue(), statement.getObject().stringValue());
        assertEquals(context, statement.getContext());
    }

    @Test
    public void testGetCoreseEdge() {
        CoreseStatement statement = new CoreseStatement(subject, predicate, object, context);
        assertNotNull(statement.getCoreseEdge());
    }
}
