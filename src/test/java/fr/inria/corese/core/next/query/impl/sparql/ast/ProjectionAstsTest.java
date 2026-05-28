package fr.inria.corese.core.next.query.impl.sparql.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProjectionAsts")
class ProjectionAstsTest {

    @Test
    @DisplayName("selectAll creates an empty wildcard projection")
    void shouldCreateSelectAllProjection() {
        ProjectionAst projection = ProjectionAsts.selectAll();

        assertTrue(projection.selectAll());
        assertTrue(projection.variables().isEmpty());
        assertTrue(projection.expressionBoundVariables().isEmpty());
        assertTrue(projection.expressionTerms().isEmpty());
        assertTrue(projection.expressionReferencedVariables().isEmpty());
    }

    @Test
    @DisplayName("of returns selectAll for null or empty variables")
    void shouldFallbackToSelectAllForMissingVariables() {
        assertTrue(ProjectionAsts.of(null).selectAll());
        assertTrue(ProjectionAsts.of(List.of()).selectAll());
    }

    @Test
    @DisplayName("of retains projection metadata defensively")
    void shouldRetainExpressionMetadataDefensively() {
        List<VarAst> variables = new ArrayList<>(List.of(new VarAst("s"), new VarAst("label")));
        Set<String> expressionBoundVariables = new LinkedHashSet<>(Set.of("label"));
        Map<String, TermAst> expressionTerms = new LinkedHashMap<>(Map.of("label", new VarAst("s")));
        Map<String, Set<String>> expressionReferencedVariables =
                new LinkedHashMap<>(Map.of("label", new LinkedHashSet<>(Set.of("s"))));

        ProjectionAst projection = ProjectionAsts.of(
                variables,
                expressionBoundVariables,
                expressionTerms,
                expressionReferencedVariables);

        variables.add(new VarAst("other"));
        expressionBoundVariables.add("other");
        expressionTerms.put("label", new VarAst("o"));
        expressionReferencedVariables.get("label").add("o");

        assertFalse(projection.selectAll());
        assertEquals(List.of(new VarAst("s"), new VarAst("label")), projection.variables());
        assertEquals(Set.of("label"), projection.expressionBoundVariables());
        assertEquals(Map.of("label", new VarAst("s")), projection.expressionTerms());
        assertEquals(Map.of("label", Set.of("s")), projection.expressionReferencedVariables());
    }
}
