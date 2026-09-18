package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end aggregate pipeline tests. */
class NextSparqlPipelineAggregateExecutorTest extends PipelineTestSupport {

    @Test
    @DisplayName("GROUP BY and COUNT aggregate count solutions per group")
    void groupByAndCountAggregate() {
        String charlie = "http://example.org/charlie";
        insert(iri(ALICE), iri(KNOWS), iri(charlie));
        insert(iri(BOB), iri(KNOWS), iri(charlie));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (COUNT(?o) AS ?cnt) WHERE {
                    ?s <http://example.org/knows> ?o .
                }
                GROUP BY ?s
                ORDER BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row1 = result.next();
            assertEquals(ALICE, row1.getValue("s").stringValue());
            assertEquals("2", row1.getValue("cnt").stringValue());

            assertTrue(result.hasNext());
            var row2 = result.next();
            assertEquals(BOB, row2.getValue("s").stringValue());
            assertEquals("1", row2.getValue("cnt").stringValue());

            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("COUNT(*) and COUNT(DISTINCT) evaluate properly over whole solution")
    void countStarAndDistinct() {
        String tag = "http://example.org/tag";
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("java"));
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("sparql"));
        insert(iri(BOB), iri(tag), valueFactory.createLiteral("java"));

        try (var result = executor.evaluateTuple("""
                SELECT (COUNT(*) AS ?total) (COUNT(DISTINCT ?val) AS ?uniqueTags) WHERE {
                    ?s <http://example.org/tag> ?val .
                }
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("3", row.getValue("total").stringValue());
            assertEquals("2", row.getValue("uniqueTags").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("SUM and AVG aggregates promote types and calculate correct totals")
    void sumAndAvgAggregates() {
        String score = "http://example.org/score";
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(10));
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(20));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (SUM(?score) AS ?total) (AVG(?score) AS ?average) WHERE {
                    ?s <http://example.org/score> ?score .
                }
                GROUP BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            assertEquals("30", row.getValue("total").stringValue());
            assertEquals("15", row.getValue("average").stringValue());
            assertEquals(XSDDatatype.DECIMAL.getIRI(), ((Literal) row.getValue("average")).getDatatype());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("MIN, MAX, and SAMPLE aggregates select extreme and sample values")
    void minMaxSampleAggregates() {
        String score = "http://example.org/score";
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(5));
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(25));
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(15));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (MIN(?score) AS ?minVal) (MAX(?score) AS ?maxVal) (SAMPLE(?score) AS ?sampleVal) WHERE {
                    ?s <http://example.org/score> ?score .
                }
                GROUP BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            assertEquals("5", row.getValue("minVal").stringValue());
            assertEquals("25", row.getValue("maxVal").stringValue());
            assertTrue(row.hasBinding("sampleVal"));
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP_CONCAT aggregates strings with custom separator")
    void groupConcatAggregate() {
        String tag = "http://example.org/tag";
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("A"));
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("B"));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (GROUP_CONCAT(?tag; separator=";") AS ?tags) WHERE {
                    ?s <http://example.org/tag> ?tag .
                }
                GROUP BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            String tags = row.getValue("tags").stringValue();
            assertTrue("A;B".equals(tags) || "B;A".equals(tags));
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP BY with expression alias binds alias for projection")
    void groupByExpressionAlias() {
        String val = "http://example.org/val";
        insert(iri(ALICE), iri(val), valueFactory.createLiteral(10));
        insert(iri(BOB), iri(val), valueFactory.createLiteral(10));

        try (var result = executor.evaluateTuple("""
                SELECT ?next (COUNT(?s) AS ?cnt) WHERE {
                    ?s <http://example.org/val> ?v .
                }
                GROUP BY (?v + 1 AS ?next)
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("11", row.getValue("next").stringValue());
            assertEquals("2", row.getValue("cnt").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("HAVING filters groups based on aggregate condition")
    void havingWithAggregateCondition() {
        String tag = "http://example.org/tag";
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("t1"));
        insert(iri(ALICE), iri(tag), valueFactory.createLiteral("t2"));
        insert(iri(BOB), iri(tag), valueFactory.createLiteral("t1"));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (COUNT(?tag) AS ?cnt) WHERE {
                    ?s <http://example.org/tag> ?tag .
                }
                GROUP BY ?s
                HAVING (COUNT(?tag) > 1)
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            assertEquals("2", row.getValue("cnt").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("Implicit aggregate on empty dataset returns exactly 1 row")
    void implicitAggregateOnEmptyDatasetReturnsOneRow() {
        try (var result = executor.evaluateTuple("""
                SELECT (COUNT(?o) AS ?count) (SUM(?score) AS ?sum) (AVG(?score) AS ?avg) (GROUP_CONCAT(?o) AS ?gc) WHERE {
                    ?s <http://example.org/nonexistent> ?o .
                    OPTIONAL { ?s <http://example.org/score> ?score }
                }
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("0", row.getValue("count").stringValue());
            assertEquals("0", row.getValue("sum").stringValue());
            assertFalse(row.hasBinding("avg"));
            assertEquals("", row.getValue("gc").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP BY on empty dataset returns 0 rows")
    void groupByOnEmptyDatasetReturnsZeroRows() {
        try (var result = executor.evaluateTuple("""
                SELECT ?s (COUNT(?o) AS ?count) WHERE {
                    ?s <http://example.org/nonexistent> ?o .
                }
                GROUP BY ?s
                """)) {
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("Projection expression with aggregate arithmetic")
    void projectionExpressionWithAggregateArithmetic() {
        try (var result = executor.evaluateTuple("""
                SELECT (COUNT(?s) AS ?c) ((COUNT(?s) + 5) AS ?cPlus) WHERE {
                    ?s <http://example.org/knows> ?o .
                }
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("1", row.getValue("c").stringValue());
            assertEquals("6", row.getValue("cPlus").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("ORDER BY with aggregate expression sorts groups")
    void orderByAggregateExpression() {
        String score = "http://example.org/score";
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(10));
        insert(iri(BOB), iri(score), valueFactory.createLiteral(20));
        insert(iri(BOB), iri(score), valueFactory.createLiteral(30));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (SUM(?val) AS ?total) WHERE {
                    ?s <http://example.org/score> ?val .
                }
                GROUP BY ?s
                ORDER BY DESC(?total)
                """)) {
            assertTrue(result.hasNext());
            var row1 = result.next();
            assertEquals(BOB, row1.getValue("s").stringValue());
            assertEquals("50", row1.getValue("total").stringValue());

            assertTrue(result.hasNext());
            var row2 = result.next();
            assertEquals(ALICE, row2.getValue("s").stringValue());
            assertEquals("10", row2.getValue("total").stringValue());

            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP_CONCAT concatenates blank nodes preserving their string representations")
    void groupConcatBlankNodes() {
        String edge = "http://example.org/item";
        var b1 = valueFactory.createBNode("b1");
        var b2 = valueFactory.createBNode("b2");
        insert(iri(ALICE), iri(edge), b1);
        insert(iri(ALICE), iri(edge), b2);

        try (var result = executor.evaluateTuple("""
                SELECT ?s (GROUP_CONCAT(?o ; SEPARATOR=",") AS ?concat) WHERE {
                    ?s <http://example.org/item> ?o .
                }
                GROUP BY ?s
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(ALICE, row.getValue("s").stringValue());
            String concat = row.getValue("concat").stringValue();
            assertTrue(concat.contains(b1.stringValue()) && concat.contains(b2.stringValue()),
                    "GROUP_CONCAT must include blank node string representations");
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("GROUP BY with multiple identical expression aliases binds each alias")
    void groupByMultipleIdenticalExpressionAliases() {
        String num = "http://example.org/num";
        insert(iri(ALICE), iri(num), valueFactory.createLiteral(5));

        try (var result = executor.evaluateTuple("""
                SELECT ?y ?z (COUNT(*) AS ?cnt) WHERE {
                    ?s <http://example.org/num> ?val .
                }
                GROUP BY (?val + 1 AS ?y) (?val + 1 AS ?z)
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals("6", row.getValue("y").stringValue());
            assertEquals("6", row.getValue("z").stringValue());
            assertEquals("1", row.getValue("cnt").stringValue());
            assertFalse(result.hasNext());
        }
    }

    @Test
    @DisplayName("Combined query with GROUP BY, Aggregates, HAVING and ORDER BY")
    void combinedGroupByAggregatesHavingOrderBy() {
        String score = "http://example.org/score";
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(10));
        insert(iri(ALICE), iri(score), valueFactory.createLiteral(5));
        insert(iri(BOB), iri(score), valueFactory.createLiteral(20));
        insert(iri(BOB), iri(score), valueFactory.createLiteral(30));

        try (var result = executor.evaluateTuple("""
                SELECT ?s (SUM(?val) AS ?total) (COUNT(?val) AS ?count) WHERE {
                    ?s <http://example.org/score> ?val .
                }
                GROUP BY ?s
                HAVING (SUM(?val) > 20)
                ORDER BY DESC(?total)
                """)) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertEquals(BOB, row.getValue("s").stringValue());
            assertEquals("50", row.getValue("total").stringValue());
            assertEquals("2", row.getValue("count").stringValue());
            assertFalse(result.hasNext());
        }
    }

}
