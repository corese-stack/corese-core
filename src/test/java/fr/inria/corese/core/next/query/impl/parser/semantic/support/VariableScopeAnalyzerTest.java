package fr.inria.corese.core.next.query.impl.parser.semantic.support;

import fr.inria.corese.core.next.query.impl.parser.AbstractSparqlParserFeatureTest;
import fr.inria.corese.core.next.query.impl.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ValueMappingAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ValuesAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.WhereClauseQueryAst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VariableScopeAnalyzer")
class VariableScopeAnalyzerTest extends AbstractSparqlParserFeatureTest {

    private final VariableScopeAnalyzer analyzer = new VariableScopeAnalyzer();

    @Test
    @DisplayName("collectVisibleVariables includes VALUES variables and ignores null placeholders")
    void shouldCollectVisibleVariablesFromValuesMappings() {
        Map<VarAst, fr.inria.corese.core.next.query.impl.sparql.ast.TermAst> firstMapping = new LinkedHashMap<>();
        firstMapping.put(new VarAst("s"), new VarAst("o"));
        firstMapping.put(null, new VarAst("ignored"));

        Map<VarAst, fr.inria.corese.core.next.query.impl.sparql.ast.TermAst> secondMapping = new LinkedHashMap<>();
        secondMapping.put(new VarAst("rank"), null);
        secondMapping.put(new VarAst("s"), new VarAst("p"));

        ValuesAst values = new ValuesAst(List.of(
                new ValueMappingAst(firstMapping),
                new ValueMappingAst(secondMapping)));

        assertEquals(Set.of("s", "rank"), analyzer.collectVisibleVariables(values));
    }

    @Test
    @DisplayName("collectReferencedVariablesOutsideAggregates ignores variables shielded by aggregates")
    void shouldIgnoreVariablesReferencedOnlyInsideAggregates() {
        SparqlParser parser = newParserDefault();

        QueryAst ast = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    BIND(IF(BOUND(?s), COUNT(?o), ?p) AS ?result)
                }
                """);

        WhereClauseQueryAst whereClauseQueryAst = assertInstanceOf(WhereClauseQueryAst.class, ast);
        BindAst bind = assertInstanceOf(BindAst.class, whereClauseQueryAst.whereClause().patterns().getLast());

        assertEquals(Set.of("s", "p"), analyzer.collectReferencedVariablesOutsideAggregates(bind.expression()));
    }

    @Test
    @DisplayName("containsAggregate detects nested aggregate calls")
    void shouldDetectNestedAggregates() {
        SparqlParser parser = newParserDefault();

        QueryAst withAggregate = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    BIND(COALESCE(?s, SUM(?o)) AS ?result)
                }
                """);
        QueryAst withoutAggregate = parser.parse("""
                SELECT * WHERE {
                    ?s ?p ?o .
                    BIND(COALESCE(?s, ?o) AS ?result)
                }
                """);

        WhereClauseQueryAst aggregatedQuery = assertInstanceOf(WhereClauseQueryAst.class, withAggregate);
        WhereClauseQueryAst plainQuery = assertInstanceOf(WhereClauseQueryAst.class, withoutAggregate);

        BindAst aggregatedBind = assertInstanceOf(BindAst.class, aggregatedQuery.whereClause().patterns().getLast());
        BindAst plainBind = assertInstanceOf(BindAst.class, plainQuery.whereClause().patterns().getLast());

        assertTrue(analyzer.containsAggregate(aggregatedBind.expression()));
        assertFalse(analyzer.containsAggregate(plainBind.expression()));
    }
}
