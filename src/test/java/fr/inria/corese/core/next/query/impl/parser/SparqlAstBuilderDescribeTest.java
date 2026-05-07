package fr.inria.corese.core.next.query.impl.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DESCRIBE query support in {@link SparqlAstBuilder}.
 */
class SparqlAstBuilderDescribeTest {

    private SparqlAstBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SparqlAstBuilder(new SparqlParserOptions.Builder().build());
        builder.enterDescribeQuery();
    }

    /**
     * Builds a DescribeQueryAst with an empty WHERE clause.
     */
    private DescribeQueryAst buildWithEmptyWhere() {
        builder.enterGroup();
        builder.exitGroup();
        return (DescribeQueryAst) builder.getResult();
    }

    /**
     * Builds a DescribeQueryAst with a single triple in the WHERE clause.
     */
    private DescribeQueryAst buildWithWhere() {
        builder.enterGroup();
        builder.enterBgp();
        builder.addTriple(builder.var("s"), builder.iri("a"), builder.iri("foaf:Person"));
        builder.exitBgp();
        builder.exitGroup();
        return (DescribeQueryAst) builder.getResult();
    }

    @Nested
    @DisplayName("DESCRIBE *")
    class DescribeAll {

        @Test
        @DisplayName("No resources added → isDescribeAll() is true")
        void noResourcesIsDescribeAll() {
            DescribeQueryAst result = buildWithEmptyWhere();
            assertTrue(result.isDescribeAll());
        }

        @Test
        @DisplayName("No resources added → described list is empty")
        void noResourcesDescribedIsEmpty() {
            DescribeQueryAst result = buildWithEmptyWhere();
            assertTrue(result.described().isEmpty());
        }

        @Test
        @DisplayName("DESCRIBE * without WHERE clause → empty WHERE clause")
        void describeAllHasEmptyWhereClause() {
            DescribeQueryAst result = buildWithEmptyWhere();
            assertTrue(result.whereClause().patterns().isEmpty());
        }
    }

    @Nested
    @DisplayName("DESCRIBE <iri>")
    class DescribeSingleIri {

        @Test
        @DisplayName("Single IRI resource is added to described list")
        void singleIriResource() {
            builder.addDescribeResource(builder.iri("<http://example.org/person>"));
            DescribeQueryAst result = buildWithEmptyWhere();

            assertFalse(result.isDescribeAll());
            assertEquals(1, result.described().size());
            assertInstanceOf(IriAst.class, result.described().getFirst());
        }

        @Test
        @DisplayName("IRI value is preserved exactly")
        void iriValuePreserved() {
            builder.addDescribeResource(builder.iri("<http://example.org/person>"));
            DescribeQueryAst result = buildWithEmptyWhere();

            IriAst iri = (IriAst) result.described().getFirst();
            assertEquals("<http://example.org/person>", iri.raw());
        }
    }

    @Nested
    @DisplayName("DESCRIBE ?s")
    class DescribeSingleVariable {

        @Test
        @DisplayName("Single variable resource is added to described list")
        void singleVariableResource() {
            builder.addDescribeResource(builder.var("?s"));
            DescribeQueryAst result = buildWithEmptyWhere();

            assertFalse(result.isDescribeAll());
            assertEquals(1, result.described().size());
            assertInstanceOf(VarAst.class, result.described().getFirst());
        }

        @Test
        @DisplayName("Variable name is stripped of ? prefix")
        void variableNameStripped() {
            builder.addDescribeResource(builder.var("?s"));
            DescribeQueryAst result = buildWithEmptyWhere();

            VarAst describedVar = (VarAst) result.described().getFirst();
            assertEquals("s", describedVar.name());
        }

        @Test
        @DisplayName("$ prefix is also stripped from variable name")
        void dollarPrefixStripped() {
            builder.addDescribeResource(builder.var("$x"));
            DescribeQueryAst result = buildWithEmptyWhere();

            VarAst describedVar = (VarAst) result.described().getFirst();
            assertEquals("x", describedVar.name());
        }
    }

    @Nested
    @DisplayName("DESCRIBE ?s <iri> (mixed resources)")
    class DescribeMixed {

        @Test
        @DisplayName("Multiple resources are preserved in order")
        void multipleResourcesPreservedInOrder() {
            builder.addDescribeResource(builder.var("?s"));
            builder.addDescribeResource(builder.iri("<http://example.org/alice>"));
            builder.addDescribeResource(builder.iri("<http://example.org/bob>"));
            DescribeQueryAst result = buildWithEmptyWhere();

            assertEquals(3, result.described().size());
            assertInstanceOf(VarAst.class, result.described().get(0));
            assertInstanceOf(IriAst.class, result.described().get(1));
            assertInstanceOf(IriAst.class, result.described().get(2));
        }

        @Test
        @DisplayName("isDescribeAll() is false when resources are present")
        void isDescribeAllFalseWhenResourcesPresent() {
            builder.addDescribeResource(builder.var("?s"));
            DescribeQueryAst result = buildWithEmptyWhere();
            assertFalse(result.isDescribeAll());
        }
    }

    @Nested
    @DisplayName("DESCRIBE ?s WHERE { ... }")
    class DescribeWithWhereClause {

        @Test
        @DisplayName("WHERE clause is present and contains expected BGP")
        void whereClauseContainsBgp() {
            builder.addDescribeResource(builder.var("?s"));
            DescribeQueryAst result = buildWithWhere();

            assertFalse(result.whereClause().patterns().isEmpty());
            assertInstanceOf(BgpAst.class, result.whereClause().patterns().getFirst());
        }

        @Test
        @DisplayName("BGP in WHERE clause contains the expected triple")
        void whereClauseBgpTriple() {
            builder.addDescribeResource(builder.var("?s"));
            DescribeQueryAst result = buildWithWhere();

            BgpAst bgp = (BgpAst) result.whereClause().patterns().getFirst();
            assertEquals(1, bgp.triples().size());
            assertEquals(new IriAst("foaf:Person"), bgp.triples().getFirst().object());
        }

        @Test
        @DisplayName("Resources and WHERE clause coexist correctly")
        void resourcesAndWhereClauseCoexist() {
            builder.addDescribeResource(builder.var("?s"));
            DescribeQueryAst result = buildWithWhere();

            assertEquals(1, result.described().size());
            assertFalse(result.whereClause().patterns().isEmpty());
        }
    }
    
    @Nested
    @DisplayName("DescribeQueryAst — null safety")
    class NullSafety {

        @Test
        @DisplayName("null described list defaults to empty list")
        void nullDescribedDefaultsToEmpty() {
            DescribeQueryAst ast = new DescribeQueryAst(null, null, null);
            assertNotNull(ast.described());
            assertTrue(ast.described().isEmpty());
        }

        @Test
        @DisplayName("null whereClause defaults to empty GroupGraphPatternAst")
        void nullWhereClauseDefaultsToEmptyGroup() {
            DescribeQueryAst ast = new DescribeQueryAst(null, null, null);
            assertNotNull(ast.whereClause());
            assertTrue(ast.whereClause().patterns().isEmpty());
        }

        @Test
        @DisplayName("null solutionModifier defaults to empty SolutionModifierAst")
        void nullSolutionModifierDefaultsToEmpty() {
            DescribeQueryAst ast = new DescribeQueryAst(null, null, null, null);
            assertNotNull(ast.solutionModifier());
            assertTrue(ast.solutionModifier().groupBy().isEmpty());
            assertTrue(ast.solutionModifier().orderBy().isEmpty());
            assertNull(ast.solutionModifier().limit());
            assertNull(ast.solutionModifier().offset());
        }

        @Test
        @DisplayName("addDescribeResource(null) throws IllegalArgumentException")
        void addNullResourceThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> builder.addDescribeResource(null));
        }
    }
}
