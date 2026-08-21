package fr.inria.corese.core.next.query.impl.kgram.api.query;

import fr.inria.corese.core.next.query.impl.kgram.core.BgpGenerator;

/**
 * @author corby
 */
public interface DistributedQueryPlanFactory {

    BgpGenerator instance();

}
