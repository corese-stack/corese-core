package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

import java.util.List;

public record NegatedPropertySetPathAst(List<PathAst> excluded) implements PathAst {
    public NegatedPropertySetPathAst {
        excluded = excluded != null ? List.copyOf(excluded) : List.of();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        if(this.excluded != null) {
            this.excluded.forEach(excludedPath -> {
                excludedPath.accept(visitor);
            });
        }
    }
}
