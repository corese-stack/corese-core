package fr.inria.corese.core.next.api.model.impl;

import fr.inria.corese.core.next.api.model.*;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InMemoryModelTest {

    Model model;
    private static Set<Statement> statements;

    protected IRI iri(String iri) {
        return new BasicIRI(iri);
    }

    public InMemoryModelTest() {
        model = new InMemoryModel();
    }

    static void init() {
        Logger.getLogger(InMemoryModelTest.class.getName()).info("Creating a list of statements");
        statements = new HashSet<>();
    }

    void setUp() {
        System.out.println("startup");
    }

    void teardown() {
        System.out.println("teardown");
    }

    @Test
    public void addAll() {
        model.addAll(statements);
        assertEquals(10, model.size());
    }

    @Test
    void contains() {
    }

    @Test
    void add(Statement st) {
        model.add(st);
        assertEquals(1, model.size());
    }

}