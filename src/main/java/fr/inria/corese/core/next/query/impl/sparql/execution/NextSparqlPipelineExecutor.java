package fr.inria.corese.core.next.query.impl.sparql.execution;

import fr.inria.corese.core.next.data.api.IRI;
import fr.inria.corese.core.next.data.api.Resource;
import fr.inria.corese.core.next.data.api.Statement;
import fr.inria.corese.core.next.data.api.Value;
import fr.inria.corese.core.next.data.api.ValueFactory;
import fr.inria.corese.core.next.data.impl.temp.CoreseAdaptedValueFactory;
import fr.inria.corese.core.next.query.impl.parser.SparqlParser;
import fr.inria.corese.core.next.query.impl.result.CoreseGraphQueryResult;
import fr.inria.corese.core.next.query.impl.result.CoreseTupleQueryResult;
import fr.inria.corese.core.next.query.impl.sparql.ast.AskQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ConstructQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DeleteDataRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.DescribeQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.InsertDataRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.IriAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.LiteralAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.QueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.ProjectionAsts;
import fr.inria.corese.core.next.query.impl.sparql.ast.SelectQueryAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TermAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.TriplePatternAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.UpdateRequestUnitAst;
import fr.inria.corese.core.next.query.impl.sparql.ast.VarAst;
import fr.inria.corese.core.next.query.impl.sparql.bridge.CoreseAstQueryBuilder;
import fr.inria.corese.core.next.query.kgram.api.core.Edge;
import fr.inria.corese.core.next.query.kgram.api.core.Node;
import fr.inria.corese.core.next.query.kgram.api.query.Environment;
import fr.inria.corese.core.next.query.kgram.api.query.Evaluator;
import fr.inria.corese.core.next.query.kgram.api.query.Matcher;
import fr.inria.corese.core.next.query.kgram.api.query.ProcessVisitor;
import fr.inria.corese.core.next.query.kgram.api.query.Producer;
import fr.inria.corese.core.next.query.kgram.core.Eval;
import fr.inria.corese.core.next.query.kgram.core.Mapping;
import fr.inria.corese.core.next.query.kgram.core.Mappings;
import fr.inria.corese.core.next.query.kgram.core.Query;
import fr.inria.corese.core.next.query.kgram.core.SparqlException;
import fr.inria.corese.core.next.query.kgram.tool.StorageManagerProducer;
import fr.inria.corese.core.next.query.api.exception.QueryEvaluationException;
import fr.inria.corese.core.next.query.api.exception.UnsupportedQueryFeatureException;
import fr.inria.corese.core.next.query.api.result.GraphQueryResult;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;
import fr.inria.corese.core.next.storagemanager.api.StorageManager;
import fr.inria.corese.core.next.storagemanager.api.support.model.MutationResult;
import fr.inria.corese.core.next.storagemanager.api.support.model.StatementPattern;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Internal integration executor for the autonomous next SPARQL pipeline.
 *
 * <p>This class is deliberately not the final public SPARQL API. It exists for
 * #473 to exercise the complete next-only path from a SPARQL string to results:</p>
 *
 * <pre>
 * SPARQL string -> next parser -> next AST -> next KGRAM -> StorageManagerProducer -> StorageManager
 * </pre>
 *
 * <p>It is acceptable for this class to gather parsing, runtime creation,
 * graph materialization, and minimal update execution while the autonomous path
 * is being proven. Those responsibilities should be split into dedicated
 * services before this becomes a stable user-facing query API.</p>
 *
 * <p>UPDATE support is intentionally minimal for #473: only concrete {@code INSERT DATA}
 * and {@code DELETE DATA} triples with explicit absolute IRIs are accepted. Unsupported
 * SPARQL 1.1 forms fail explicitly instead of falling back to the legacy engine.</p>
 */
public final class NextSparqlPipelineExecutor {

    private final StorageManager storage;
    private final SparqlParser parser;
    private final CoreseAstQueryBuilder queryBuilder;
    private final ValueFactory valueFactory;

    public NextSparqlPipelineExecutor(StorageManager storage) {
        this(storage, new SparqlParser(), new CoreseAstQueryBuilder(), new CoreseAdaptedValueFactory());
    }

