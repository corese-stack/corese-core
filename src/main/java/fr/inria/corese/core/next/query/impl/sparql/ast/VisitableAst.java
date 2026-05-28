package fr.inria.corese.core.next.query.impl.sparql.ast;

import fr.inria.corese.core.next.query.impl.parser.semantic.support.AstVisitor;

public interface VisitableAst {

    void accept(AstVisitor visitor);
}
