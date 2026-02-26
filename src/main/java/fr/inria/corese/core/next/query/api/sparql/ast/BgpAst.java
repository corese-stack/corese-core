package fr.inria.corese.core.next.query.api.sparql.ast;

import java.util.List;

/**
 * Basic Graph Pattern: a list of triple patterns (TriplesBlock).
 */
public record BgpAst(List<TriplePatternAst> triples) implements PatternAst {
    public BgpAst {
        triples = triples != null ? List.copyOf(triples) : List.of();
    }
}
