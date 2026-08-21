package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.AndAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.ExistsAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterThanAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.NotExistsAst;
import fr.inria.corese.core.next.query.impl.kgram.api.core.Expr;
import fr.inria.corese.core.next.query.impl.kgram.api.core.ExprType;
import fr.inria.corese.core.next.query.impl.kgram.core.Exp;
import fr.inria.corese.core.next.query.impl.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WhereCompilerExistsTest {

    private final WhereCompiler compiler = new WhereCompiler();

    private static GroupGraphPatternAst group(PatternAst... elements) {
        return new GroupGraphPatternAst(List.of(elements));
    }

    private static BgpAst bgp(String s, String p, String o) {
        return new BgpAst(List.of(
                new TriplePatternAst(new VarAst(s), new VarAst(p), new VarAst(o))));
    }

    /**
     * Returns the FILTER's inner expression (the AstBackedExpr) from a compiled body.
     */
    private Expr filterExpr(Exp body) {
        for (int i = 0; i < body.size(); i++) {
            if (body.get(i).isFilter()) {
                return body.get(i).getFilter().getExp();
            }
        }
        throw new AssertionError("no FILTER in body");
    }

    @Test
    @DisplayName("FILTER EXISTS { ?s ?p ?o } -> Expr with oper EXIST carrying the compiled pattern")
    void compilesExists() {
        FilterAst filter = new FilterAst(new ExistsAst(group(bgp("s", "p", "o"))));

        Exp body = compiler.compile(group(bgp("a", "b", "c"), filter));

        Expr exist = filterExpr(body);
        assertEquals(ExprType.EXIST, exist.oper(), "operator is EXIST");

        Object pattern = exist.getPattern();
        assertNotNull(pattern, "EXISTS pattern is attached");
        assertInstanceOf(Exp.class, pattern, "pattern is a runtime Exp");
        Exp patternExp = (Exp) pattern;
        assertTrue(patternExp.isAnd() || patternExp.isBGP(), "pattern is the compiled graph pattern");
    }

    @Test
    @DisplayName("FILTER NOT EXISTS { ?s ?p ?o } -> negation wrapping an EXIST expression")
    void compilesNotExists() {
        FilterAst filter = new FilterAst(new NotExistsAst(group(bgp("s", "p", "o"))));

        Exp body = compiler.compile(group(bgp("a", "b", "c"), filter));

        Expr notExpr = filterExpr(body);
        assertEquals(ExprType.NOT, notExpr.oper(), "top operator is boolean NOT");
        assertEquals(1, notExpr.getExpList().size(), "NOT wraps a single sub-expression");

        Expr exist = notExpr.getExpList().getFirst();
        assertEquals(ExprType.EXIST, exist.oper(), "the wrapped expression is EXIST");
        assertInstanceOf(Exp.class, exist.getPattern(), "EXIST carries the compiled pattern");
    }

    @Test
    @DisplayName("EXISTS nested in a compound filter: FILTER(EXISTS { ... } && ?o > 5)")
    void compilesExistsNestedInCompoundFilter() {
        ExistsAst exists = new ExistsAst(group(bgp("s", "p", "o")));
        // EXISTS { ?s ?p ?o } && (?o > 5)
        AndAst and = new AndAst(List.of(
                exists,
                new GreaterThanAst(List.of(new VarAst("o"), new LiteralAst("5", null, null)))));
        FilterAst filter = new FilterAst(and);

        Exp body = compiler.compile(group(bgp("a", "b", "o"), filter));

        Expr root = filterExpr(body);
        // one of the two AND operands must be the EXIST with a compiled pattern
        boolean foundExist = false;
        for (Expr operand : root.getExpList()) {
            if (operand.oper() == ExprType.EXIST) {
                foundExist = true;
                assertInstanceOf(Exp.class, operand.getPattern(), "nested EXISTS keeps its compiled pattern");
            }
        }
        assertTrue(foundExist, "EXISTS is preserved when nested inside a compound filter");
    }

    @Test
    @DisplayName("{ ?s ?p ?o FILTER EXISTS { ?s ?q ?z } } : ?z is collected as a query node (exists scope)")
    void existsVariablesAreCollectedInExistsScope() {
        FilterAst filter = new FilterAst(new ExistsAst(group(bgp("s", "q", "z"))));
        Exp body = compiler.compile(group(bgp("s", "p", "o"), filter));

        Query query = Query.create(body);
        query.collect();

        assertTrue(query.getPatternNodes().stream().anyMatch(n -> "o".equals(n.getLabel())));
        assertFalse(query.getPatternNodes().stream().anyMatch(n -> "z".equals(n.getLabel())),
                "?z lives inside EXISTS and must not leak into the outer pattern scope");
        assertTrue(query.getQueryNodes().stream().anyMatch(n -> "z".equals(n.getLabel())),
                "?z is collected through collectExist -> getPattern()");
    }
}