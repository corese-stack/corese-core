package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.api.term.Literal;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.storage.impl.memory.MemoryStorageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/** Scalar expressions exercised through parsing, evaluation and public result adaptation. */
class ScalarFunctionsTest {
    private static final String PREFIX = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    @ParameterizedTest
    @CsvSource({"double,0E1,0", "double,1E0,1", "float,0E1,0", "float,1E0,1"})
    void decimalCastDoesNotRewriteItsSourceTerm(String type, String lexical, int expected) {
        var executor = new NextSparqlPipelineExecutor(MemoryStorageManager.builder().build());
        try (var result = executor.evaluateTuple(PREFIX
                + "SELECT ?v (xsd:decimal(?v) AS ?decimal) WHERE { VALUES ?v { '"
                + lexical + "'^^xsd:" + type + " } }")) {
            var row = result.next();
            var source = (Literal) row.getValue("v");
            var converted = (Literal) row.getValue("decimal");
            assertEquals(lexical, source.getLabel());
            assertEquals("http://www.w3.org/2001/XMLSchema#" + type, source.getDatatype().stringValue());
            assertEquals(XSDDatatype.DECIMAL, converted.getCoreDatatype());
            assertEquals(0, converted.decimalValue().compareTo(java.math.BigDecimal.valueOf(expected)));
            assertFalse(result.hasNext());
        }
    }

    private BindingSet evaluate(String expressions) {
        var executor = new NextSparqlPipelineExecutor(MemoryStorageManager.builder().build());
        try (var result = executor.evaluateTuple(PREFIX + "SELECT " + expressions + " WHERE {}")) {
            assertTrue(result.hasNext());
            var row = result.next();
            assertFalse(result.hasNext());
            return row;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "STRLEN('👪') = 1", "SUBSTR('a👪b', 2, 1) = '👪'",
            "UCASE('abc') = 'ABC'", "LCASE('ABC') = 'abc'",
            "STRSTARTS('abc', 'a')", "STRENDS('abc', 'c')", "CONTAINS('abc', 'b')",
            "STRBEFORE('abc', 'b') = 'a'", "STRAFTER('abc', 'b') = 'c'",
            "LANG(STRBEFORE('abc'@fr, 'z')) = ''", "LANG(STRAFTER('abc'@fr, 'z')) = ''",
            "ENCODE_FOR_URI('* ~é') = '%2A%20~%C3%A9'", "CONCAT() = ''",
            "LANG(CONCAT('a'@fr, 'b'@fr)) = 'fr'", "LANGMATCHES('fr-FR', 'fr')",
            "REGEX('Abc', '^a', 'i')", "REPLACE('abc', 'b', 'Z') = 'aZc'",
            "ABS(-2) = 2", "ROUND(-2.5) = -2", "CEIL(-2.5) = -2", "FLOOR(-2.5) = -3",
            "RAND() >= 0 && RAND() < 1", "NOW() = NOW()",
            "YEAR(xsd:dateTime('2020-03-04T05:06:07Z')) = 2020",
            "MONTH(xsd:dateTime('2020-03-04T05:06:07Z')) = 3",
            "DAY(xsd:dateTime('2020-03-04T05:06:07Z')) = 4",
            "HOURS(xsd:dateTime('2020-03-04T05:06:07Z')) = 5",
            "MINUTES(xsd:dateTime('2020-03-04T05:06:07Z')) = 6",
            "SECONDS(xsd:dateTime('2020-03-04T05:06:07.125Z')) = 7.125",
            "TZ(xsd:dateTime('2020-03-04T05:06:07')) = ''",
            "STR(TIMEZONE(xsd:dateTime('2020-03-04T05:06:07-08:00'))) = '-PT8H'",
            "MD5('abc') = '900150983cd24fb0d6963f7d28e17f72'",
            "SHA1('abc') = 'a9993e364706816aba3e25717850c26c9cd0d89d'",
            "STRLEN(SHA256('abc')) = 64", "STRLEN(SHA384('abc')) = 96", "STRLEN(SHA512('abc')) = 128",
            "ISIRI(UUID())", "STRLEN(STRUUID()) = 36", "UUID() != UUID()", "STRUUID() != STRUUID()",
            "ISBLANK(BNODE())", "BNODE() != BNODE()", "BNODE('x') = BNODE('x')",
            "STRDT('1', xsd:integer) = 1", "LANG(STRLANG('abc', 'fr')) = 'fr'",
            "ISNUMERIC(1)", "!ISNUMERIC('one')", "!ISNUMERIC('bad'^^xsd:integer)",
            "IF(true, 1, 1/0) = 1", "IF(false, 1/0, 2) = 2", "COALESCE(1/0, ?missing, 3) = 3",
            "xsd:integer('-2') = -2", "xsd:integer(-2.9) = -2", "xsd:decimal(true) = 1",
            "xsd:double('1E2') = 100", "xsd:float(false) = 0", "xsd:boolean('false') = false",
            "xsd:string(true) = 'true'", "xsd:string(1.20) = '1.2'",
            "xsd:string(1.0E0) = '1'", "xsd:string(1.0E7) = '1.0E7'",
            "xsd:string(-0.0E0) = '-0'", "xsd:string(ROUND(-0.4E0)) = '-0'",
            "ROUND(4503599627370497.0E0) = 4503599627370497.0E0",
            "xsd:boolean(0.000000000000000000000000000000000000000000000000000000000001)",
            "!ISNUMERIC('128'^^xsd:byte)", "!ISNUMERIC('-1'^^xsd:unsignedInt)",
            "!ISNUMERIC('0x1.0p1'^^xsd:double)", "!ISNUMERIC('1E2'^^xsd:decimal)",
            "!('bad'^^xsd:integer)", "!('bad'^^xsd:boolean)",
            "SUBSTR('abc', '-INF'^^xsd:double, 'INF'^^xsd:double) = ''",
            "REGEX('#', '#', 'x')", "REGEX('ab', 'a b', 'x')",
            "xsd:integer(' 12 ') = 12", "ABS(' 12 '^^xsd:integer) = 12",
            "xsd:boolean(' true '^^xsd:boolean)", "xsd:string(' false '^^xsd:boolean) = 'false'",
            "xsd:integer(1E23) = 99999999999999991611392"
    })
    void evaluatesScalar(String expression) {
        var value = (Literal) evaluate("(" + expression + " AS ?actual)").getValue("actual");
        assertNotNull(value, expression);
        assertTrue(value.booleanValue(), expression);
    }

