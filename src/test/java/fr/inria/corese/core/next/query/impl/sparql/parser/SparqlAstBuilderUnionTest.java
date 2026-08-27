package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the UNION support in {@link SparqlAstBuilder}.
 */
class SparqlAstBuilderUnionTest {

    private SparqlQueryAstBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SparqlQueryAstBuilder(new SparqlParserOptions.Builder().build());
        // All tests operate on a SELECT query
        builder.enterSelectQuery();
    }

    /**
     * Simulates: outer group open → UNION open → branch { } → branch { } → UNION close → outer group close.
     * Each branch is empty (no BGP inside).
     */
    private GroupGraphPatternAst buildTwoEmptyBranchUnion() {
        builder.enterGroup();
        builder.enterUnion();

        builder.enterGroup();
        builder.exitGroup();
        builder.collectUnionBranch();

        builder.enterGroup();
        builder.exitGroup();
        builder.collectUnionBranch();

        builder.exitUnion();
        builder.exitGroup();
        builder.exitSelectQuery();
        return Objects.requireNonNull((SelectQueryAst)builder.getResult()).whereClause();
    }

    /**
     * Builds a BGP with a single triple ?s a <iri> inside the current group.
     */
    private void addTriple(String object) {
        builder.enterBgp();
        builder.addTriple(builder.variable("s"), builder.iri("a"), builder.iri(object));
        builder.exitBgp();
    }

    @Nested
    @DisplayName("Two-branch UNION")
    class TwoBranchUnion {

        @Test
        @DisplayName("WHERE clause contains exactly one UnionAst")
        void whereClauseContainsUnionAst() {
            GroupGraphPatternAst where = buildTwoEmptyBranchUnion();

            assertEquals(1, where.patterns().size());
            assertInstanceOf(UnionAst.class, where.patterns().getFirst());
        }

        @Test
        @DisplayName("UnionAst left and right branches are GroupGraphPatternAst")
        void unionBranchesAreGroups() {
            GroupGraphPatternAst where = buildTwoEmptyBranchUnion();
            UnionAst union = (UnionAst) where.patterns().getFirst();

            assertNotNull(union.left());
            assertNotNull(union.right());
            assertInstanceOf(GroupGraphPatternAst.class, union.left());
            assertInstanceOf(GroupGraphPatternAst.class, union.right());
        }

        @Test
        @DisplayName("Both branches are empty when no triples are added")
        void emptyBranchesHaveNoPatterns() {
            GroupGraphPatternAst where = buildTwoEmptyBranchUnion();
            UnionAst union = (UnionAst) where.patterns().getFirst();

            assertTrue(union.left().patterns().isEmpty());
            assertTrue(union.right().patterns().isEmpty());
        }

        @Test
        @DisplayName("Each branch holds its own BGP with correct triples")
        void branchesContainCorrectBgps() {
            builder.enterGroup();
            builder.enterUnion();

            builder.enterGroup();
            addTriple("foaf:Person");
            builder.exitGroup();
            builder.collectUnionBranch();

            builder.enterGroup();
            addTriple("ex:Animal");
            builder.exitGroup();
            builder.collectUnionBranch();

            builder.exitUnion();
            builder.exitGroup();

            builder.exitSelectQuery();
            SelectQueryAst result = (SelectQueryAst) builder.getResult();
            assertNotNull(result);
            UnionAst union = (UnionAst) result.whereClause().patterns().getFirst();

            // Left branch: ?s a foaf:Person
            BgpAst leftBgp = (BgpAst) union.left().patterns().getFirst();
            assertEquals(1, leftBgp.triples().size());
            assertEquals(new IriAst("foaf:Person"), leftBgp.triples().getFirst().object());

            // Right branch: ?s a ex:Animal
            BgpAst rightBgp = (BgpAst) union.right().patterns().getFirst();
            assertEquals(1, rightBgp.triples().size());
            assertEquals(new IriAst("ex:Animal"), rightBgp.triples().getFirst().object());
        }
    }

    // Three-branch UNION (left-associative folding)

    @Nested
    @DisplayName("Three-branch UNION (left-associative)")
    class ThreeBranchUnion {

        /**
         * Builds: { A } UNION { B } UNION { C }
         * Expected AST: UnionAst( GroupGraphPatternAst[ UnionAst(A,B) ], C )
         */
        private UnionAst buildThreeBranchUnion() {
            builder.enterGroup();
            builder.enterUnion();

            builder.enterGroup();
            addTriple("ex:A");
            builder.exitGroup();
            builder.collectUnionBranch();

            builder.enterGroup();
            addTriple("ex:B");
            builder.exitGroup();
            builder.collectUnionBranch();

            builder.enterGroup();
            addTriple("ex:C");
            builder.exitGroup();
            builder.collectUnionBranch();

            builder.exitUnion();
            builder.exitGroup();

            builder.exitSelectQuery();
            SelectQueryAst result = (SelectQueryAst) builder.getResult();
            assertNotNull(result);
            return (UnionAst) result.whereClause().patterns().getFirst();
        }

        @Test
        @DisplayName("Root node is a UnionAst")
        void rootIsUnionAst() {
            assertInstanceOf(UnionAst.class, buildThreeBranchUnion());
        }

        @Test
        @DisplayName("Left child wraps the first nested UnionAst (left-associative)")
        void leftChildWrapsInnerUnion() {
            UnionAst root = buildThreeBranchUnion();
            // root.left() = GroupGraphPatternAst[ UnionAst(A, B) ]
            GroupGraphPatternAst leftWrapper = root.left();
            assertEquals(1, leftWrapper.patterns().size());
            assertInstanceOf(UnionAst.class, leftWrapper.patterns().getFirst());
        }

        @Test
        @DisplayName("Inner UnionAst contains branches A and B")
        void innerUnionContainsAandB() {
            UnionAst root = buildThreeBranchUnion();
            UnionAst inner = (UnionAst) root.left().patterns().getFirst();

            BgpAst leftBgp = (BgpAst) inner.left().patterns().getFirst();
            BgpAst rightBgp = (BgpAst) inner.right().patterns().getFirst();

            assertEquals(new IriAst("ex:A"), leftBgp.triples().getFirst().object());
            assertEquals(new IriAst("ex:B"), rightBgp.triples().getFirst().object());
        }

        @Test
        @DisplayName("Right child of root is branch C")
        void rightChildIsBranchC() {
            UnionAst root = buildThreeBranchUnion();
            BgpAst rightBgp = (BgpAst) root.right().patterns().getFirst();
            assertEquals(new IriAst("ex:C"), rightBgp.triples().getFirst().object());
        }
    }

    // Single-branch (no UNION keyword)

    @Nested
    @DisplayName("Single-branch GroupOrUnion (no UNION keyword)")
    class SingleBranch {

        @Test
        @DisplayName("Single branch produces a GroupGraphPatternAst, not a UnionAst")
        void singleBranchIsNotUnion() {
            builder.enterGroup();
            builder.enterUnion();

            builder.enterGroup();
            addTriple("foaf:Person");
            builder.exitGroup();
            builder.collectUnionBranch();

            builder.exitUnion();
            builder.exitGroup();

            builder.exitSelectQuery();
            SelectQueryAst result = (SelectQueryAst) builder.getResult();
            assertNotNull(result);
            PatternAst pattern = result.whereClause().patterns().getFirst();

            assertInstanceOf(GroupGraphPatternAst.class, pattern,
                    "Single branch should not produce a UnionAst");
        }

        @Test
        @DisplayName("Single branch preserves its BGP")
        void singleBranchPreservesBgp() {
            builder.enterGroup();
            builder.enterUnion();

            builder.enterGroup();
            addTriple("foaf:Person");
            builder.exitGroup();
            builder.collectUnionBranch();

            builder.exitUnion();
            builder.exitGroup();

            builder.exitSelectQuery();
            SelectQueryAst result = (SelectQueryAst) builder.getResult();
            assertNotNull(result);
            GroupGraphPatternAst group = (GroupGraphPatternAst) result.whereClause().patterns().getFirst();
            BgpAst bgp = (BgpAst) group.patterns().getFirst();

            assertEquals(new IriAst("foaf:Person"), bgp.triples().getFirst().object());
        }
    }

    // Guard cases

    @Nested
    @DisplayName("Guard cases")
    class GuardCases {

        @Test
        @DisplayName("collectUnionBranch() is a no-op when unionStack is empty")
        void collectOnEmptyUnionStackIsNoOp() {
            builder.enterGroup();
            // Do NOT call enterUnion() — unionStack is empty
            assertDoesNotThrow(() -> builder.collectUnionBranch());
            builder.exitGroup();
        }

        @Test
        @DisplayName("collectUnionBranch() is a no-op when groupStack is empty")
        void collectOnEmptyGroupStackIsNoOp() {
            builder.enterUnion();
            // Do NOT call enterGroup() — groupStack is empty
            assertDoesNotThrow(() -> builder.collectUnionBranch());
            builder.exitUnion();
        }

        @Test
        @DisplayName("exitUnion() with empty branch list is a no-op")
        void exitUnionWithNoBranchesIsNoOp() {
            builder.enterGroup();
            builder.enterUnion();
            // collectUnionBranch() never called
            assertDoesNotThrow(() -> builder.exitUnion());
            builder.exitGroup();
        }
    }

    // UNION nested inside an outer group

    @Nested
    @DisplayName("UNION nested inside an outer group with other patterns")
    class NestedUnion {

        @Test
        @DisplayName("Outer group contains a BGP followed by a UnionAst")
        void outerGroupContainsBgpThenUnion() {
            builder.enterGroup();
            addTriple("ex:Root");

            builder.enterUnion();
            builder.enterGroup();
            addTriple("ex:A");
            builder.exitGroup();
            builder.collectUnionBranch();

            builder.enterGroup();
            addTriple("ex:B");
            builder.exitGroup();
            builder.collectUnionBranch();
            builder.exitUnion();

            builder.exitGroup();

            builder.exitSelectQuery();
            SelectQueryAst result = (SelectQueryAst) builder.getResult();
            assertNotNull(result);
            List<PatternAst> patterns = result.whereClause().patterns();

            assertEquals(2, patterns.size());
            assertInstanceOf(BgpAst.class, patterns.get(0));
            assertInstanceOf(UnionAst.class, patterns.get(1));
        }
    }
}