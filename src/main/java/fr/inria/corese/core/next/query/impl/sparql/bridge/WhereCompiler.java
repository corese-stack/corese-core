package fr.inria.corese.core.next.query.impl.sparql.bridge;

import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.MinusAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OptionalAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ServiceAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UnionAst;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.ExpType.Type;
import fr.inria.corese.core.next.query.kgram.api.core.Filter;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.core.Exp;
import fr.inria.corese.core.next.query.kgram.core.Query;

import java.util.Objects;

/**
 * Compiles the content of a SPARQL {@code WHERE} clause — a tree of
 * {@link PatternAst} nodes — into a KGRAM {@link Exp} body that the engine can
 * evaluate.
 */
public final class WhereCompiler {

    /**
     * Compiles a full SPARQL {@code WHERE} clause into the runtime body carried by a
     * KGRAM {@link Query}.
     *
     * <p>This is the main entry point of the compiler. It expects the root
     * {@link GroupGraphPatternAst} produced by the parser for a complete
     * {@code WHERE { ... }} block.</p>
     */
    public Exp compile(GroupGraphPatternAst where) {
        Objects.requireNonNull(where, "where");
        return compileGroup(where);
    }

    /**
     * Compiles a single {@link PatternAst} node.
     *
     * <p>This package-visible dispatcher is used inside the bridge while recursively
     * traversing a {@code WHERE} tree, and by same-package tests that exercise one
     * pattern kind in isolation.</p>
     */
    Exp compile(PatternAst pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return switch (pattern) {
            case BgpAst bgp -> compileBgp(bgp);
            case FilterAst filter -> compileFilter(filter);
            case UnionAst union -> compileUnion(union);
            case OptionalAst optional -> compileOptional(optional);
            case MinusAst minus -> compileMinus(minus);
            case BindAst bind -> compileBind(bind);
            case ServiceAst service -> compileService(service);
            case GroupGraphPatternAst group -> compileGroup(group);
            default -> throw new UnsupportedOperationException(
                    "WHERE compilation not yet supported for: " + pattern.getClass().getSimpleName());
        };
    }

    /**
     * {@code { e1 e2 ... en }} becomes an {@code AND} of the compiled elements.
     */
    private Exp compileGroup(GroupGraphPatternAst group) {
        Exp body = Exp.create(Type.AND);
        for (PatternAst element : group.patterns()) {
            switch (element) {
                case OptionalAst(PatternAst ast) -> {
                    Exp left = body;
                    Exp right = compile(ast);
                    Exp optionalExp = Exp.create(Type.OPTIONAL, left, right);
                    body = Exp.create(Type.AND);
                    body.add(optionalExp);
                }
                case MinusAst(GroupGraphPatternAst pattern) -> {
                    Exp left = body;
                    Exp right = compile(pattern);
                    Exp minusExp = Exp.create(Type.MINUS, left, right);
                    body = Exp.create(Type.AND);
                    body.add(minusExp);
                }
                default -> body.add(compile(element));
            }
        }
        return body;
    }

    private Exp compileBgp(BgpAst bgp) {
        Exp pattern = Exp.create(Type.BGP);
        for (TriplePatternAst triple : bgp.triples()) {
            pattern.add(toEdge(triple));
        }
        return pattern;
    }

    private Edge toEdge(TriplePatternAst triple) {
        Node subject = CoreseAstQueryBuilder.toNode(triple.subject());
        Node predicate = CoreseAstQueryBuilder.toNode(CoreseAstQueryBuilder.simplePredicate(triple.predicate()));
        Node object = CoreseAstQueryBuilder.toNode(triple.object());
        return new AstBackedEdge(subject, predicate, object);
    }

    private Exp compileFilter(FilterAst filter) {
        Filter nextFilter = SparqlAstToExpression.toNextFilter(filter);
        return Exp.create(Type.FILTER, nextFilter);
    }

    private Exp compileUnion(UnionAst union) {
        Exp left = compile(union.left());
        Exp right = compile(union.right());
        return Exp.create(Type.UNION, left, right);
    }

    /**
     * Compiles a bare {@link OptionalAst}, i.e. one that is not folded with a
     * preceding pattern by {@link #compileGroup(GroupGraphPatternAst)} (for
     * instance a lone {@code { OPTIONAL { ... } }}). The mandatory left part is
     * then the empty pattern, matching SPARQL's {@code {} OPTIONAL { ... }}.
     */
    private Exp compileOptional(OptionalAst optional) {
        Exp left = Exp.create(Type.AND);
        Exp right = compile(optional.ast());
        return Exp.create(Type.OPTIONAL, left, right);
    }

    /**
     * Compiles a bare {@link MinusAst} not folded with a preceding pattern.
     * The mandatory left part is the empty pattern, matching {@code {} MINUS { ... }}.
     */
    private Exp compileMinus(MinusAst minus) {
        Exp left = Exp.create(Type.AND);
        Exp right = compile(minus.pattern());
        return Exp.create(Type.MINUS, left, right);
    }

    /**
     * Compiles {@code BIND(expression AS ?var)} into a KGRAM {@link Exp}.
     */
    private Exp compileBind(BindAst bind) {
        Filter filter = SparqlAstToExpression.toNextFilter(bind.expression());
        Node variable = CoreseAstQueryBuilder.toNode(bind.variable());
        Exp exp = Exp.create(Type.BIND);
        exp.setFilter(filter);
        exp.setFunctional(filter.isFunctional());
        exp.setNode(variable);
        return exp;
    }

    /**
     * Compiles {@code SERVICE <endpoint> { ... }} into a KGRAM {@link Exp}.
     */
    private Exp compileService(ServiceAst service) {
        Node endpoint = CoreseAstQueryBuilder.toNode(service.endpoint());
        Exp endpointNode = Exp.create(Type.NODE, endpoint);
        Query body = Query.create(compile(service.pattern()));
        body.setService(true);
        body.setSilent(service.silent());
        Exp exp = Exp.create(Type.SERVICE, endpointNode, body);
        exp.setSilent(service.silent());
        return exp;
    }
}
