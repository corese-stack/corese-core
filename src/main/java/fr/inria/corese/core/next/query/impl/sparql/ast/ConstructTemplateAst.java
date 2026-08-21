package fr.inria.corese.core.next.query.impl.sparql.ast;

import java.util.List;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VisitableAst;

/**
 * CONSTRUCT template: list of triple templates used to build the output RDF
 * graph.
 *
 * <p>
 * Unlike a WHERE clause BGP, a construct template does not represent a graph
 * pattern to match,
 * but triples to instantiate from bindings.
 * <p/>
 *
 * @param triplePatternAsts triples to instantiate from solution bindings
 */
public record ConstructTemplateAst(List<TriplePatternAst> triplePatternAsts) implements VisitableAst {
    public ConstructTemplateAst {
        triplePatternAsts = triplePatternAsts != null ? List.copyOf(triplePatternAsts) : List.of();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.triplePatternAsts.forEach(triplePatternAst -> triplePatternAst.accept(visitor));
    }
}
