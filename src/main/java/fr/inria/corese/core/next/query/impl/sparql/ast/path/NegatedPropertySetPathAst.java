package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.AstVisitor;

import java.util.List;
import java.util.Objects;

public record NegatedPropertySetPathAst(List<PathAst> excluded) implements PathAst {
    public NegatedPropertySetPathAst {
        excluded = excluded == null
                ? List.of()
                : excluded.stream()
                .map(path -> Objects.requireNonNull(path, "excluded path is null"))
                .toList();
    }

    @Override
    public void accept(AstVisitor visitor) {
        visitor.visit(this);
        excluded.forEach(path -> path.accept(visitor));
    }
}
