package fr.inria.corese.core.next.query.impl.sparql.execution;


import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

/** End-to-end propertypath pipeline tests. */
class NextSparqlPipelinePropertyPathExecutorTest extends PipelineTestSupport {

    @Test
    void propertyPathsPreserveSequenceAndAlternativeMultiplicities() {
        insert(iri(ALICE), iri(KNOWS), iri("http://example.org/carol"));
        insert(iri(BOB), iri(KNOWS), iri("http://example.org/dave"));
        insert(iri("http://example.org/carol"), iri(KNOWS), iri("http://example.org/dave"));
        assertEquals(2, pathCount("ex:alice ex:knows/ex:knows ?end"));
        assertEquals(4, pathCount("ex:alice (ex:knows|ex:knows) ?end"));
        assertEquals(3, pathCount("ex:alice ex:knows+ ?end"));
        assertEquals(2, pathCount("?start ^(ex:knows/ex:knows) ex:alice"));
    }

    @Test
    void propertyPathCyclesTerminateAndDeduplicateReachableNodes() {
        insert(iri(BOB), iri(KNOWS), iri(ALICE));
        insert(iri(ALICE), iri(KNOWS), iri(ALICE));
        assertEquals(2, pathCount("ex:alice ex:knows* ?end"));
        assertEquals(2, pathCount("ex:alice ex:knows+ ?end"));
        assertEquals(2, pathCount("ex:alice ex:knows? ?end"));
        assertEquals(2, pathCount("?node ex:knows+ ?node"));
        assertEquals(1, pathCount("ex:alice ex:knows+ ex:alice"));
    }

    @Test
    void zeroLengthPathsIncludeAbsentConstantsAndLiteralGraphNodes() {
        insert(iri(BOB), iri(NAME), valueFactory.createLiteral("Bob"));
        assertEquals(1, pathCount("ex:absent ex:knows* ?end"));
        assertEquals(1, pathCount("?start ex:knows* ex:absent"));
        assertEquals(1, pathCount("ex:absent ex:knows? ex:absent"));
        assertEquals(0, pathCount("ex:absent ex:knows+ ?end"));
        assertEquals(3, pathCount("?node ex:missing* ?node"));
        assertEquals(1, pathCount("\"Bob\" ex:missing* ?end"));
    }

    @Test
    void negatedPropertySetsRespectEachDirection() {
        insert(iri(ALICE), iri(NAME), valueFactory.createLiteral("Alice"));
        insert(iri(BOB), iri(NAME), iri(ALICE));
        assertEquals(1, pathCount("ex:alice !ex:knows ?end"));
        assertEquals(0, pathCount("ex:alice !(ex:knows|ex:name) ?end"));
        assertEquals(1, pathCount("ex:alice !(^ex:knows) ?end"));
        assertEquals(2, pathCount("ex:alice !(ex:knows|^ex:knows) ?end"));
        assertEquals(1, pathCount("ex:alice !(ex:name|^ex:name) ?end"));
    }

    @Test
    void propertyPathsComposeWithValuesOptionalAndExists() {
        assertEquals(2, pathCount("VALUES ?start { ex:alice ex:alice } ?start ex:knows+ ?end"));
        assertEquals(1, pathCount("ex:alice ex:knows+ ?end OPTIONAL { ?end ex:missing+ ?other }"));
        assertEquals(1, pathCount("ex:alice ex:knows ?end FILTER EXISTS { ?end ^ex:knows+ ex:alice }"));
    }

    @Test
    void propertyPathsStayWithinTheActiveGraph() {
        Resource graph = iri("http://example.org/graph");
        storage.mutations().add(valueFactory.createStatement(iri(BOB), iri(KNOWS), iri(ALICE), graph));
        assertEquals(1, pathCount("GRAPH ex:graph { ?start ex:knows+ ?end }"));
        assertEquals(0, pathCount("GRAPH ex:graph { ex:alice ex:knows+ ?end }"));
        assertEquals(1, pathCount("GRAPH ?g { ex:bob ex:knows+ ex:alice }"));
    }

    @Test
    void boundVariablesDoNotInventZeroLengthGraphNodes() {
        assertEquals(0, pathCount("VALUES ?node { ex:absent } ?node ex:knows? ?node"));
        assertEquals(0, pathCount("VALUES ?start { ex:absent } ?start ex:knows* ?end"));
        assertEquals(0, pathCount("VALUES ?end { ex:absent } ?start ex:knows* ?end"));
        assertEquals(1, pathCount("VALUES ?start { ex:absent } ?start ex:knows* ex:absent"));
    }

    @Test
    void propertyPathTraversalHandlesLongCyclesWithoutRecursiveJavaCalls() {
        int length = 2000;
        for (int index = 0; index < length; index++) {
            insert(iri("http://example.org/node" + index), iri(KNOWS),
                    iri("http://example.org/node" + ((index + 1) % length)));
        }
        assertEquals(length, pathCount("ex:node0 ex:knows+ ?end"));
    }

    private long pathCount(String pattern) {
        try (TupleQueryResult result = executor.evaluateTuple(
                "PREFIX ex: <http://example.org/> SELECT * WHERE { " + pattern + " }")) {
            long count = 0;
            while (result.hasNext()) {
                result.next();
                count++;
            }
            return count;
        }
    }

}
