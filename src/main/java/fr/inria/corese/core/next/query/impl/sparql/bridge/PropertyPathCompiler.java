package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.path.PropertyPath;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.AlternativePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.InversePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.NegatedPropertySetPathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.OneOrMorePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.OptionalPathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.SequencePathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.ZeroOrMorePathAst;

import java.util.ArrayList;
import java.util.List;

/** Resolves path IRIs at the bridge boundary, including inverse exclusions. */
final class PropertyPathCompiler {
    private final SparqlTermResolver terms;

    PropertyPathCompiler(SparqlTermResolver terms) {
        this.terms = terms;
    }

    PropertyPath compile(PathAst path) {
        return switch (path) {
            case PredicatePathAst(TermAst predicate) -> new PropertyPath.Predicate(terms.toNode(predicate));
            case SequencePathAst(PathAst left, PathAst right) ->
                    new PropertyPath.Sequence(compile(left), compile(right));
            case AlternativePathAst(PathAst left, PathAst right) ->
                    new PropertyPath.Alternative(compile(left), compile(right));
            case InversePathAst(PathAst operand) -> new PropertyPath.Inverse(compile(operand));
            case ZeroOrMorePathAst(PathAst operand) ->
                    new PropertyPath.Repetition(compile(operand), true, true);
            case OneOrMorePathAst(PathAst operand) ->
                    new PropertyPath.Repetition(compile(operand), false, true);
            case OptionalPathAst(PathAst operand) ->
                    new PropertyPath.Repetition(compile(operand), true, false);
            case NegatedPropertySetPathAst p -> negated(p);
        };
    }

    private PropertyPath negated(NegatedPropertySetPathAst path) {
        List<Node> forward = new ArrayList<>();
        List<Node> backward = new ArrayList<>();
        for (PathAst excluded : path.excluded()) {
            if (excluded instanceof InversePathAst(PathAst operand)) {
                backward.add(terms.toNode(WhereCompiler.simplePredicate(operand)));
            } else {
                forward.add(terms.toNode(WhereCompiler.simplePredicate(excluded)));
            }
        }
        return new PropertyPath.Negated(forward, backward);
    }
}
