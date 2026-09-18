package fr.inria.corese.core.next.query.impl.engine.storage;

import fr.inria.corese.core.next.data.api.model.DatatypeValue;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.query.impl.engine.model.Edge;
import fr.inria.corese.core.next.query.impl.engine.model.Node;
import fr.inria.corese.core.next.query.impl.engine.spi.Environment;
import fr.inria.corese.core.next.storage.api.model.StatementPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Translates KGRAM query edges and dataset/graph constraints into storage-layer statement patterns.
 */
final class StoragePatternTranslator {

    private StoragePatternTranslator() {}

    /**
     * Statement pattern plus an explicit empty-result marker.
     *
     * <p>This avoids using {@code null} to represent impossible RDF patterns while still
     * keeping a non-null placeholder pattern for the record state.</p>
     */
    record StorageQueryPattern(StatementPattern statementPattern, boolean noMatch) {

        static StorageQueryPattern of(StatementPattern statementPattern) {
            return new StorageQueryPattern(statementPattern, false);
        }

        static StorageQueryPattern emptyResult() {
            return new StorageQueryPattern(StatementPattern.matchAll(), true);
        }
    }

    /**
     * Storage contexts selected for a query pattern.
     *
     * <p>An empty context list means "all contexts" for the storage API; {@code noMatch}
     * distinguishes this from a graph/dataset restriction that cannot produce results.</p>
     */
    record ContextSelection(List<Resource> contexts, boolean noMatch) {

        static ContextSelection of(List<Resource> contexts) {
            return new ContextSelection(List.copyOf(contexts), false);
        }

        static ContextSelection allContexts() {
            return of(List.of());
        }

        static ContextSelection emptyResult() {
            return new ContextSelection(List.of(), true);
        }

        Resource[] contextsArray() {
            return contexts.toArray(Resource[]::new);
        }
    }

    /**
     * Converts a KGRAM query edge into the storage-layer statement pattern.
     *
     * <p>Unbound KGRAM variables become {@code null} components, which the storage API
     * interprets as wildcards. Impossible RDF combinations, such as a literal subject
     * or predicate, are represented as an empty-result pattern.</p>
     *
     * @param graphNode current GRAPH node, or {@code null} for the default graph context
     * @param from active FROM/FROM NAMED restriction computed by KGRAM
     * @param queryEdge KGRAM triple pattern to translate
     * @param environment current bindings used to resolve already-bound variables
     * @return a storage query pattern, or an empty-result marker when no RDF statement can match
     */
    static StorageQueryPattern translate(
            Node graphNode,
            List<Node> from,
            Edge queryEdge,
            Environment environment) {
        Node subjectNode = resolve(queryEdge.getNode(0), environment);
        Node predicateNode = resolve(predicateQueryNode(queryEdge), environment);
        Node objectNode = resolve(queryEdge.getNode(1), environment);

        Resource subject = null;
        IRI predicate = null;
        Value object = null;

        // Subject and predicate have stricter RDF roles than object: subject must
        // be a resource, predicate must be an IRI, while object accepts any RDF value.
        if (subjectNode != null) {
            Value value = rdfValue(subjectNode);
            if (!(value instanceof Resource resource)) {
                return StorageQueryPattern.emptyResult();
            }
            subject = resource;
        }
        if (predicateNode != null) {
            Value value = rdfValue(predicateNode);
            if (!(value instanceof IRI iri)) {
                return StorageQueryPattern.emptyResult();
            }
            predicate = iri;
        }
        if (objectNode != null) {
            object = rdfValue(objectNode);
        }

        // Graph and dataset clauses become the statement contexts passed to storage.
        ContextSelection contextSelection = contextSelection(graphNode, from, environment);
        if (contextSelection.noMatch()) {
            return StorageQueryPattern.emptyResult();
        }
        return StorageQueryPattern.of(StatementPattern.of(
                subject,
                predicate,
                object,
                contextSelection.contextsArray()));
    }

    /**
     * Returns the effective predicate node carried by a KGRAM edge.
     *
     * <p>KGRAM stores variable predicates in {@link Edge#getEdgeVariable()}; {@link Edge#getEdgeNode()}
     * may only be the technical root-property placeholder for {@code ?s ?p ?o} patterns.</p>
     *
     * @param queryEdge KGRAM edge whose predicate must be read
     * @return the predicate variable when present, otherwise the constant predicate node
     */
    static Node predicateQueryNode(Edge queryEdge) {
        return queryEdge.getEdgeVariable() == null ? queryEdge.getEdgeNode() : queryEdge.getEdgeVariable();
    }

