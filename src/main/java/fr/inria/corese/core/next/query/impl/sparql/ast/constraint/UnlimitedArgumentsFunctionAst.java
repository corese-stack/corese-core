package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ConstraintAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

public interface UnlimitedArgumentsFunctionAst extends ConstraintAst {
    List<TermAst> arguments();
}
