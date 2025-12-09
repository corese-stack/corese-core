package fr.inria.corese.core.next.api.result;

import fr.inria.corese.core.next.api.Triple;

import java.io.Closeable;

public interface GraphQueryResult extends Closeable {

    /**
     * Returns {@code true} if at least one more triple is available from the result.
     *
     * @return {@code true} if another triple can be retrieved
     */
    boolean hasNext();

    /**
     * Returns the next triple in the result sequence.
     * <p>
     * If no next triple exists, this method may throw an Exception.
     *
     * @return the next {@link Triple} in the result
     */
    Triple next();

    /**
     * Closes the result and releases any underlying resources such as
     * I/O streams, network connections, or database cursors.
     * <p>
     * Once closed, further iteration is not permitted.
     */
    @Override
    void close();

}