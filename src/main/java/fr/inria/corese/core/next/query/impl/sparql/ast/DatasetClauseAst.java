package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;
import fr.inria.corese.core.next.query.impl.parser.semantic.support.VisitableAst;

import java.util.LinkedHashSet;
import java.util.Set;

public record DatasetClauseAst(Set<IriAst> graphs, Set<IriAst> namedGraphs) implements VisitableAst {

    public DatasetClauseAst {
        graphs = graphs == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(graphs));
        namedGraphs = namedGraphs == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(namedGraphs));
    }

    public static DatasetClauseAst none() {
        return new DatasetClauseAst(Set.of(), Set.of());
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        this.graphs.forEach(iriAst -> {
            iriAst.accept(visitor);
        });
        this.namedGraphs.forEach(iriAst -> {
            iriAst.accept(visitor);
        });
    }
}
