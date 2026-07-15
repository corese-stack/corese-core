package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.parser.AbstractSparqlParserFeatureTest;
import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.FunctionCallAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterThanAst;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.tool.KgramNodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WhereCompilerTest extends AbstractSparqlParserFeatureTest {

    private final WhereCompiler compiler = new WhereCompiler();


    private static GroupGraphPatternAst group(PatternAst... elements) {
        return new GroupGraphPatternAst(List.of(elements));
    }


    @Test
    @DisplayName("OPTIONAL as a left join (first = mandatory part, rest = optional part)")
    void compilesOptionalAsLeftJoin() {
        BgpAst mandatory = new BgpAst(List.of(
                new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))));
        BgpAst optionalBgp = new BgpAst(List.of(
                new TriplePatternAst(new VarAst("s"), new VarAst("q"), new VarAst("z"))));

        Exp body = compiler.compile(group(mandatory, new OptionalAst(optionalBgp)));

        assertTrue(body.isAnd());
        Exp optional = body.get(0);
        assertTrue(optional.isOptional(), "OPTIONAL node");
        assertTrue(optional.first().isAnd(), "mandatory left part is the preceding group");
        assertTrue(optional.first().get(0).isBGP());
        assertTrue(optional.rest().isBGP(), "optional right part is the optional body");
    }


    @Test
    @DisplayName("UNION of its two branches")
    void compilesUnion() {
        UnionAst union = new UnionAst(
                group(new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))),
                group(new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("q"), new VarAst("z"))))));

        Exp body = compiler.compile(group(union));

        Exp unionExp = body.get(0);
        assertTrue(unionExp.isUnion());
        assertTrue(unionExp.first().isAnd());
        assertTrue(unionExp.rest().isAnd());
    }


    @Test
    @DisplayName("FILTER added after the BGP in the group")
    void compilesFilter() {
        BgpAst bgp = new BgpAst(List.of(
                new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))));
        FilterAst filter = new FilterAst(new GreaterThanAst(List.of(
                new VarAst("o"), new LiteralAst("5", null, null))));

        Exp body = compiler.compile(group(bgp, filter));

        assertEquals(2, body.size());
        assertTrue(body.get(0).isBGP());
        assertTrue(body.get(1).isFilter(), "second element is a FILTER");
    }


    @Test
    @DisplayName("MINUS folded with the preceding pattern")
    void compilesMinus() {
        BgpAst main = new BgpAst(List.of(
                new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))));
        BgpAst subtracted = new BgpAst(List.of(
                new TriplePatternAst(new VarAst("s"), new VarAst("q"), new VarAst("z"))));

        Exp body = compiler.compile(group(main, new MinusAst(group(subtracted))));

        assertTrue(body.isAnd());
        Exp minusExp = body.get(0);
        assertTrue(minusExp.isMinus(), "MINUS node");
        assertTrue(minusExp.first().isAnd(), "left part is the preceding pattern");
        assertTrue(minusExp.first().get(0).isBGP());
        assertTrue(minusExp.rest().isAnd(), "right part is the MINUS body");
    }


    @Test
    @DisplayName("OPTIONAL { BGP . FILTER } — the filter is compiled inside the optional right part")
    void compilesOptionalWithFilter() {
        FilterAst filter = new FilterAst(new GreaterThanAst(List.of(
                new VarAst("z"), new LiteralAst("5", null, null))));
        GroupGraphPatternAst optionalBody = group(bgp("s", "q", "z"), filter);

        Exp body = compiler.compile(group(bgp("s", "p", "o"), new OptionalAst(optionalBody)));

        Exp optional = body.get(0);
        assertTrue(optional.isOptional());
        Exp right = optional.rest();
        assertTrue(right.isAnd(), "optional body is a group");
        assertEquals(2, right.size(), "BGP + FILTER inside the optional body");
        assertTrue(right.get(0).isBGP());
        assertTrue(right.get(1).isFilter());
    }

    @Test
    @DisplayName("BIND produces an Exp with a filter and a target variable node")
    void compilesBind() {
        BindAst bind = new BindAst(new VarAst("x"), new VarAst("y"));

        Exp body = compiler.compile(group(bind));

        assertEquals(1, body.size());
        Exp bindExp = body.get(0);
        assertTrue(bindExp.isBind(), "BIND node");
        assertNotNull(bindExp.getFilter(), "filter carries the expression");
        assertNotNull(bindExp.getNode(), "node carries the target variable");
    }

    @Test
    @DisplayName("SERVICE produces an Exp with an endpoint and a body")
    void compilesService() {
        ServiceAst service = new ServiceAst(
                new IriAst("<http://example.org/>"),
                false,
                group(new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))));

        Exp body = compiler.compile(group(service));

        Exp serviceExp = body.get(0);
        assertTrue(serviceExp.isService(), "SERVICE node");
        assertFalse(serviceExp.isSilent(), "no SILENT flag");
        assertTrue(serviceExp.rest().isQuery(), "the body is a Query");
        assertTrue(serviceExp.rest().getQuery().getBody().isAnd(), "the Query body is an AND group");
    }

    @Test
    @DisplayName("SERVICE rest() exposes a Query for the runtime (exp.rest().getQuery() != null)")
    void serviceRestShouldExposeAQueryForRuntime() {
        ServiceAst service = new ServiceAst(
                new IriAst("<http://example.org/>"),
                false,
                group(new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))));

        Exp serviceExp = new WhereCompiler().compile(service);

        assertTrue(serviceExp.isService());
        assertNotNull(serviceExp.rest().getQuery(), "SERVICE runtime expects a query body");
    }

    @Test
    @DisplayName("BGP ?s ?p ?o — variable predicate uses KGRAM root property and edge variable")
    void edgeExposesVariablePredicateAsEdgeVariable() {
        Exp bgp = new WhereCompiler().compile(new BgpAst(List.of(
                new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o")))));

        Exp edgeExp = bgp.get(0);
        assertEquals(KgramNodes.ROOT_PROPERTY_URI, edgeExp.getEdge().getEdgeNode().getLabel());
        assertEquals(KgramNodes.ROOT_PROPERTY_URI, edgeExp.getEdge().getEdgeLabel());
        assertNotNull(edgeExp.getEdge().getEdgeVariable(), "variable predicate should be exposed as edge variable");
        assertTrue(edgeExp.getEdge().getEdgeVariable().isVariable());
    }

    @Test
    @DisplayName("BIND propagates the filter's isFunctional flag (e.g. unnest)")
    void bindShouldPropagateFunctionalFlag() {
        BindAst bind = new BindAst(
                new FunctionCallAst(new IriAst("<http://example.org/unnest>"), List.of(new VarAst("x"))),
                new VarAst("y"));

        Exp bindExp = new WhereCompiler().compile(bind);

        assertTrue(bindExp.getFilter().isFunctional());
        assertTrue(bindExp.isFunctional(), "BIND exp should propagate functional flag");
    }

    @Test
    @DisplayName("Unsupported WHERE pattern kinds fail with UnsupportedQueryFeatureException")
    void unsupportedPatternFailsWithTypedException() {
        SubQueryAst subQuery = new SubQueryAst(new SelectQueryAst(group(bgp("s", "p", "o"))));

        UnsupportedQueryFeatureException error = assertThrows(
                UnsupportedQueryFeatureException.class,
                () -> compiler.compile(subQuery));

        assertTrue(error.getMessage().contains("WHERE pattern"));
    }

    @Test
    @DisplayName("SERVICE SILENT sets the isSilent flag")
    void compilesServiceSilent() {
        ServiceAst service = new ServiceAst(
                new IriAst("<http://example.org/>"),
                true,
                group(new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))));

        Exp serviceExp = compiler.compile(service);

        assertTrue(serviceExp.isService());
        assertTrue(serviceExp.isSilent(), "SILENT flag enabled");
    }

    @Test
    @DisplayName("SERVICE body query is marked as a service query")
    void serviceBodyShouldBeMarkedAsServiceQuery() {
        ServiceAst service = new ServiceAst(
                new IriAst("<http://example.org/>"),
                false,
                group(new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))));

        Exp serviceExp = new WhereCompiler().compile(service);

        assertTrue(serviceExp.rest().getQuery().isService(),
                "SERVICE body query should be marked as service");
    }

    @Test
    @DisplayName("Pattern after OPTIONAL stays a sibling: { P1 . OPTIONAL{P2} . P3 } -> AND[ OPTIONAL(AND[P1], P2), BGP(P3) ]")
    void patternAfterOptionalStaysSibling() {
        Exp body = compiler.compile(group(
                bgp("s", "p", "o"),
                new OptionalAst(bgp("s", "q", "z")),
                bgp("s", "r", "w")));

        assertTrue(body.isAnd());
        assertEquals(2, body.size(), "OPTIONAL and the trailing BGP are two siblings");

        Exp optional = body.get(0);
        assertTrue(optional.isOptional());
        assertTrue(optional.first().isAnd(), "mandatory left part is the preceding pattern");
        assertTrue(optional.first().get(0).isBGP());
        assertTrue(optional.rest().isBGP(), "optional right part is P2");

        assertTrue(body.get(1).isBGP(), "P3 is a sibling of the OPTIONAL, not folded into it");
    }

    @Test
    @DisplayName("Chained OPTIONALs left-associate: { P1 . OPTIONAL{P2} . OPTIONAL{P3} } -> OPTIONAL(OPTIONAL(P1,P2), P3)")
    void chainedOptionalsLeftAssociate() {
        Exp body = compiler.compile(group(
                bgp("s", "p", "o"),
                new OptionalAst(bgp("s", "q", "z")),
                new OptionalAst(bgp("s", "r", "w"))));

        assertEquals(1, body.size());
        Exp outer = body.get(0);
        assertTrue(outer.isOptional(), "outermost OPTIONAL");
        assertTrue(outer.rest().isBGP(), "outer optional part is P3");

        Exp inner = outer.first().get(0);
        assertTrue(inner.isOptional(), "left part is the inner OPTIONAL(P1, P2)");
        assertTrue(inner.rest().isBGP(), "inner optional part is P2");
        assertTrue(inner.first().get(0).isBGP(), "inner mandatory part is P1");
    }

    @Test
    @DisplayName("Pattern after MINUS stays a sibling: { P1 . MINUS{P2} . P3 } -> AND[ MINUS(AND[P1], AND[P2]), BGP(P3) ]")
    void patternAfterMinusStaysSibling() {
        Exp body = compiler.compile(group(
                bgp("s", "p", "o"),
                new MinusAst(group(bgp("s", "q", "z"))),
                bgp("s", "r", "w")));

        assertTrue(body.isAnd());
        assertEquals(2, body.size());

        Exp minus = body.get(0);
        assertTrue(minus.isMinus());
        assertTrue(minus.first().isAnd(), "mandatory left part is the preceding pattern");
        assertTrue(minus.first().get(0).isBGP());
        assertTrue(minus.rest().isAnd(), "right part is the MINUS body");
        assertTrue(minus.rest().get(0).isBGP());

        assertTrue(body.get(1).isBGP(), "P3 is a sibling of the MINUS");
    }

    @Test
    @DisplayName("OPTIONAL first in the group has an empty mandatory left part: { OPTIONAL{P1} } -> OPTIONAL(AND[], P1)")
    void optionalFirstHasEmptyMandatoryPart() {
        Exp body = compiler.compile(group(new OptionalAst(bgp("s", "p", "o"))));

        assertEquals(1, body.size());
        Exp optional = body.get(0);
        assertTrue(optional.isOptional());
        assertTrue(optional.first().isAnd());
        assertEquals(0, optional.first().size(), "mandatory left part is empty ({} OPTIONAL { ... })");
        assertTrue(optional.rest().isBGP());
    }

    @Test
    @DisplayName("Bare OPTIONAL/MINUS via the pattern dispatcher use an empty mandatory left part")
    void bareOptionalAndMinusUseEmptyLeft() {
        Exp optional = compiler.compile(new OptionalAst(bgp("s", "p", "o")));
        assertTrue(optional.isOptional());
        assertTrue(optional.first().isAnd());
        assertEquals(0, optional.first().size(), "bare OPTIONAL: {} OPTIONAL { ... }");
        assertTrue(optional.rest().isBGP());

        Exp minus = compiler.compile(new MinusAst(group(bgp("s", "p", "o"))));
        assertTrue(minus.isMinus());
        assertTrue(minus.first().isAnd());
        assertEquals(0, minus.first().size(), "bare MINUS: {} MINUS { ... }");
        assertTrue(minus.rest().isAnd());
    }

    @Test
    @DisplayName("Parser -> WhereCompiler: OPTIONAL compiles to an OPTIONAL Exp")
    void parsedOptionalCompilesCorrectly() {
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class,
                newParserDefault().parse("SELECT * WHERE { ?s ?p ?o . OPTIONAL { ?s ?q ?z } }"));

        Exp body = compiler.compile(select.whereClause());

        assertTrue(body.isAnd());
        Exp optional = body.get(0);
        assertTrue(optional.isOptional(), "OPTIONAL compiled from parsed query");
        assertTrue(optional.first().isAnd(), "mandatory left part");
        assertTrue(optional.first().get(0).isBGP());
        assertTrue(optional.rest().isAnd(), "optional right part is the inner group");
    }

    @Test
    @DisplayName("Parser -> WhereCompiler: MINUS compiles to a MINUS Exp")
    void parsedMinusCompilesCorrectly() {
        SelectQueryAst select = assertInstanceOf(SelectQueryAst.class,
                newParserDefault().parse("SELECT * WHERE { ?s ?p ?o . MINUS { ?s ?q ?z } }"));

        Exp body = compiler.compile(select.whereClause());

        assertTrue(body.isAnd());
        Exp minus = body.get(0);
        assertTrue(minus.isMinus(), "MINUS compiled from parsed query");
        assertTrue(minus.first().isAnd(), "left part is the preceding pattern");
        assertTrue(minus.first().get(0).isBGP());
        assertTrue(minus.rest().isAnd(), "right part is the MINUS body");
    }

    private static BgpAst bgp(String s, String p, String o) {
        return new BgpAst(List.of(
                new TriplePatternAst(new VarAst(s), new VarAst(p), new VarAst(o))));
    }
}
