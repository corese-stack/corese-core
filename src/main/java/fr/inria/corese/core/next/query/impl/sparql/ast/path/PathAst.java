package fr.inria.corese.core.next.query.impl.sparql.ast.path;

public sealed interface PathAst
        permits PredicatePathAst,
        SequencePathAst,
        AlternativePathAst,
        ZeroOrMorePathAst,
        OneOrMorePathAst,
        OptionalPathAst,
        InversePathAst,
        NegatedPropertySetPathAst {
}