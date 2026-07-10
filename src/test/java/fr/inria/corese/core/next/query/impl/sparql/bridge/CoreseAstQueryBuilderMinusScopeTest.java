package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreseAstQueryBuilderMinusScopeTest {

    private final WhereCompiler compiler = new WhereCompiler();

    private static BgpAst bgp(String s, String p, String o) {
        return new BgpAst(List.of(
                new TriplePatternAst(new VarAst(s), new VarAst(p), new VarAst(o))));
    }

    private static boolean hasLabel(List<Node> nodes, String label) {
        return nodes.stream().anyMatch(n -> label.equals(n.getLabel()));
    }

    @Test
    @DisplayName("{ ?s ?p ?o . MINUS { ?s ?q ?z } } : ?z (MINUS-only) is a query node, not a pattern node")
    void minusRightSideVariablesStayOutOfOuterScope() {
        Exp body = compiler.compile(new GroupGraphPatternAst(List.of(
                bgp("s", "p", "o"),
                new MinusAst(new GroupGraphPatternAst(List.of(bgp("s", "q", "z")))))));

        Query query = Query.create(body);
        query.collect();

        // WHERE pattern variables are in scope (pattern nodes)
        assertTrue(hasLabel(query.getPatternNodes(), "s"), "?s from WHERE is a pattern node");
        assertTrue(hasLabel(query.getPatternNodes(), "o"), "?o from WHERE is a pattern node");

        // MINUS-only variable ?z binds nothing outside: it must NOT be a pattern node
        assertFalse(hasLabel(query.getPatternNodes(), "z"),
                "?z appears only in the MINUS body and must not leak into the outer pattern scope");

        // it is collected in the query (minus/exists) node list instead
        assertTrue(hasLabel(query.getQueryNodes(), "z"),
                "?z from the MINUS body is stored as a query node (minus scope), stored separately");
    }

    @Test
    @DisplayName("{ ?s ?p ?o . OPTIONAL { ?s ?q ?z } } : ?z (OPTIONAL-only) is a pattern node visible in the outer scope")
    void optionalVariablesAreInOuterScope() {
        Exp body = compiler.compile(new GroupGraphPatternAst(List.of(
                bgp("s", "p", "o"),
                new OptionalAst(new GroupGraphPatternAst(List.of(bgp("s", "q", "z")))))));

        Query query = Query.create(body);
        query.collect();

        assertTrue(hasLabel(query.getPatternNodes(), "s"), "?s from WHERE is a pattern node");
        assertTrue(hasLabel(query.getPatternNodes(), "o"), "?o from WHERE is a pattern node");
        // OPTIONAL variables ARE in the outer scope (unlike MINUS)
        assertTrue(hasLabel(query.getPatternNodes(), "z"),
                "?z from OPTIONAL body must be visible in the outer pattern scope");
    }
}
