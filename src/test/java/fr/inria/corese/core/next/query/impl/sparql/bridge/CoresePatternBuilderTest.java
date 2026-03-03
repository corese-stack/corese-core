package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CoresePatternBuilder}.
 */
class CoresePatternBuilderTest {

    private final CoresePatternBuilder builder = new CoresePatternBuilder();

    @Nested
    @DisplayName("toEdge")
    class ToEdgeTest {

        @Test
        @DisplayName("triple pattern produces edge with correct nodes")
        void tripleToEdge() {
            VarAst s = new VarAst("s");
            IriAst p = new IriAst("http://example.org/p");
            VarAst o = new VarAst("o");
            TriplePatternAst triple = new TriplePatternAst(s, p, o);
            Edge e = builder.toEdge(triple);
            assertNotNull(e);
            assertEquals("s", e.getNode(0).getLabel());
            assertEquals("http://example.org/p", e.getProperty().getLabel());
            assertEquals("o", e.getNode(1).getLabel());
        }
    }

    @Nested
    @DisplayName("toBgpExp")
    class ToBgpExpTest {

        @Test
        @DisplayName("empty BGP produces BGP exp with no edges")
        void emptyBgp() {
            BgpAst bgp = new BgpAst(List.of());
            Exp exp = builder.toBgpExp(bgp);
            assertNotNull(exp);
            assertEquals(ExpType.Type.BGP, exp.type());
            assertEquals(0, exp.size());
        }

        @Test
        @DisplayName("BGP with one triple produces exp with one edge")
        void singleTripleBgp() {
            TriplePatternAst t = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            BgpAst bgp = new BgpAst(List.of(t));
            Exp exp = builder.toBgpExp(bgp);
            assertNotNull(exp);
            assertEquals(1, exp.size());
        }

        @Test
        @DisplayName("BGP with two triples produces exp with two edges")
        void twoTriplesBgp() {
            TriplePatternAst t1 = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            TriplePatternAst t2 = new TriplePatternAst(
                    new VarAst("s"), new IriAst("http://type"), new VarAst("o"));
            BgpAst bgp = new BgpAst(List.of(t1, t2));
            Exp exp = builder.toBgpExp(bgp);
            assertEquals(2, exp.size());
        }
    }

    @Nested
    @DisplayName("toExp")
    class ToExpTest {

        @Test
        @DisplayName("BgpAst delegates to toBgpExp")
        void bgpToExp() {
            BgpAst bgp = new BgpAst(List.of(
                    new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o"))));
            Exp exp = builder.toExp(bgp);
            assertNotNull(exp);
            assertEquals(ExpType.Type.BGP, exp.type());
            assertEquals(1, exp.size());
        }
    }
}
