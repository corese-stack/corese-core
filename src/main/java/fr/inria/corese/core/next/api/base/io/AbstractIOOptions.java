package fr.inria.corese.core.next.api.base.io;

import fr.inria.corese.core.next.api.io.IOOptions;

/**
 * Abstract class for parser/serializers that sets up the necessity of implementing a builder pattern.
 */
public abstract  class AbstractIOOptions implements IOOptions {

     public abstract static class Builder< T extends IOOptions> {
        protected Builder() {

        }

        public abstract T build();
    }
}
