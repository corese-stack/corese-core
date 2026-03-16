package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.ExprAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function call of a function identified by an IRI
 */
public record FunctionCallAst(TermAst functionName, List<TermAst> arguments) implements ExprAst {

}
