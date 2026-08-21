package fr.inria.corese.core.next.query.impl.sparql.ast.path;

import fr.inria.corese.core.next.query.impl.sparql.parser.semantic.support.VisitableAst;

public sealed interface PathAst
        extends VisitableAst
        permits PredicatePathAst,
        SequencePathAst,
        AlternativePathAst,
        ZeroOrMorePathAst,
        OneOrMorePathAst,
        OptionalPathAst,
        InversePathAst,
        NegatedPropertySetPathAst {

}
