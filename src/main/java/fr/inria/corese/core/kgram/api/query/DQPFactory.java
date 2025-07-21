package fr.inria.corese.core.kgram.api.query;

import fr.inria.corese.core.kgram.core.BgpGenerator;

/**
 *
 * @author corby
 */
public interface DQPFactory {
    
    BgpGenerator instance();
    
}
