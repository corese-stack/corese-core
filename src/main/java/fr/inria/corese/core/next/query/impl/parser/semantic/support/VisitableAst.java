package fr.inria.corese.core.next.query.impl.parser.semantic.support;

public interface VisitableAst {

    void accept(AstVisitor visitor);
}
