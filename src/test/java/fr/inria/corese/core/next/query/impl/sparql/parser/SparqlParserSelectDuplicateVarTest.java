package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.api.exception.QuerySyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the SELECT clause projection rules from SPARQL 1.1 §18.2.4.4:
 * <ul>
 *   <li>Plain variable projection is a set — duplicates are silently deduplicated.</li>
 *   <li>A plain variable reference after an alias is permitted (it just references the alias).</li>
 *   <li>An alias whose target variable was already projected (bare or via another alias) is rejected.</li>
 * </ul>
 */
@DisplayName("SPARQL parser - SELECT clause projection rules")
class SparqlParserSelectDuplicateVarTest extends AbstractSparqlParserFeatureTest {

    @Test
    @DisplayName("SELECT with distinct variables is valid")
    void distinctVariablesAreValid() {
        assertDoesNotThrow(() -> newParserDefault().parse("""
                SELECT ?x ?y WHERE { ?x ?p ?y }
                """));
    }

    @Test
    @DisplayName("SELECT ?x ?x is valid — duplicates are silently deduplicated (SPARQL 1.1 §18.2.4.4)")
    void duplicateBareVariablesAreSilentlyDeduplicated() {
        assertDoesNotThrow(() -> newParserDefault().parse("""
                SELECT ?x ?x WHERE { ?x ?p ?o }
                """), "Duplicate plain variables must be silently deduplicated per SPARQL 1.1");
    }

    @Test
    @DisplayName("SELECT (expr AS ?x) ?x is valid — bare reference after alias is permitted")
    void bareReferenceAfterAliasIsValid() {
        // ?x is not in the WHERE scope, so the alias introduces it and the bare ref is just a back-reference
        assertDoesNotThrow(() -> newParserDefault().parse("""
                SELECT (?o AS ?x) ?x WHERE { ?s ?p ?o }
                """), "A bare variable reference after an alias must be permitted per SPARQL 1.1");
    }

    @Test
    @DisplayName("SELECT ?x (1 AS ?x) is rejected — alias redefines an already-projected variable")
    void aliasRedefineBareVariableIsRejected() {
        assertThrows(QuerySyntaxException.class, () -> newParserDefault().parse("""
                SELECT ?x (1 AS ?x) WHERE { ?x ?p ?o }
                """), "An alias may not redefine a variable already in the projection");
    }

    @Test
    @DisplayName("SELECT (?o AS ?label) (?x AS ?label) is rejected — two aliases with same target")
    void twoAliasesWithSameNameAreRejected() {
        assertThrows(QuerySyntaxException.class, () -> newParserDefault().parse("""
                SELECT (?o AS ?label) (?x AS ?label) WHERE { ?x ?p ?o }
                """), "Two expression aliases may not share the same variable name in SELECT");
    }
}
