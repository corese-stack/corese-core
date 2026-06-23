package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.impl.sparql.ast.constraint.GreaterThanAst;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WhereCompilerTest {

    private final WhereCompiler compiler = new WhereCompiler();


    private static GroupGraphPatternAst group(PatternAst... elements) {
        return new GroupGraphPatternAst(List.of(elements));
    }


    @Test
    @DisplayName("OPTIONAL en left join (first = partie obligatoire, rest = partie optionnelle)")
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
    @DisplayName("UNION binaire de ses deux branches")
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
    @DisplayName("FILTER ajouté après le BGP dans le groupe")
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
    @DisplayName("MINUS folded avec le pattern précédent")
    void compilesMinus() {
        BgpAst main = new BgpAst(List.of(
                new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))));
        BgpAst subtracted = new BgpAst(List.of(
                new TriplePatternAst(new VarAst("s"), new VarAst("q"), new VarAst("z"))));

        Exp body = compiler.compile(group(main, new MinusAst(group(subtracted))));

        assertTrue(body.isAnd());
        Exp minusExp = body.get(0);
        assertTrue(minusExp.isMinus(), "MINUS node");
        assertTrue(minusExp.first().isAnd(), "left part est le pattern précédent");
        assertTrue(minusExp.first().get(0).isBGP());
        assertTrue(minusExp.rest().isAnd(), "right part est le corps du MINUS");
    }


    @Test
    @DisplayName("BIND produit un Exp avec filter et node variable")
    void compilesBind() {
        BindAst bind = new BindAst(new VarAst("x"), new VarAst("y"));

        Exp body = compiler.compile(group(bind));

        assertEquals(1, body.size());
        Exp bindExp = body.get(0);
        assertTrue(bindExp.isBind(), "BIND node");
        assertNotNull(bindExp.getFilter(), "filter porte l'expression");
        assertNotNull(bindExp.getNode(), "node porte la variable cible");
    }

    @Test
    @DisplayName("SERVICE produit un Exp avec endpoint et corps")
    void compilesService() {
        ServiceAst service = new ServiceAst(
                new IriAst("<http://example.org/>"),
                false,
                group(new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))));

        Exp body = compiler.compile(group(service));

        Exp serviceExp = body.get(0);
        assertTrue(serviceExp.isService(), "SERVICE node");
        assertFalse(serviceExp.isSilent(), "pas de flag SILENT");
        assertTrue(serviceExp.rest().isAnd(), "le corps est un groupe AND");
    }

    @Test
    @DisplayName("SERVICE SILENT positionne le flag isSilent")
    void compilesServiceSilent() {
        ServiceAst service = new ServiceAst(
                new IriAst("<http://example.org/>"),
                true,
                group(new BgpAst(List.of(
                        new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))))));

        Exp serviceExp = compiler.compile(service);

        assertTrue(serviceExp.isService());
        assertTrue(serviceExp.isSilent(), "flag SILENT activé");
    }
}