package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PrefixDeclarationAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.BoundAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AstBackedExpr: AST wrapping, value resolution, and filter view")
class AstBackedExprTest {

    @Test
    void resolvesConstantUsingCompilerPrologue() {
        QueryPrologueAst prologue = new QueryPrologueAst(
                List.of(new PrefixDeclarationAst("ex:", new IriAst("http://example.org/"))),
                null);
        WhereCompiler compiler = new WhereCompiler().withPrologue(prologue);
        AstBackedExpr expression = new AstBackedExpr(new IriAst("ex:name"), compiler);

        assertEquals("http://example.org/name", expression.getValue().stringValue());
        assertTrue(expression.isConstant());
        assertFalse(expression.isVariable());
    }

    @Test
    void resolvesLiteralValue() {
        WhereCompiler compiler = new WhereCompiler();
        AstBackedExpr stringExpr = new AstBackedExpr(new LiteralAst("\"hello\"", null, null), compiler);
        assertEquals("hello", stringExpr.getValue().stringValue());

        AstBackedExpr intExpr = new AstBackedExpr(new LiteralAst("42", null, "http://www.w3.org/2001/XMLSchema#integer"), compiler);
        assertEquals("42", intExpr.getValue().stringValue());
    }

    @Test
    void variableExpressionMetadata() {
        WhereCompiler compiler = new WhereCompiler();
        AstBackedExpr varExpr = new AstBackedExpr(new VarAst("x"), compiler);

        assertEquals("x", varExpr.getLabel());
        assertTrue(varExpr.isVariable());
        assertFalse(varExpr.isConstant());
        assertTrue(varExpr.getExpList().isEmpty());
        assertNull(varExpr.getArg());
    }

    @Test
    void boundExpressionAndFilterView() {
        WhereCompiler compiler = new WhereCompiler();
        BoundAst boundAst = new BoundAst(List.of(new VarAst("y")));
        AstBackedExpr boundExpr = new AstBackedExpr(boundAst, compiler);

        assertTrue(boundExpr.isBound());
        assertNotNull(boundExpr.getFilter());
        assertTrue(boundExpr.getFilter().isBound());
        assertEquals(List.of("y"), boundExpr.getFilter().getVariables());
        assertEquals(boundAst, boundExpr.getFilter().getFilterExpression());
    }

    @Test
    void immutableExpressionsThrowOnModification() {
        WhereCompiler compiler = new WhereCompiler();
        AstBackedExpr expr = new AstBackedExpr(new VarAst("z"), compiler);

        assertThrows(UnsupportedOperationException.class, () -> expr.setExp(0, expr));
        assertThrows(UnsupportedOperationException.class, () -> expr.setArg(expr));
    }
}
