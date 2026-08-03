package fr.inria.corese.core.next.query.kgram.api.query;

import fr.inria.corese.core.next.query.kgram.core.BgpGenerator;

/**
 * @author corby
 */
public interface DistributedQueryPlanFactory {

    BgpGenerator instance();

}
