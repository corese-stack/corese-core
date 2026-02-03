package fr.inria.corese.core.next.data.api.base.io;

import fr.inria.corese.core.next.data.io.IOOptions;

/**
 * Abstract class for parser/serializers that sets up the necessity of implementing a builder pattern.
 */
public abstract  class AbstractIOOptions implements IOOptions {

    /**
     * Builder class for constructing instances of IOOptions.
     */
     public abstract static class Builder< T extends IOOptions> {
        protected Builder() {

        }

        public abstract T build();
    }
}
