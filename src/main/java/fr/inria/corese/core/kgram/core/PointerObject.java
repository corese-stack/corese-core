package fr.inria.corese.core.kgram.core;

import fr.inria.corese.core.kgram.api.core.Pointerable;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2015
 *
 */
public abstract class PointerObject implements Pointerable {
    
    @Override
    public Object getPointerObject() {
        return this;
    }

}
