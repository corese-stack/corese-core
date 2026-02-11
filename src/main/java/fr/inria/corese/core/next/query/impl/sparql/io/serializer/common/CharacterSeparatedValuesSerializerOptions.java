package fr.inria.corese.core.next.query.impl.sparql.io.serializer.common;

import fr.inria.corese.core.next.data.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.data.api.io.serializer.LineEndingOptions;
import fr.inria.corese.core.next.data.impl.io.serialization.util.SerializationConstants;

public abstract class CharacterSeparatedValuesSerializerOptions extends AbstractIOOptions implements LineEndingOptions {

    private AbstractBuilder<?> builder;

    protected CharacterSeparatedValuesSerializerOptions(AbstractBuilder<?> builder) {
        this.builder = builder;
    }

    public String getLineEnding() {
        return this.builder.lineEnding;
    }

    public static abstract class AbstractBuilder<S extends CharacterSeparatedValuesSerializerOptions.AbstractBuilder<S>> {

        protected String lineEnding = SerializationConstants.DEFAULT_LINE_ENDING;

        public S setLineEnding(String lineEnding) {
            this.lineEnding = lineEnding;
            return self();
        }

        public abstract CharacterSeparatedValuesSerializerOptions build();

        /**
         * Helper method to return the concrete builder instance for fluent API chaining.
         * This method is used internally by the builder methods to ensure that method calls
         * return the correct subclass type, allowing for method chaining.
         *
         * @return The concrete builder instance.
         */
        @SuppressWarnings("unchecked")
        protected final S self() {
            return (S) this;
        }
    }
}
