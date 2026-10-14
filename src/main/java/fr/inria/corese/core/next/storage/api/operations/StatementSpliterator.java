package fr.inria.corese.core.next.storage.api.operations;

import fr.inria.corese.core.next.data.api.model.Statement;

import java.util.Spliterator;

/**
 * Optional snapshot-bound guarantees for a statement traversal.
 *
 * <p>
 * A storage stream may expose this interface through {@link java.util.stream.Stream#spliterator()}. Wrappers and
 * backends without this interface require conservative handling by callers.
 * </p>
 */
public interface StatementSpliterator extends Spliterator<Statement> {

    /**
     * Whether this traversal contains each RDF subject/predicate/object triple at most once, ignoring its context. The
     * guarantee must stay valid throughout this traversal, including across concurrent mutations of the underlying
     * storage.
     *
     * <p>
     * This is stronger than {@link Spliterator#DISTINCT}, which may only distinguish statements by their contexts. A
     * single RDF graph normally provides this guarantee; a union of graph partitions may not.
     * </p>
     *
     * <p>The guarantee must describe the same captured read version as this traversal.
     * Inspecting it must not consume statements or change snapshot ownership.
     * An implementation may return false when uniqueness cannot be established.</p>
     *
     * @return true only when triple uniqueness is guaranteed for the captured read
     */
    boolean hasDistinctTriples();
}