    NextSparqlPipelineExecutor(
            StorageManager storage,
            SparqlParser parser,
            CoreseAstQueryBuilder queryBuilder,
            ValueFactory valueFactory) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.queryBuilder = Objects.requireNonNull(queryBuilder, "queryBuilder");
        this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
    }

    public TupleQueryResult evaluateTuple(String sparql) {
        QueryAst ast = parser.parse(sparql);
        if (!(ast instanceof SelectQueryAst select)) {
            throw new UnsupportedQueryFeatureException("Expected SELECT query, got: " + ast.getClass().getSimpleName());
        }
        return new CoreseTupleQueryResult(evaluate(queryBuilder.toNextQuery(select)));
    }

    public boolean evaluateBoolean(String sparql) {
        QueryAst ast = parser.parse(sparql);
        if (!(ast instanceof AskQueryAst ask)) {
            throw new UnsupportedQueryFeatureException("Expected ASK query, got: " + ast.getClass().getSimpleName());
        }
        return evaluate(queryBuilder.toNextQuery(ask)).size() > 0;
    }

    public GraphQueryResult evaluateGraph(String sparql) {
        QueryAst ast = parser.parse(sparql);
        return switch (ast) {
            case ConstructQueryAst construct -> {
                Query query = queryBuilder.toNextQuery(construct);
                yield new CoreseGraphQueryResult(materialize(query, evaluate(query)).iterator());
            }
            case DescribeQueryAst describe -> new CoreseGraphQueryResult(describe(describe).iterator());
            default -> throw new UnsupportedQueryFeatureException(
                    "Expected CONSTRUCT or DESCRIBE query, got: " + ast.getClass().getSimpleName());
        };
    }

    public void executeUpdate(String sparql) {
        QueryAst ast = parser.parse(sparql);
        if (!(ast instanceof UpdateRequestAst update)) {
            throw new UnsupportedQueryFeatureException("Expected SPARQL UPDATE request, got: " + ast.getClass().getSimpleName());
        }
        for (UpdateRequestUnitAst operation : update.operations()) {
            executeOperation(operation);
        }
    }

    private Mappings evaluate(Query query) {
        try {
            Eval eval = Eval.create(new StorageManagerProducer(storage), new NoOpEvaluator(), new BasicMatcher());
            eval.setVisitor(new ProcessVisitor() {
            });
            return eval.query(query);
        } catch (SparqlException e) {
            throw new QueryEvaluationException("Failed to evaluate query with the next pipeline: " + e.getMessage(), e);
        }
    }

    private List<Statement> materialize(Query query, Mappings mappings) {
        if (query.getConstruct() == null) {
            throw new UnsupportedQueryFeatureException("Graph query has no construct template to materialize");
        }
        List<Statement> statements = new ArrayList<>();
        for (Mapping mapping : mappings) {
            for (var template : query.getConstruct()) {
                if (!template.isEdge()) {
                    throw new UnsupportedQueryFeatureException("Only EDGE expressions are supported in graph templates");
                }
                Statement statement = statementFromTemplate(template.getEdge(), mapping);
                if (statement != null && !statements.contains(statement)) {
                    statements.add(statement);
                }
            }
        }
        return statements;
    }

    private List<Statement> describe(DescribeQueryAst describe) {
        SelectQueryAst select = new SelectQueryAst(
                ProjectionAsts.selectAll(),
                describe.datasetClause(),
                describe.whereClause(),
                describe.solutionModifier(),
                describe.prologue(),
                describe.valuesClause());
        Mappings mappings = evaluate(queryBuilder.toNextQuery(select));
        List<Resource> resources = describedResources(describe, mappings);
        List<Statement> statements = new ArrayList<>();
        for (Resource resource : resources) {
            try (var outgoing = storage.getQueryOperations().query(StatementPattern.of(resource, null, null))) {
                outgoing.filter(statement -> !statements.contains(statement)).forEach(statements::add);
            }
            try (var incoming = storage.getQueryOperations().query(StatementPattern.of(null, null, resource))) {
                incoming.filter(statement -> !statements.contains(statement)).forEach(statements::add);
            }
        }
        return statements;
    }

    private List<Resource> describedResources(DescribeQueryAst describe, Mappings mappings) {
        List<Resource> resources = new ArrayList<>();
        for (Mapping mapping : mappings) {
            if (describe.isDescribeAll()) {
                for (String variable : mapping.getVariableNames()) {
                    addResource(resources, nullableValue(mapping.getNode(variable)));
                }
            } else {
                for (TermAst term : describe.described()) {
                    if (term instanceof VarAst(String variableName)) {
                        addResource(resources, nullableValue(mapping.getNode(variableName)));
                    } else {
                        addResource(resources, termValue(term));
                    }
                }
            }
        }
        return resources;
    }

    private void addResource(List<Resource> resources, Value value) {
        if (value instanceof Resource resource && !resources.contains(resource)) {
            resources.add(resource);
        }
    }

    private Statement statementFromTemplate(Edge edge, Mapping mapping) {
        Value subjectValue = nullableValue(resolve(edge.getNode(0), mapping));
        Value predicateValue = nullableValue(resolve(predicateNode(edge), mapping));
        Value object = nullableValue(resolve(edge.getNode(1), mapping));
        if (subjectValue == null || predicateValue == null || object == null) {
            return null;
        }
        Resource subject = asResource(subjectValue, "subject");
        IRI predicate = asIri(predicateValue, "predicate");
        return valueFactory.createStatement(subject, predicate, object);
    }

    private Node resolve(Node node, Mapping mapping) {
        if (node != null && node.isVariable()) {
            return mapping.getNode(node);
        }
        return node;
    }

    private void executeOperation(UpdateRequestUnitAst operation) {
        switch (operation) {
            case InsertDataRequestAst(List<TriplePatternAst> triples) -> {
                for (TriplePatternAst triple : triples) {
                    checkMutation(storage.getMutationOperations().insertStatement(statementFromAst(triple)));
                }
            }
            case DeleteDataRequestAst(List<TriplePatternAst> triples) -> {
                for (TriplePatternAst triple : triples) {
                    checkMutation(storage.getMutationOperations().deleteStatement(statementFromAst(triple)));
                }
            }
            default -> throw new UnsupportedQueryFeatureException(
                    "SPARQL update operation is not supported yet by the next pipeline: "
                            + operation.getClass().getSimpleName());
        }
    }

    private Statement statementFromAst(TriplePatternAst triple) {
        Resource subject = asResource(termValue(triple.subject()), "subject");
        IRI predicate = asIri(termValue(triple.predicate()), "predicate");
        Value object = termValue(triple.object());
        return valueFactory.createStatement(subject, predicate, object);
    }

    private Value termValue(TermAst term) {
        return switch (term) {
            case VarAst(String variableName) -> throw new UnsupportedQueryFeatureException(
                    "Variables are not allowed in INSERT DATA / DELETE DATA: ?" + variableName);
            case IriAst(String rawIri) -> valueFactory.createIRI(normalizeExplicitIri(rawIri, "IRI term"));
            case LiteralAst(String lexical, String language, String datatype) -> {
                String label = unquote(lexical);
                if (language != null && !language.isBlank()) {
                    yield valueFactory.createLiteral(label, language);
                }
                if (datatype != null && !datatype.isBlank()) {
                    yield valueFactory.createLiteral(label, valueFactory.createIRI(normalizeDatatypeIri(datatype)));
                }
                yield valueFactory.createLiteral(label);
            }
            default -> throw new UnsupportedQueryFeatureException(
                    "Unsupported RDF term in update data: " + term.getClass().getSimpleName());
        };
    }

    private void checkMutation(MutationResult result) {
        if (result.isFailure()) {
            throw new QueryEvaluationException("Storage mutation failed: " + result.getMessage());
        }
    }

    private Resource asResource(Value value, String role) {
        if (value instanceof Resource resource) {
            return resource;
        }
        throw new QueryEvaluationException("Constructed " + role + " is not an RDF resource");
    }

    private IRI asIri(Value value, String role) {
        if (value instanceof IRI iri) {
            return iri;
        }
        throw new QueryEvaluationException("Constructed " + role + " is not an IRI");
    }

    private Value nullableValue(Node node) {
        if (node == null) {
            return null;
        }
        IDatatype datatype = node.getDatatypeValue();
        if (datatype.isURI()) {
            return valueFactory.createIRI(datatype.getLabel());
        }
        if (datatype.isBlank()) {
            return valueFactory.createBNode(datatype.getLabel());
        }
        if (datatype.isLiteral()) {
            if (datatype.getLang() != null && !datatype.getLang().isBlank()) {
                return valueFactory.createLiteral(datatype.getLabel(), datatype.getLang());
            }
            if (datatype.getDatatypeURI() != null && !datatype.getDatatypeURI().isBlank()) {
                return valueFactory.createLiteral(datatype.getLabel(), valueFactory.createIRI(datatype.getDatatypeURI()));
            }
            return valueFactory.createLiteral(datatype.getLabel());
        }
        throw new QueryEvaluationException("Unsupported KGRAM node datatype: " + datatype);
    }

    private static Node predicateNode(Edge edge) {
        return edge.getEdgeVariable() == null ? edge.getEdgeNode() : edge.getEdgeVariable();
    }

    private static String normalizeExplicitIri(String raw, String role) {
        if (raw.startsWith("<") && raw.endsWith(">")) {
            String iri = raw.substring(1, raw.length() - 1);
            if (isAbsoluteIri(iri)) {
                return iri;
            }
            throw new UnsupportedQueryFeatureException(role + " must be absolute, got: " + raw);
        }
        throw new UnsupportedQueryFeatureException(
                role + " must be an explicit absolute IRI between <...>; prefixed names are not resolved here: " + raw);
    }

    private static String normalizeDatatypeIri(String raw) {
        if (raw.startsWith("<") && raw.endsWith(">")) {
            String iri = raw.substring(1, raw.length() - 1);
            if (isAbsoluteIri(iri)) {
                return iri;
            }
            throw new UnsupportedQueryFeatureException("Literal datatype must be absolute, got: " + raw);
        }
        if (isAbsoluteIri(raw)) {
            return raw;
        }
        throw new UnsupportedQueryFeatureException(
                "Literal datatype must be an explicit absolute IRI; prefixed datatypes are not resolved here: " + raw);
    }

    private static boolean isAbsoluteIri(String iri) {
        return iri.startsWith("http://") || iri.startsWith("https://") || iri.startsWith("urn:");
    }

    private static String unquote(String lexical) {
        if (lexical.length() >= 2 && lexical.startsWith("\"") && lexical.endsWith("\"")) {
            String unquoted = lexical.substring(1, lexical.length() - 1);
            if (unquoted.contains("\\")) {
                throw new UnsupportedQueryFeatureException(
                        "Escaped string literals are not supported yet by the minimal next update executor");
            }
            return unquoted;
        }
        if (lexical.contains("\\")) {
            throw new UnsupportedQueryFeatureException(
                    "Escaped string literals are not supported yet by the minimal next update executor");
        }
        return lexical;
    }

    private static final class BasicMatcher implements Matcher {
        private int mode = Matcher.UNDEF;

        @Override
        public boolean match(Edge query, Edge target, Environment environment) {
            return match(query.getNode(0), target.getNode(0), environment)
                    && match(query.getNode(1), target.getNode(1), environment)
                    && match(predicateNode(query), target.getEdgeNode(), environment);
        }

        @Override
        public boolean match(Node query, Node target, Environment environment) {
            if (query == null || target == null) {
                return query == target;
            }
            if (query.isVariable()) {
                Node bound = environment == null ? null : environment.getNode(query);
                return bound == null || bound.match(target);
            }
            return query.match(target);
        }

        @Override
        public boolean same(Node queryNode, Node left, Node right, Environment environment) {
            return left != null && left.same(right);
        }

        @Override
        public int getMode() {
            return mode;
        }

        @Override
        public void setMode(int mode) {
            this.mode = mode;
        }
    }

    private static final class NoOpEvaluator implements Evaluator {
        private Mode mode = Mode.KGRAM_MODE;

        @Override
        public Mode getMode() {
            return mode;
        }

        @Override
        public void setMode(Mode mode) {
            this.mode = mode;
        }

        @Override
        public void setProducer(Producer producer) {
            // No producer state is needed by this minimal evaluator.
        }

        @Override
        public void setKGRAM(Eval eval) {
            // The executor drives Eval directly, so there is no evaluator-side KGRAM state to keep.
        }

        @Override
        public void start(Environment environment) {
            // No per-query initialization is needed for the supported #473 query subset.
        }

        @Override
        public void finish(Environment environment) {
            // No per-query cleanup is needed for the supported #473 query subset.
        }

        @Override
        public void init(Environment environment) {
            // No per-environment initialization is needed by this no-op evaluator.
        }
    }
}
