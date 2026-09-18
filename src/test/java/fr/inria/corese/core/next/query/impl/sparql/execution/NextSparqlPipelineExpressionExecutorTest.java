package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end expression pipeline tests. */
class NextSparqlPipelineExpressionExecutorTest extends PipelineTestSupport {

    @Test
    @DisplayName("FILTER evaluates native numeric expressions")
    void filterRunsThroughNativeExpressionEvaluator() {
        String age = "http://example.org/age";
        insert(iri(ALICE), iri(age), valueFactory.createLiteral(42));
        insert(iri(BOB), iri(age), valueFactory.createLiteral(18));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person WHERE {
                  ?person <http://example.org/age> ?age .
                  FILTER(?age >= 21)
                }
                ORDER BY ?person
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("person").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("BIND evaluates native arithmetic and exposes the result")
    void bindRunsThroughNativeExpressionEvaluator() {
        String age = "http://example.org/age";
        insert(iri(ALICE), iri(age), valueFactory.createLiteral(41));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?nextAge WHERE {
                  <http://example.org/alice> <http://example.org/age> ?age .
                  BIND(?age + 1 AS ?nextAge)
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(42, ((fr.inria.corese.core.next.data.api.term.Literal)
                result.next().getValue("nextAge")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("FILTER numeric comparison does not reuse RDF term tie-breakers")
    void filterUsesValueComparisonInsteadOfTotalTermOrder() {
        String rank = "http://example.org/rank";
        insert(iri(ALICE), iri(rank), valueFactory.createLiteral(1));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?rank WHERE {
                  <http://example.org/alice> <http://example.org/rank> ?rank .
                  FILTER(?rank < 1.0)
                }
                """);

        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("STRLEN counts Unicode code points through the native evaluator")
    void stringLengthUsesUnicodeCodePoints() {
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("A🙂"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?length WHERE {
                  <http://example.org/alice> <http://example.org/name> ?name .
                  BIND(STRLEN(?name) AS ?length)
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(2, ((fr.inria.corese.core.next.data.api.term.Literal)
                result.next().getValue("length")).intValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("SUBSTR uses Unicode code-point positions and preserves the language tag")
    void substringUsesUnicodeCodePointPositions() {
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("A🙂B", "fr"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?part WHERE {
                  <http://example.org/alice> <http://example.org/name> ?name .
                  BIND(SUBSTR(?name, 2, 1) AS ?part)
                }
                """);

        Literal part = (Literal) result.next().getValue("part");
        assertEquals("🙂", part.getLabel());
        assertEquals("fr", part.getLanguage().orElseThrow());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("NOW returns one stable value throughout a query evaluation")
    void nowIsStableWithinOneQuery() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?first ?second WHERE {
                  ?s ?p ?o .
                  BIND(NOW() AS ?first)
                  BIND(NOW() AS ?second)
                }
                """);

        var binding = result.next();
        assertTrue(binding.getValue("first").sameTerm(binding.getValue("second")));
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Logical operators apply the SPARQL error truth table")
    void logicalOperatorsApplySparqlErrorTruthTable() {
        assertFalse(executor.evaluateTuple("""
                SELECT ?s WHERE { ?s ?p ?o . FILTER((1 / 0) && false) }
                """).hasNext());
        assertTrue(executor.evaluateTuple("""
                SELECT ?s WHERE { ?s ?p ?o . FILTER((1 / 0) || true) }
                """).hasNext());
    }

    @Test
    @DisplayName("BIND expression errors keep the input solution with an unbound target")
    void bindExpressionErrorLeavesTargetUnbound() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person ?value WHERE {
                  VALUES ?person { <http://example.org/alice> }
                  BIND(1 / 0 AS ?value)
                }
                """);

        assertTrue(result.hasNext());
        var binding = result.next();
        assertEquals(ALICE, binding.getValue("person").stringValue());
        assertFalse(binding.hasBinding("value"));
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("Arithmetic preserves SPARQL floating-point type promotion")
    void arithmeticPreservesFloatingPointPromotion() {
        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?sum WHERE { ?s ?p ?o . BIND(1e0 + 1 AS ?sum) }
                """);

        Literal sum = (Literal) result.next().getValue("sum");
        assertEquals(XSDDatatype.DOUBLE, sum.getCoreDatatype());
        assertEquals(2.0d, sum.doubleValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("FILTER REGEX evaluates without the historical expression interpreter")
    void regexFilterRunsThroughNativeExpressionEvaluator() {
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("Alice"));
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person WHERE {
                  ?person <http://example.org/name> ?name .
                  FILTER(REGEX(?name, "^ali", "i"))
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("person").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("FILTER EXISTS evaluates as a correlated native graph pattern")
    void correlatedExistsFilterRunsEndToEnd() {
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person WHERE {
                  ?person <http://example.org/knows> ?friend .
                  FILTER EXISTS { ?friend <http://example.org/name> ?name }
                }
                """);

        assertTrue(result.hasNext());
        assertEquals(ALICE, result.next().getValue("person").stringValue());
        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("FILTER NOT EXISTS rejects solutions with a correlated match")
    void correlatedNotExistsFilterRunsEndToEnd() {
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));

        TupleQueryResult result = executor.evaluateTuple("""
                SELECT ?person WHERE {
                  ?person <http://example.org/knows> ?friend .
                  FILTER NOT EXISTS { ?friend <http://example.org/name> ?name }
                }
                """);

        assertFalse(result.hasNext());
    }

    @Test
    @DisplayName("temporalProximity01: NOT EXISTS excludes only earlier examinations")
    void temporalProximity01() {
        String ex = "http://www.w3.org/2009/sparql/docs/tests/data-sparql11/negation#";
        IRI type = iri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        IRI date = iri("http://purl.org/dc/elements/1.1/date");
        for (String exam : List.of("examination1", "examination2")) {
            insert(iri(ex + exam), type, iri(ex + "PhysicalExamination"));
            insert(iri(ex + exam), iri(ex + "precedes"), iri(ex + "operation1"));
        }
        insert(iri(ex + "examination1"), date,
                valueFactory.createLiteral("2010-01-10", XSDDatatype.DATE.getIRI()));
        insert(iri(ex + "examination2"), date,
                valueFactory.createLiteral("2010-01-02", XSDDatatype.DATE.getIRI()));
        insert(iri(ex + "examination1"), iri(ex + "follows"), iri(ex + "examination2"));
        insert(iri(ex + "examination2"), iri(ex + "precedes"), iri(ex + "examination1"));
        insert(iri(ex + "operation1"), type, iri(ex + "SurgicalProcedure"));
        insert(iri(ex + "operation1"), date,
                valueFactory.createLiteral("2010-01-15", XSDDatatype.DATE.getIRI()));
        insert(iri(ex + "operation1"), iri(ex + "follows"), iri(ex + "examination1"));
        insert(iri(ex + "operation1"), iri(ex + "follows"), iri(ex + "examination2"));

        try (var result = executor.evaluateTuple("""
                PREFIX ex: <http://www.w3.org/2009/sparql/docs/tests/data-sparql11/negation#>
                PREFIX dc: <http://purl.org/dc/elements/1.1/>
                SELECT ?exam ?date {
                  ?exam a ex:PhysicalExamination; dc:date ?date; ex:precedes ex:operation1 .
                  ?op a ex:SurgicalProcedure; dc:date ?opDT .
                  FILTER NOT EXISTS {
                    ?otherExam a ex:PhysicalExamination;
                               ex:follows ?exam;
                               ex:precedes ex:operation1
                  }
                }
                """)) {
            var rows = result.stream().toList();
            assertEquals(1, rows.size());
            assertEquals(ex + "examination1", rows.getFirst().getValue("exam").stringValue());
            assertEquals(valueFactory.createLiteral("2010-01-10", XSDDatatype.DATE.getIRI()),
                    rows.getFirst().getValue("date"));
        }
    }

    @Test
    @DisplayName("EXISTS inside OPTIONAL filters within optional branch rather than being postponed")
    void existsInsideOptionalFiltersLocally() {
        IRI a = iri("http://example.org/a");
        IRI b = iri("http://example.org/b");
        IRI e = iri("http://example.org/e");
        IRI d = iri("http://example.org/d");
        IRI p = iri("http://example.org/p");
        IRI r = iri("http://example.org/r");
        insert(a, p, b);
        insert(a, p, e);
        insert(b, r, d);
        try (var result = executor.evaluateTuple("""
                PREFIX : <http://example.org/>
                SELECT ?s ?o {
                  VALUES ?s { :a }
                  OPTIONAL {
                    ?s :p ?o
                    FILTER EXISTS { ?o :r ?z }
                  }
                }
                """)) {
            var rows = result.stream().toList();
            assertEquals(1, rows.size());
            assertEquals("http://example.org/b", rows.getFirst().getValue("o").stringValue());
        }
    }

    @Test
    @DisplayName("SELECT with initial binding filters results to bound variable value")
    void selectWithInitialBindingFiltersResults() {
        // bob also knows carol — without binding both alice→bob and bob→carol would match ?s
        insert(iri(BOB), iri(KNOWS), iri("http://example.org/carol"));

        BindingSet bindings = singleBinding("s", iri(ALICE));
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?friend WHERE { ?s <" + KNOWS + "> ?friend }",
                bindings, null, 0L);

        assertTrue(result.hasNext());
        assertEquals(BOB, result.next().getValue("friend").stringValue());
        assertFalse(result.hasNext(), "Only alice's friends should be returned");
    }

    @Test
    @DisplayName("SELECT with literal initial binding filters results by literal value")
    void selectWithLiteralInitialBindingFiltersResults() {
        insert(iri(BOB),   iri(NAME), valueFactory.createLiteral("Bob"));
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("Alice"));

        BindingSet bindings = singleBinding("name", valueFactory.createLiteral("Bob"));
        TupleQueryResult result = executor.evaluateTuple(
                "SELECT ?s WHERE { ?s <" + NAME + "> ?name }",
                bindings, null, 0L);

        assertTrue(result.hasNext());
        assertEquals(BOB, result.next().getValue("s").stringValue());
        assertFalse(result.hasNext(), "Only the triple with literal 'Bob' should match");
    }

    // -------------------------------------------------------------------------
    // Timeout
    // -------------------------------------------------------------------------

    @Test
    void nestedGroupFilterDoesNotSeeOuterBind() {
        try (var result = executor.evaluateTuple("SELECT ?x { BIND(1 AS ?x) { FILTER(?x = 1) } }")) {
            assertFalse(result.hasNext());
        }
    }

}
