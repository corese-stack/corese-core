package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.*;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds KGRAM Exp (body) and Query from AST query nodes.
 */
public final class CoreseAstQueryBuilder {

    private final CoresePatternBuilder patternBuilder;

    public CoreseAstQueryBuilder() {
        this.patternBuilder = new CoresePatternBuilder();
    }

    public CoreseAstQueryBuilder(CoresePatternBuilder patternBuilder) {
        this.patternBuilder = patternBuilder;
    }

    public Exp buildBody(QueryAst ast) {
        if (ast == null) {
            return Exp.create(ExpType.Type.BGP);
        }
        return buildBody(ast.whereClause());
    }

    public Exp buildBody(GroupGraphPatternAst group) {
        if (group == null || group.patterns().isEmpty()) {
            return Exp.create(ExpType.Type.BGP);
        }
        List<TriplePatternAst> allTriples = new ArrayList<>();
        for (PatternAst p : group.patterns()) {
            if (p instanceof BgpAst bgp) {
                allTriples.addAll(bgp.triples());
            }
        }
        Exp bgpExp = Exp.create(ExpType.Type.BGP);
        for (TriplePatternAst t : allTriples) {
            bgpExp.add(patternBuilder.toEdge(t));
        }
        return bgpExp;
    }

    public Query buildQuery(QueryAst ast) {
        Exp body = buildBody(ast);
        Query q = new Query();
        q.setBody(body);
        return q;
    }
}