    @ParameterizedTest
    @ValueSource(strings = {"xsd:integer('1.5')", "xsd:decimal('1E2')", "xsd:boolean('yes')",
            "xsd:double('0x1.0p1')", "xsd:dateTime('bad')", "xsd:integer('NaN'^^xsd:double)",
            "STR(BNODE())", "STRDT('a'@fr, xsd:string)", "STRLANG('a'@fr, 'en')",
            "STRDT('a', 'not an IRI')", "YEAR('2020-01-01'^^xsd:date)",
            "TIMEZONE(xsd:dateTime('2020-01-01T00:00:00'))", "REGEX('abc', 'a', 'z')",
            "CONTAINS('abc', 'a'@fr)", "COALESCE()", "MD5('abc'@en)",
            "REGEX('abc', 'a'@en)", "REPLACE('abc', 'a*', 'x')", "BNODE(1)",
            "IRI(1)", "IRI('a'@en)", "ABS('128'^^xsd:byte)"})
    void typeErrorsLeaveOnlyAliasUnbound(String expression) {
        var row = evaluate("(7 AS ?valid) (" + expression + " AS ?invalid)");
        assertEquals(7, ((Literal) row.getValue("valid")).intValue());
        assertFalse(row.hasBinding("invalid"), expression);
        assertEquals(9, ((Literal) evaluate("(COALESCE(" + expression + ", 9) AS ?v)").getValue("v")).intValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"STRLEN('abc')", "YEAR(NOW())", "MONTH(NOW())", "DAY(NOW())", "HOURS(NOW())", "MINUTES(NOW())"})
    void integerFunctionsReturnXsdInteger(String expression) {
        assertEquals(XSDDatatype.INTEGER, ((Literal) evaluate("(" + expression + " AS ?v)").getValue("v")).getCoreDatatype());
    }

    @Test
    void iriResolvesAgainstBaseWithoutExpandingPrefixes() {
        var executor = new NextSparqlPipelineExecutor(MemoryStorageManager.builder().build());
        try (var result = executor.evaluateTuple("BASE <http://example.org/> PREFIX ex: <http://other/> SELECT (IRI('relative') AS ?iri) (URI('ex:name') AS ?uri) WHERE {}")) {
            var row = result.next();
            assertEquals("http://example.org/relative", row.getValue("iri").stringValue());
            assertEquals("ex:name", row.getValue("uri").stringValue());
        }
    }

    @Test
    void projectionAliasesJoinOrderAndDoNotLeakAcrossRows() {
        var executor = new NextSparqlPipelineExecutor(MemoryStorageManager.builder().build());
        try (var result = executor.evaluateTuple("SELECT (?x + 1 AS ?a) (?a + 1 AS ?b) WHERE { VALUES ?x { 2 1 } } ORDER BY ?a")) {
            var rows = result.stream().toList();
            assertEquals(2, rows.size());
            assertEquals(2, ((Literal) rows.getFirst().getValue("a")).intValue());
            assertEquals(3, ((Literal) rows.getFirst().getValue("b")).intValue());
            assertEquals(4, ((Literal) rows.getLast().getValue("b")).intValue());
        }
    }

    @Test
    void labeledBlankNodesAreFreshForEachSolutionAndEvaluation() {
        var executor = new NextSparqlPipelineExecutor(MemoryStorageManager.builder().build());
        String query = "SELECT (BNODE('label') AS ?a) (BNODE('label') AS ?b) WHERE { VALUES ?x { 1 2 } }";
        try (var first = executor.evaluateTuple(query); var second = executor.evaluateTuple(query)) {
            var rows = first.stream().toList();
            assertEquals(rows.getFirst().getValue("a"), rows.getFirst().getValue("b"));
            assertNotEquals(rows.getFirst().getValue("a"), rows.getLast().getValue("a"));
            assertNotEquals(rows.getFirst().getValue("a"), second.next().getValue("a"));
        }
    }
}
