package fr.inria.corese.core.next.api.model.impl;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Resource;
import fr.inria.corese.core.next.api.model.Statement;
import fr.inria.corese.core.next.impl.inmemory.BasicIRI;
import fr.inria.corese.core.next.api.model.base.GenericStatement;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GenericStatementTest {
    public Statement stmt;
    @Before
    public void setUp() throws Exception {
        Resource subj = new BasicIRI("http://example.org/subj#1");
        IRI pred = new BasicIRI("http://example.org/pred#1");
        Resource obj = new BasicIRI("http://example.org/obj#1");
        Resource ctx = new BasicIRI("http://example.org/ctx#1");
        stmt = new GenericStatement<>(subj, pred, obj, ctx);
    }

    @Test
    public void getSubject() {
        assertEquals("http://example.org/subj#1", stmt.getSubject().toString());
    }

    @Test
    public void getPredicate() {
        assertEquals("http://example.org/pred#1", stmt.getPredicate().toString());
    }

    @Test
    public void getObject() {
        assertEquals("http://example.org/obj#1", stmt.getObject().toString());
    }

    @Test
    public void getContext() {
        assertEquals("http://example.org/ctx#1", stmt.getContext().toString());
    }

}