package fr.inria.corese.core.next.data.impl.io.serializer.trig;

import fr.inria.corese.core.next.data.impl.io.serializer.option.AbstractTFamilyOptions;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.data.impl.io.serializer.option.BlankNodeStyle;

/**
 * Configuration for TriG serialization format.
 * This class extends {@link AbstractTFamilyOptions} and provides specific defaults
 * and options tailored for TriG, which extends Turtle with named graphs.
 *
 * <p>Use the {@link Builder} class to create instances of {@code TriGConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class TriGSerializerOptions extends AbstractTFamilyOptions {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected TriGSerializerOptions(Builder builder) {
        super(builder);
    }

    /**
     * Public Builder for {@link TriGSerializerOptions}.
     * Provides a fluent API for constructing {@code TriGConfig} instances with default values
     * specific to the TriG format.
     */
    public static class Builder extends AbstractTFamilyOptions.AbstractTFamilyBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for TriG.
         */
        public Builder() {
            includeContext(true);
            blankNodeStyle(BlankNodeStyle.NAMED);
            useCollections(false);


         }

        /** Creates TriG options from the shared public RDF options. */
        public Builder(IOOptions otherOptions) {
            super(otherOptions);
            includeContext(true);
            blankNodeStyle(BlankNodeStyle.NAMED);
            useCollections(false);
        }

        /**
         * Builds and returns a new {@link TriGSerializerOptions} instance with the current builder settings.
         *
         * @return A new {@code TriGConfig} instance.
         */
        @Override
        public TriGSerializerOptions build() {
            return new TriGSerializerOptions(this);
        }
    }

    /**
     * Returns a default configuration suitable for TriG serialization.
     * This provides a convenient way to get a standard TriG configuration without
     * manually building it.
     *
     * @return A {@code TriGConfig} instance with default settings.
     */
    public static TriGSerializerOptions defaultConfig() {
        return new Builder().build();
    }

    /**
     * Returns a new builder instance for {@link TriGSerializerOptions}.
     * This allows for fluent construction of custom TriG configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static TriGSerializerOptions.Builder builder() {
        return new TriGSerializerOptions.Builder();
    }
}
