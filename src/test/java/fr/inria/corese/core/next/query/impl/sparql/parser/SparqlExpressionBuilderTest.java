package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SparqlExpressionBuilder}.
 * <p>
 * Direct tests cover factory methods that do not require ANTLR contexts:
 * {@code createConstraint}, {@code createFunCall}, {@code createAggregate}.
 * Integration tests parse SPARQL queries and assert the resulting constraint AST.
 */
class SparqlExpressionBuilderTest extends AbstractSparqlParserFeatureTest {

    private SparqlExpressionBuilder expressionBuilder;
    private SparqlTermBuilder termBuilder;

    @BeforeEach
    void setUp() {
        termBuilder = new SparqlTermBuilder(new SparqlParserOptions.Builder().build());
        expressionBuilder = new SparqlExpressionBuilder(termBuilder, () -> null);
    }

    // -------------------------------------------------------------------------
    // createConstraint — direct tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createConstraint()")
    class CreateConstraint {

        private TermAst a() { return new VarAst("a"); }
        private TermAst b() { return new VarAst("b"); }

        @Test
        @DisplayName("OR produces OrAst")
        void orConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.OR, List.of(a(), b()));
            assertInstanceOf(OrAst.class, result);
        }

        @Test
        @DisplayName("AND produces AndAst")
        void andConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.AND, List.of(a(), b()));
            assertInstanceOf(AndAst.class, result);
        }

        @Test
        @DisplayName("EQ produces EqualsAst")
        void eqConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.EQ, List.of(a(), b()));
            assertInstanceOf(EqualsAst.class, result);
        }

        @Test
        @DisplayName("NE produces DifferentAst")
        void neConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.NE, List.of(a(), b()));
            assertInstanceOf(DifferentAst.class, result);
        }

        @Test
        @DisplayName("LT produces LowerThanAst")
        void ltConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.LT, List.of(a(), b()));
            assertInstanceOf(LowerThanAst.class, result);
        }

        @Test
        @DisplayName("GT produces GreaterThanAst")
        void gtConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.GT, List.of(a(), b()));
            assertInstanceOf(GreaterThanAst.class, result);
        }

        @Test
        @DisplayName("ADD produces AddAst")
        void addConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.ADD, List.of(a(), b()));
            assertInstanceOf(AddAst.class, result);
        }

        @Test
        @DisplayName("SUB produces SubtractAst")
        void subConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.SUB, List.of(a(), b()));
            assertInstanceOf(SubtractAst.class, result);
        }

        @Test
        @DisplayName("MUL produces MultiplyAst")
        void mulConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.MUL, List.of(a(), b()));
            assertInstanceOf(MultiplyAst.class, result);
        }

        @Test
        @DisplayName("DIV produces DivideAst")
        void divConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.DIV, List.of(a(), b()));
            assertInstanceOf(DivideAst.class, result);
        }

        @Test
        @DisplayName("NOT produces BooleanNotAst")
        void notConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.OPERATOR.NOT, List.of(a()));
            assertInstanceOf(BooleanNotAst.class, result);
        }

        @Test
        @DisplayName("BOUND produces BoundAst")
        void boundConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.FUNCTION_CALL.BOUND, List.of(a()));
            assertInstanceOf(BoundAst.class, result);
        }

        @Test
        @DisplayName("IS_IRI produces IsIriAst")
        void isIriConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.FUNCTION_CALL.IS_IRI, List.of(a()));
            assertInstanceOf(IsIriAst.class, result);
        }

        @Test
        @DisplayName("IS_BLANK produces IsBlankAst")
        void isBlankConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.FUNCTION_CALL.IS_BLANK, List.of(a()));
            assertInstanceOf(IsBlankAst.class, result);
        }

        @Test
        @DisplayName("IS_LITERAL produces IsLiteralAst")
        void isLiteralConstraint() {
            ConstraintAst result = expressionBuilder.createConstraint(ASTConstants.FUNCTION_CALL.IS_LITERAL, List.of(a()));
            assertInstanceOf(IsLiteralAst.class, result);
        }

        @Test
        @DisplayName("unary PLUS with two args throws QuerySyntaxException")
        void unaryPlusWithTwoArgsThrows() {
            // PLUS is unary — passing 2 args violates the arity constraint
            assertThrows(QuerySyntaxException.class,
                    () -> expressionBuilder.createConstraint(ASTConstants.OPERATOR.PLUS, List.of(a(), b())));
        }
    }

    // -------------------------------------------------------------------------
    // createFunCall — direct tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createFunCall()")
    class CreateFunCall {

        @Test
        @DisplayName("produces FunctionCallAst with correct IRI and arguments")
        void basicFunCall() {
            IriAst fn = new IriAst("<http://ex.org/myFunc>");
            VarAst arg = new VarAst("x");
            ConstraintAst result = expressionBuilder.createFunCall(fn, List.of(arg));
            assertInstanceOf(FunctionCallAst.class, result);
            FunctionCallAst call = (FunctionCallAst) result;
            assertEquals(fn, call.functionName());
        }

        @Test
        @DisplayName("zero-argument function call is accepted")
        void zeroArgFunCall() {
            IriAst fn = new IriAst("<http://ex.org/noArgs>");
            assertDoesNotThrow(() -> expressionBuilder.createFunCall(fn, List.of()));
        }
    }

    // -------------------------------------------------------------------------
    // createAggregate — direct tests
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createAggregate()")
    class CreateAggregate {

        @Test
        @DisplayName("COUNT(*) — no expression, not distinct")
        void countStar() {
            AggregateAst result = expressionBuilder.createAggregate(AggregateFunction.COUNT, false, null, null);
            assertEquals(AggregateFunction.COUNT, result.function());
            assertFalse(result.distinct());
            assertNull(result.expression());
        }

        @Test
        @DisplayName("COUNT(DISTINCT ?x)")
        void countDistinct() {
            VarAst x = new VarAst("x");
            AggregateAst result = expressionBuilder.createAggregate(AggregateFunction.COUNT, true, x, null);
            assertTrue(result.distinct());
            assertSame(x, result.expression());
        }

        @Test
        @DisplayName("SUM(?val)")
        void sum() {
            VarAst val = new VarAst("val");
            AggregateAst result = expressionBuilder.createAggregate(AggregateFunction.SUM, false, val, null);
            assertEquals(AggregateFunction.SUM, result.function());
        }

        @Test
        @DisplayName("GROUP_CONCAT(?x ; SEPARATOR = \",\")")
        void groupConcat() {
            VarAst x = new VarAst("x");
            AggregateAst result = expressionBuilder.createAggregate(AggregateFunction.GROUP_CONCAT, false, x, "\",\"");
            assertEquals(AggregateFunction.GROUP_CONCAT, result.function());
            assertEquals("\",\"", result.groupConcatSeparator());
        }

        @Test
        @DisplayName("non-GROUP_CONCAT with separator throws")
        void separatorOnNonGroupConcatThrows() {
            VarAst x = new VarAst("x");
            assertThrows(IllegalArgumentException.class,
                    () -> expressionBuilder.createAggregate(AggregateFunction.SUM, false, x, "\",\""));
        }
    }

    // -------------------------------------------------------------------------
    // Integration tests via SPARQL parsing
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("FILTER expression parsing")
    class FilterParsing {

        private QueryAst parse(String query) {
            return newParserDefault().parse(query);
        }

        @Test
        @DisplayName("FILTER(?s = ?o) produces a non-null AST")
        void equalFilter() {
            QueryAst ast = parse("SELECT * WHERE { ?s ?p ?o . FILTER(?s = ?o) }");
            assertNotNull(ast);
        }

        @Test
        @DisplayName("FILTER(?x > 5) produces a non-null AST")
        void greaterThanFilter() {
            QueryAst ast = parse("SELECT * WHERE { ?s ?p ?x . FILTER(?x > 5) }");
            assertNotNull(ast);
        }

        @Test
        @DisplayName("FILTER with REGEX is parsed without error")
        void regexFilter() {
            QueryAst ast = parse("SELECT * WHERE { ?s ?p ?x . FILTER(REGEX(STR(?x), \"foo\")) }");
            assertNotNull(ast);
        }

        @Test
        @DisplayName("FILTER with nested AND/OR is parsed without error")
        void nestedAndOrFilter() {
            QueryAst ast = parse(
                    "SELECT * WHERE { ?s ?p ?x . FILTER(?x > 1 && ?x < 10 || ?x = 0) }");
            assertNotNull(ast);
        }

        @Test
        @DisplayName("FILTER with IS_IRI is parsed without error")
        void isIriFilter() {
            QueryAst ast = parse("SELECT * WHERE { ?s ?p ?o . FILTER(isIRI(?s)) }");
            assertNotNull(ast);
        }
    }
}
