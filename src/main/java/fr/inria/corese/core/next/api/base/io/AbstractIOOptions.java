package fr.inria.corese.core.next.api.base.io;

import fr.inria.corese.core.next.api.io.IOOptions;

public abstract  class AbstractIOOptions implements IOOptions {

     public abstract static class Builder< T extends IOOptions> {
        protected Builder() {

        }

        public abstract T build();
    }
}