    /**
     * Selects the storage contexts for the active graph pattern.
     *
     * <p>SPARQL evaluates {@code GRAPH <g> { ... }} only when {@code <g>} is a named graph
     * in the active dataset; otherwise the graph pattern has no solution.</p>
     */
    static ContextSelection contextSelection(Node graphNode, List<Node> from, Environment environment) {
        List<Node> activeGraphs = selectActiveGraphs(graphNode, from, environment);
        if (graphNode != null) {
            return selectExplicitGraphContext(graphNode, activeGraphs, environment);
        }
        return selectDatasetContexts(activeGraphs, environment);
    }

    static List<Node> selectActiveGraphs(Node graphNode, List<Node> from, Environment environment) {
        if (isExplicitDataset(environment)) {
            return graphNode == null
                    ? environment.getQuery().getFrom()
                    : environment.getQuery().getNamed();
        }
        return from;
    }

    static ContextSelection selectExplicitGraphContext(
            Node graphNode,
            List<Node> activeGraphs,
            Environment environment) {
        Node resolvedGraphNode = resolve(graphNode, environment);
        if (resolvedGraphNode == null) {
            return isExplicitDataset(environment) && (activeGraphs == null || activeGraphs.isEmpty())
                    ? ContextSelection.emptyResult()
                    : ContextSelection.allContexts();
        }
        Value value = rdfValue(resolvedGraphNode);
        if (!(value instanceof Resource resource)) {
            return ContextSelection.emptyResult();
        }
        if (!matchesFrom(resolvedGraphNode, activeGraphs, environment)) {
            return ContextSelection.emptyResult();
        }
        return ContextSelection.of(List.of(resource));
    }

    static ContextSelection selectDatasetContexts(List<Node> activeGraphs, Environment environment) {
        if (activeGraphs == null || activeGraphs.isEmpty()) {
            return isExplicitDataset(environment)
                    ? ContextSelection.emptyResult()
                    : ContextSelection.allContexts();
        }

        List<Resource> contexts = new ArrayList<>();
        for (Node node : activeGraphs) {
            Node resolvedNode = resolve(node, environment);
            if (resolvedNode == null) {
                continue;
            }
            Value value = rdfValue(resolvedNode);
            if (!(value instanceof Resource resource)) {
                return ContextSelection.emptyResult();
            }
            contexts.add(resource);
        }
        return ContextSelection.of(contexts);
    }

    /**
     * Checks whether a resolved graph node is allowed by the active dataset restriction.
     *
     * @param graphNode resolved graph node to test
     * @param from active FROM/FROM NAMED restriction computed by KGRAM
     * @param environment current bindings used to resolve graph variables in {@code from}
     * @return {@code true} when no restriction exists or when {@code graphNode} belongs to it
     */
    static boolean matchesFrom(Node graphNode, List<Node> from, Environment environment) {
        if (from == null || from.isEmpty()) {
            return !isExplicitDataset(environment);
        }
        for (Node fromNode : from) {
            Node resolvedNode = resolve(fromNode, environment);
            if (resolvedNode != null && resolvedNode.match(graphNode)) {
                return true;
            }
        }
        return false;
    }

    static boolean isExplicitDataset(Environment environment) {
        return environment != null
                && environment.getQuery() != null
                && environment.getQuery().isDatasetSpecified();
    }

    /**
     * Resolves a query node against the current KGRAM environment.
     *
     * <p>Constants resolve to themselves, bound variables resolve to their current target node,
     * and unbound variables resolve to {@code null}.</p>
     *
     * @param queryNode KGRAM query node to resolve
     * @param environment current bindings, or {@code null}
     * @return resolved node, or {@code null} when the node is unbound
     */
    static Node resolve(Node queryNode, Environment environment) {
        if (queryNode == null) {
            return null;
        }
        if (queryNode.isConstant()) {
            return queryNode;
        }
        return environment == null ? null : environment.getNode(queryNode);
    }

    static Value rdfValue(Node node) {
        DatatypeValue value = Objects.requireNonNull(node, "node").getDatatypeValue();
        if (value instanceof Value rdfValue) {
            return rdfValue;
        }
        throw new IllegalArgumentException("KGRAM node does not carry an RDF value: " + node);
    }
}
