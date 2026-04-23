package fr.inria.corese.core.next.query.impl.sparql.ast.constraint;

import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;

import java.util.List;

/**
 * Function {@code STRDT(string, datatype)}: constructs a typed literal from a lexical
 * string and a datatype IRI.
 */
public class StrDtAst extends AbstractBinaryFunctionAst implements LiteralExpressionAst {
    public StrDtAst(List<TermAst> args) {
        super(args);
    }
}
