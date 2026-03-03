package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CoreseAstQueryBuilder}.
 */
class CoreseAstQueryBuilderTest {

    private final CoreseAstQueryBuilder builder = new CoreseAstQueryBuilder();

    @Nested
    @DisplayName("buildBody(QueryAst)")
    class BuildBodyQueryAstTest {

        @Test
        @DisplayName("null ast returns empty BGP exp")
        void nullAst() {
            Exp exp = builder.buildBody((fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst) null);
            assertNotNull(exp);
            assertEquals(ExpType.Type.BGP, exp.type());
            assertEquals(0, exp.size());
        }

        @Test
        @DisplayName("SelectQueryAst with where clause builds body from patterns")
        void selectWithWhere() {
            TriplePatternAst t = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            BgpAst bgp = new BgpAst(List.of(t));
            GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(bgp));
            SelectQueryAst ast = new SelectQueryAst(where);
            Exp exp = builder.buildBody(ast);
            assertNotNull(exp);
            assertEquals(ExpType.Type.BGP, exp.type());
            assertEquals(1, exp.size());
        }
    }

    @Nested
    @DisplayName("buildBody(GroupGraphPatternAst)")
    class BuildBodyGroupTest {

        @Test
        @DisplayName("null group returns empty BGP exp")
        void nullGroup() {
            Exp exp = builder.buildBody((GroupGraphPatternAst) null);
            assertNotNull(exp);
            assertEquals(ExpType.Type.BGP, exp.type());
            assertEquals(0, exp.size());
        }

        @Test
        @DisplayName("empty patterns returns empty BGP exp")
        void emptyPatterns() {
            Exp exp = builder.buildBody(new GroupGraphPatternAst(List.of()));
            assertNotNull(exp);
            assertEquals(0, exp.size());
        }

        @Test
        @DisplayName("group with one BGP containing one triple")
        void oneBgpOneTriple() {
            TriplePatternAst t = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            BgpAst bgp = new BgpAst(List.of(t));
            GroupGraphPatternAst group = new GroupGraphPatternAst(List.of(bgp));
            Exp exp = builder.buildBody(group);
            assertEquals(1, exp.size());
        }

        @Test
        @DisplayName("group with two BGPs aggregates all triples")
        void twoBgps() {
            TriplePatternAst t1 = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            TriplePatternAst t2 = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p2"), new VarAst("o2"));
            GroupGraphPatternAst group = new GroupGraphPatternAst(List.of(
                    new BgpAst(List.of(t1)),
                    new BgpAst(List.of(t2))));
            Exp exp = builder.buildBody(group);
            assertEquals(2, exp.size());
        }
    }

    @Nested
    @DisplayName("buildQuery")
    class BuildQueryTest {

        @Test
        @DisplayName("buildQuery sets body on Query")
        void buildQuerySetsBody() {
            TriplePatternAst t = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            SelectQueryAst ast = new SelectQueryAst(
                    new GroupGraphPatternAst(List.of(new BgpAst(List.of(t)))));
            Query q = builder.buildQuery(ast);
            assertNotNull(q);
            Exp body = q.getBody();
            assertNotNull(body);
            assertEquals(ExpType.Type.BGP, body.type());
            assertEquals(1, body.size());
        }
    }
}
