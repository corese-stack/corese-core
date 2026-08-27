package fr.inria.corese.core.next.data.impl.adapter;

import fr.inria.corese.core.kgram.api.core.Edge;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.spi.model.AbstractStatement;

/**
 * Represents a statement in Corese. A Corese statement consists of a subject,
 * predicate, object, and an optional context.
 */
@SuppressWarnings("java:S2160")
public class CoreseStatement extends AbstractStatement {

    private static final CoreseValueConverter CONVERTER = new CoreseValueConverter();

    private final Resource subject;
    private final IRI predicate;
    private final Value object;
    private final Resource context;

    /**
     * Constructs a {@link CoreseStatement} from a subject, predicate, object, and context.
     *
     * @param subject   the subject of the statement (non-null)
     * @param predicate the predicate of the statement (non-null)
     * @param object    the object of the statement (non-null)
     * @param context   the context (or graph) of the statement (can be null)
     */
    public CoreseStatement(Resource subject, IRI predicate, Value object, Resource context) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.context = context;
    }

    /**
     * Constructs a {@link CoreseStatement} from an existing {@link Edge}.
     * This constructor extracts the subject, predicate, object, and context from
     * the provided {@link Edge} and initializes the fields of this statement accordingly.
     *
     * @param edge the existing {@link Edge} object that represents the statement in
     *             the Corese API (non-null)
     */
    public CoreseStatement(Edge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("Edge cannot be null");
        }

        Resource subjectValue = (Resource) CONVERTER.fromCoreseNode(edge.getSubjectNode());
        IRI predicateValue = (IRI) CONVERTER.fromCoreseNode(edge.getPropertyNode());
        Value objectValue = CONVERTER.fromCoreseNode(edge.getObjectNode());
        Resource contextValue = CONVERTER.fromCoreseContext(edge.getGraph());

        this.subject = subjectValue;
        this.predicate = predicateValue;
        this.object = objectValue;
        this.context = contextValue;
    }

    @Override
    public Resource getSubject() {
        return this.subject;
    }

    @Override
    public IRI getPredicate() {
        return this.predicate;
    }

    @Override
    public Value getObject() {
        return this.object;
    }

    @Override
    public Resource getContext() {
        return this.context;
    }
}
