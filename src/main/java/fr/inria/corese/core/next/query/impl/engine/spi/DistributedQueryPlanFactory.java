package fr.inria.corese.core.next.query.impl.engine.spi;

import fr.inria.corese.core.next.query.impl.engine.pattern.BgpGenerator;

/**
 * @author corby
 */
public interface DistributedQueryPlanFactory {

    BgpGenerator instance();

}
