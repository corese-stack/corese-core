package fr.inria.corese.core.next.query.impl.sparql.ast.visitor;

public interface VisitableAst {

    void accept(AstVisitor visitor);
}
