package fr.inria.corese.core.next.query.impl.sparql.bridge;


import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.impl.sparql.ast.BgpAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.BindAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.FilterAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GraphAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.GroupGraphPatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.MinusAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.OptionalAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.PatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryPrologueAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ServiceAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.SubQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UnionAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ValueMappingAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ValuesAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PathAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.path.PredicatePathAst;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.ExpType.Type;
import fr.inria.corese.core.next.query.impl.engine.model.Filter;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.pattern.Exp;
import fr.inria.corese.core.next.query.impl.engine.pattern.Query;
import fr.inria.corese.core.next.query.impl.engine.solution.Mapping;
import fr.inria.corese.core.next.query.impl.engine.solution.Mappings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compiles the content of a SPARQL {@code WHERE} clause — a tree of
 * {@link PatternAst} nodes — into a KGRAM {@link Exp} body that the engine can
 * evaluate.
 */
public final class WhereCompiler {

    private final SparqlTermResolver termResolver;

    public WhereCompiler() {
        this(QueryPrologueAst.empty());
    }

    private WhereCompiler(QueryPrologueAst prologue) {
        termResolver = new SparqlTermResolver(prologue);
    }

    WhereCompiler withPrologue(QueryPrologueAst prologue) {
        return new WhereCompiler(prologue);
    }

    SparqlTermResolver termResolver() {
        return termResolver;
    }

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
            case ValuesAst values -> compileValues(values);
            case ServiceAst service -> compileService(service);
            case GraphAst(TermAst name, GroupGraphPatternAst graphPattern) -> Exp.create(Type.GRAPH,
                    Exp.create(Type.GRAPHNODE, Exp.create(Type.NODE, termResolver.toNode(name))),
                    compile(graphPattern));
            case SubQueryAst subQuery -> compileSubQuery(subQuery);
            case GroupGraphPatternAst group -> compileGroup(group);
            default -> throw new UnsupportedQueryFeatureException(
                    "WHERE pattern is not supported yet by the next pipeline: "
                            + pattern.getClass().getSimpleName());
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
                case GroupGraphPatternAst nested -> {
                    Exp joined = Exp.create(Type.JOIN, body, compile(nested));
                    body = Exp.create(Type.AND);
                    body.add(joined);
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
        Node subject = termResolver.toPatternNode(triple.subject());
        Node predicate = termResolver.toPatternNode(
            simplePredicate(triple.predicate()));
        Node object = termResolver.toPatternNode(triple.object());
        return new AstBackedEdge(subject, predicate, object);
    }

    static TermAst simplePredicate(PathAst path) {
        if (path instanceof PredicatePathAst(TermAst predicate)) {
            return predicate;
        }
        throw new UnsupportedQueryFeatureException(
                "Property path bridge compilation is not supported yet by the next pipeline for: "
                        + path.getClass().getSimpleName());
    }

    private Exp compileFilter(FilterAst filter) {
        Filter nextFilter = new AstBackedExpr(filter.operator(), this).getFilter();
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
        Filter filter = new AstBackedExpr(bind.expression(), this).getFilter();
        Node variable = termResolver.toNode(bind.variable());
        Exp exp = Exp.create(Type.BIND);
        exp.setFilter(filter);
        exp.setFunctional(filter.isFunctional());
        exp.setNode(variable);
        return exp;
    }

    /** Lowers a SPARQL inline-data table to the runtime's native VALUES expression. */
    Exp compileValues(ValuesAst values) {
        List<Node> variables = new ArrayList<>();
        for (VarAst variable : values.variables()) {
            variables.add(termResolver.toNode(variable));
        }

        Mappings mappings = new Mappings();
        for (ValueMappingAst row : values.mappings()) {
            List<Node> rowVariables = new ArrayList<>();
            List<Node> rowValues = new ArrayList<>();
            for (int index = 0; index < values.variables().size(); index++) {
                VarAst variable = values.variables().get(index);
                TermAst value = row.values().get(variable);
                if (value != null) {
                    rowVariables.add(variables.get(index));
                    rowValues.add(termResolver.toNode(value));
                }
            }
            mappings.add(Mapping.create(rowVariables, rowValues));
        }
        return Exp.createValues(variables, mappings);
    }

    /**
     * Compiles {@code SERVICE <endpoint> { ... }} into a KGRAM {@link Exp}.
     */
    private Exp compileService(ServiceAst service) {
        Node endpoint = termResolver.toNode(service.endpoint());
        Exp endpointNode = Exp.create(Type.NODE, endpoint);
        Query body = Query.create(compile(service.pattern()));
        body.setService(true);
        body.setSilent(service.silent());
        Exp exp = Exp.create(Type.SERVICE, endpointNode, body);
        exp.setSilent(service.silent());
        return exp;
    }

    /** Compiles a nested SELECT into the runtime query expression used by KGRAM. */
    private Exp compileSubQuery(SubQueryAst subQuery) {
        return new CoreseAstQueryBuilder(this).toNextQuery((SelectQueryAst) subQuery.query());
    }
}
