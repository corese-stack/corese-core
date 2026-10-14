package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SparqlParserTripleOrderTest {
    @Test
    void recursiveTriplesBlocksPreserveSourceOrder() {
        var query = (SelectQueryAst) new SparqlParser().parse("""
                PREFIX : <urn:bench:>
                SELECT ?s ?friend ?name WHERE {
                    ?s :group :g7 ; :knows ?friend .
                    ?friend :knows ?other .
                    ?other :name ?name .
                }
                """);
        List<String> subjects = query.whereClause().patterns().stream()
                .map(BgpAst.class::cast)
                .map(bgp -> ((VarAst) bgp.triples().getFirst().subject()).name()).toList();
        assertEquals(List.of("s", "friend", "other"), subjects);
    }
}
