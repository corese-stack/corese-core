package fr.inria.corese.core.next.query.impl.sparql;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CoreseQueryArtifactAdapter}.
 */
class CoreseQueryArtifactAdapterTest {

    @Nested
    @DisplayName("default constructor")
    class DefaultConstructorTest {

        @Test
        @DisplayName("toBody(null) returns empty BGP exp")
        void toBodyNullReturnsEmptyBgp() {
            CoreseQueryArtifactAdapter adapter = new CoreseQueryArtifactAdapter();
            Exp body = adapter.toBody(null);
            assertNotNull(body);
            assertEquals(ExpType.Type.BGP, body.type());
            assertEquals(0, body.size());
        }

        @Test
        @DisplayName("toQuery(null) returns Query with empty body")
        void toQueryNullReturnsQueryWithEmptyBody() {
            CoreseQueryArtifactAdapter adapter = new CoreseQueryArtifactAdapter();
            Query query = adapter.toQuery(null);
            assertNotNull(query);
            Exp body = query.getBody();
            assertNotNull(body);
            assertEquals(ExpType.Type.BGP, body.type());
            assertEquals(0, body.size());
        }
    }

    @Nested
    @DisplayName("toBody")
    class ToBodyTest {

        @Test
        @DisplayName("toBody(SelectQueryAst with WHERE) returns BGP exp with triples")
        void toBodyWithWhereClause() {
            CoreseQueryArtifactAdapter adapter = new CoreseQueryArtifactAdapter();
            TriplePatternAst triple = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            BgpAst bgp = new BgpAst(List.of(triple));
            GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(bgp));
            SelectQueryAst ast = new SelectQueryAst(where);

            Exp body = adapter.toBody(ast);

            assertNotNull(body);
            assertEquals(ExpType.Type.BGP, body.type());
            assertEquals(1, body.size());
        }

        @Test
        @DisplayName("toBody(SelectQueryAst with empty WHERE) returns empty BGP exp")
        void toBodyWithEmptyWhere() {
            CoreseQueryArtifactAdapter adapter = new CoreseQueryArtifactAdapter();
            SelectQueryAst ast = new SelectQueryAst(new GroupGraphPatternAst(List.of()));

            Exp body = adapter.toBody(ast);

            assertNotNull(body);
            assertEquals(ExpType.Type.BGP, body.type());
            assertEquals(0, body.size());
        }
    }

    @Nested
    @DisplayName("toQuery")
    class ToQueryTest {

        @Test
        @DisplayName("toQuery(SelectQueryAst with WHERE) returns Query with body set")
        void toQueryWithWhereClause() {
            CoreseQueryArtifactAdapter adapter = new CoreseQueryArtifactAdapter();
            TriplePatternAst triple = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            BgpAst bgp = new BgpAst(List.of(triple));
            GroupGraphPatternAst where = new GroupGraphPatternAst(List.of(bgp));
            SelectQueryAst ast = new SelectQueryAst(where);

            Query query = adapter.toQuery(ast);

            assertNotNull(query);
            Exp body = query.getBody();
            assertNotNull(body);
            assertEquals(ExpType.Type.BGP, body.type());
            assertEquals(1, body.size());
        }

        @Test
        @DisplayName("toQuery(SelectQueryAst with empty WHERE) returns Query with empty body")
        void toQueryWithEmptyWhere() {
            CoreseQueryArtifactAdapter adapter = new CoreseQueryArtifactAdapter();
            SelectQueryAst ast = new SelectQueryAst(new GroupGraphPatternAst(List.of()));

            Query query = adapter.toQuery(ast);

            assertNotNull(query);
            assertEquals(0, query.getBody().size());
        }
    }

    @Nested
    @DisplayName("constructor with custom CoreseAstQueryBuilder")
    class CustomBuilderTest {

        @Test
        @DisplayName("adapter uses injected builder for toBody")
        void injectedBuilderUsedForToBody() {
            CoreseAstQueryBuilder builder = new CoreseAstQueryBuilder();
            CoreseQueryArtifactAdapter adapter = new CoreseQueryArtifactAdapter(builder);
            TriplePatternAst triple = new TriplePatternAst(
                    new VarAst("s"), new VarAst("p"), new VarAst("o"));
            SelectQueryAst ast = new SelectQueryAst(
                    new GroupGraphPatternAst(List.of(new BgpAst(List.of(triple)))));

            Exp fromAdapter = adapter.toBody(ast);
            Exp fromBuilder = builder.buildBody(ast);

            assertNotNull(fromAdapter);
            assertEquals(fromBuilder.type(), fromAdapter.type());
            assertEquals(fromBuilder.size(), fromAdapter.size());
        }

        @Test
        @DisplayName("adapter uses injected builder for toQuery")
        void injectedBuilderUsedForToQuery() {
            CoreseAstQueryBuilder builder = new CoreseAstQueryBuilder();
            CoreseQueryArtifactAdapter adapter = new CoreseQueryArtifactAdapter(builder);
            SelectQueryAst ast = new SelectQueryAst(
                    new GroupGraphPatternAst(List.of(new BgpAst(List.of(
                            new TriplePatternAst(new VarAst("s"), new VarAst("p"), new VarAst("o")))))));

            Query query = adapter.toQuery(ast);

            assertNotNull(query);
            Exp expectedBody = builder.buildBody(ast);
            assertEquals(expectedBody.type(), query.getBody().type());
            assertEquals(expectedBody.size(), query.getBody().size());
        }
    }
}
