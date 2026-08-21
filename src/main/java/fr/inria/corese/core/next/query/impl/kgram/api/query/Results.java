package fr.inria.corese.core.next.query.impl.kgram.api.query;

import fr.inria.corese.core.next.query.impl.kgram.api.core.Node;

import java.util.List;

/**
 * Interface to KGRAM  results (mappings)
 *
 * @author corby
 */
public interface Results extends Iterable<Result> {

    List<Node> getSelect();

    int size();

}
