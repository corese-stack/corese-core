package fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support;

public interface VisitableAst {

    void accept(AstVisitor visitor);
}
