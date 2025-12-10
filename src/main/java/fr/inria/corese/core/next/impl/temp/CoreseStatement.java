package fr.inria.corese.core.next.impl.temp;

import fr.inria.corese.core.edge.EdgeImpl;
import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.api.base.model.AbstractStatement;

/**
 * Represents a statement in Corese. A Corese statement consists of a subject,
 * predicate, object,
 * and an optional context. This class provides methods to access these
 * components
 */
public class CoreseStatement extends AbstractStatement implements CoreseEdgeAdapter {

    private final CoreseValueConverter converter;

    private final Edge edge;
    private final Resource subject;
    private final IRI predicate;
    private final Value object;
    private final Resource context;

    /**
     * Constructs a {@link CoreseStatement} from a subject, predicate, object, and
     * context.
     *
     *
     * @param subject   the subject of the statement (non-null)
     * @param predicate the predicate of the statement (non-null)
     * @param object    the object of the statement (non-null)
     * @param context   the context (or graph) of the statement (can be null)
     */
    public CoreseStatement(Resource subject, IRI predicate, Value object, Resource context) {
        this.converter = new CoreseValueConverter();

        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.context = context;

        Node subjectNode = converter.toCoreseNode(subject);
        Node predicateNode = converter.toCoreseNode(predicate);
        Node objectNode = converter.toCoreseNode(object);
        Node contextNode = converter.toCoreseContext(context);

        EdgeImpl edgeImpl = EdgeImpl.create(contextNode, subjectNode, predicateNode, objectNode);
        this.edge = edgeImpl;
    }

    /**
     * Constructs a {@link CoreseStatement} from an existing {@link Edge}.
     * This constructor extracts the subject, predicate, object, and context from
     * the provided
     * {@link Edge} and initializes the fields of this statement accordingly.
     *
     * @param edge the existing {@link Edge} object that represents the statement in
     *             the V4 Corese API (non-null)
     */
    public CoreseStatement(Edge edge) {
        this.converter = new CoreseValueConverter();

        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        Resource subject_corese = (Resource) converter.toRdf4jValue(edge.getSubjectValue());
        IRI predicate_corese = (IRI) converter.toRdf4jValue(edge.getPredicateValue());
        Value object_corese = converter.toRdf4jValue(edge.getObjectValue());
        Resource context_corese = (Resource) converter.toRdf4jValueContext(edge.getGraph());

        this.subject = subject_corese;
        this.predicate = predicate_corese;
        this.object = object_corese;
        this.context = context_corese;
        this.edge = edge;
    }

    /**
     * Returns the subject of the statement.
     *
     * @return the subject of the statement
     */
    @Override
    public Resource getSubject() {
        return this.subject;
    }

    /**
     * Returns the predicate of the statement.
     *
     * @return the predicate of the statement
     */
    @Override
    public IRI getPredicate() {
        return this.predicate;
    }

    /**
     * Returns the object of the statement.
     *
     * @return the object of the statement
     */
    @Override
    public Value getObject() {
        return this.object;
    }

    /**
     * Returns the context (graph) of the statement.
     *
     * @return the context of the statement (may be null if not provided)
     */
    @Override
    public Resource getContext() {
        return this.context;
    }

    /**
     * Returns the underlying Corese {@link Edge} that represents this statement.
     *
     * @return the Corese edge object
     */
    public Edge getCoreseEdge() {
        return this.edge;
    }
}