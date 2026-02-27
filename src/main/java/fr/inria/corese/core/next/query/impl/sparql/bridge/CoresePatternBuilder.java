package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Exp;

/**
 * Builds KGRAM structures from AST pattern nodes.
 */
public final class CoresePatternBuilder {

    public CoresePatternBuilder() {
    }

    private static Node toNode(TermAst term) {
        return CoreseTermAdapter.toNode(term);
    }

    public Edge toEdge(TriplePatternAst triple) {
        Node sub = toNode(triple.subject());
        Node pred = toNode(triple.predicate());
        Node obj = toNode(triple.object());
        return AstEdge.create(sub, pred, obj);
    }

    public Exp toBgpExp(BgpAst bgp) {
        Exp exp = Exp.create(ExpType.Type.BGP);
        for (TriplePatternAst t : bgp.triples()) {
            exp.add(toEdge(t));
        }
        return exp;
    }

    public Exp toExp(PatternAst pattern) {
        if (pattern instanceof BgpAst bgp) {
            return toBgpExp(bgp);
        }
        throw new IllegalArgumentException("Unsupported pattern type: " + pattern.getClass().getName());
    }
}
