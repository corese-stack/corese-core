package fr.inria.corese.core.next.query.impl.sparql.parser;

import fr.inria.corese.core.next.generated.antlr.SparqlParser;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.*;

import java.util.List;

/**
 * Property-path building helpers extracted from {@link SparqlAstBuilder}.
 * <p>
 * All methods are pure functions of the ANTLR parse-tree contexts; the only
 * dependency on shared state is {@link SparqlTermBuilder} (needed to create
 * IRI terms for path predicates).
 */
public final class SparqlPathBuilder {

    private final SparqlTermBuilder terms;

    public SparqlPathBuilder(SparqlTermBuilder terms) {
        this.terms = terms;
    }

    public PathAst pathFromVerbPath(SparqlParser.VerbPathContext ctx) {
        return pathFromPath(ctx.path());
    }

    public PathAst pathFromPath(SparqlParser.PathContext ctx) {
        return pathFromPathAlternative(ctx.pathAlternative());
    }

    PathAst pathFromPathAlternative(SparqlParser.PathAlternativeContext ctx) {
        return foldAlternatives(ctx.pathSequence().stream().map(this::pathFromPathSequence).toList());
    }

    PathAst pathFromPathSequence(SparqlParser.PathSequenceContext ctx) {
        return foldSequences(ctx.pathEltOrInverse().stream().map(this::pathFromPathEltOrInverse).toList());
    }

    PathAst pathFromPathEltOrInverse(SparqlParser.PathEltOrInverseContext ctx) {
        if (ctx.CARET() != null) {
            return new InversePathAst(pathFromPathElt(ctx.pathElt()));
        }
        return pathFromPathElt(ctx.pathElt());
    }

    PathAst pathFromPathElt(SparqlParser.PathEltContext ctx) {
        PathAst primary = pathFromPathPrimary(ctx.pathPrimary());
        SparqlParser.PathModContext mod = ctx.pathMod();
        if (mod == null) {
            return primary;
        }
        if (mod.QUESTION() != null) {
            return new OptionalPathAst(primary);
        }
        if (mod.STAR() != null) {
            return new ZeroOrMorePathAst(primary);
        }
        if (mod.PLUS() != null) {
            return new OneOrMorePathAst(primary);
        }
        throw new QueryEvaluationException("Unexpected path modifier in " + ctx.getText());
    }

    PathAst pathFromPathPrimary(SparqlParser.PathPrimaryContext ctx) {
        if (ctx.iriRef() != null) {
            return new PredicatePathAst(terms.termFromIriRef(ctx.iriRef()));
        }
        if (ctx.A() != null) {
            return new PredicatePathAst(terms.iri("a"));
        }
        if (ctx.EXCLAMATION() != null) {
            return pathFromPathNegatedPropertySet(ctx.pathNegatedPropertySet());
        }
        if (ctx.path() != null) {
            return pathFromPath(ctx.path());
        }
        throw new QueryEvaluationException("Unexpected path primary in " + ctx.getText());
    }

    PathAst pathFromPathNegatedPropertySet(SparqlParser.PathNegatedPropertySetContext ctx) {
        List<PathAst> excluded;
        if (ctx.L_PAREN() != null) {
            excluded = ctx.pathOneInPropertySet().stream().map(this::pathFromPathOneInPropertySet).toList();
        } else {
            excluded = List.of(pathFromPathOneInPropertySet(ctx.pathOneInPropertySet().getFirst()));
        }
        return new NegatedPropertySetPathAst(excluded);
    }

    PathAst pathFromPathOneInPropertySet(SparqlParser.PathOneInPropertySetContext ctx) {
        if (ctx.CARET() != null) {
            if (ctx.iriRef() != null) {
                return new InversePathAst(new PredicatePathAst(terms.termFromIriRef(ctx.iriRef())));
            }
            return new InversePathAst(new PredicatePathAst(terms.iri("a")));
        }
        if (ctx.iriRef() != null) {
            return new PredicatePathAst(terms.termFromIriRef(ctx.iriRef()));
        }
        return new PredicatePathAst(terms.iri("a"));
    }

    /**
     * Left-folds a list of path alternatives into a binary {@link AlternativePathAst} tree.
     * <p>
     * Given paths {@code [p1, p2, p3]}, the result is
     * {@code AlternativePathAst(AlternativePathAst(p1, p2), p3)}, which faithfully
     * represents the left-associative semantics of the SPARQL {@code |} operator.
     * A singleton list is returned as-is without wrapping.
     *
     * @param parts the ordered list of alternative path branches; must not be empty
     * @return the left-folded {@link AlternativePathAst}, or the single element if the list
     *         has exactly one entry
     * @throws QueryEvaluationException if {@code parts} is empty
     */
    PathAst foldAlternatives(List<PathAst> parts) {
        if (parts.isEmpty()) {
            throw new QueryEvaluationException("Empty property path alternative");
        }
        PathAst result = parts.getFirst();
        for (int i = 1; i < parts.size(); i++) {
            result = new AlternativePathAst(result, parts.get(i));
        }
        return result;
    }

    /**
     * Left-folds a list of path elements into a binary {@link SequencePathAst} tree.
     * <p>
     * Given paths {@code [p1, p2, p3]}, the result is
     * {@code SequencePathAst(SequencePathAst(p1, p2), p3)}, which faithfully
     * represents the left-associative semantics of the SPARQL {@code /} operator.
     * A singleton list is returned as-is without wrapping.
     *
     * @param parts the ordered list of path sequence elements; must not be empty
     * @return the left-folded {@link SequencePathAst}, or the single element if the list
     *         has exactly one entry
     * @throws QueryEvaluationException if {@code parts} is empty
     */
    PathAst foldSequences(List<PathAst> parts) {
        if (parts.isEmpty()) {
            throw new QueryEvaluationException("Empty property path sequence");
        }
        PathAst result = parts.getFirst();
        for (int i = 1; i < parts.size(); i++) {
            result = new SequencePathAst(result, parts.get(i));
        }
        return result;
    }
}
