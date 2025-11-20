package fr.inria.corese.core.next.impl.io.serialization.turtle;

import fr.inria.corese.core.next.impl.common.vocabulary.OWL;
import fr.inria.corese.core.next.impl.common.vocabulary.RDF;
import fr.inria.corese.core.next.impl.common.vocabulary.RDFS;
import fr.inria.corese.core.next.impl.common.vocabulary.XSD;
import fr.inria.corese.core.next.impl.io.serialization.option.AbstractTFamilyOption;
import fr.inria.corese.core.next.impl.io.serialization.option.BlankNodeStyleEnum;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Turtle serialization format.
 * This class extends {@link AbstractTFamilyOption} and provides specific defaults
 * and options tailored for Turtle, such as using collections and anonymous blank nodes.
 *
 * <p>Use the {@link Builder} class to create instances of {@code TurtleConfig}.
 * A predefined default configuration is available via {@link #defaultConfig()}.</p>
 */
public class TurtleSerializerOptions extends AbstractTFamilyOption {

    /**
     * Protected constructor to be used by the {@link Builder}.
     *
     * @param builder The builder instance containing the desired configuration values.
     */
    protected TurtleSerializerOptions(Builder builder) {
        super(builder);
    }

    /**
     * Public Builder for {@link TurtleSerializerOptions}.
     * Provides a fluent API for constructing {@code TurtleConfig} instances with default values
     * specific to the Turtle format.
     */
    public static class Builder extends AbstractTFamilyOption.AbstractTFamilyBuilder<Builder> {
        /**
         * Default constructor initializes all options with their default values for Turtle.
         */
        public Builder() {
            lineEnding(System.lineSeparator());
            validateURIs(false);
            useCollections(true);
            blankNodeStyle(BlankNodeStyleEnum.ANONYMOUS);

        }

        /**
         * Builds and returns a new {@link TurtleSerializerOptions} instance with the current builder settings.
         *
         * @return A new {@code TurtleConfig} instance.
         */
        @Override
        public TurtleSerializerOptions build() {
            return new TurtleSerializerOptions(this);
        }
    }

    /**
     * Returns a default configuration suitable for Turtle serialization.
     * This provides a convenient way to get a standard Turtle configuration without
     * manually building it.
     *
     * @return A {@code TurtleConfig} instance with default settings.
     */
    public static TurtleSerializerOptions defaultConfig() {
        return new Builder().build();
    }


    /**
     * Returns a new builder instance for {@link TurtleSerializerOptions}.
     * This allows for fluent construction of custom Turtle configurations.
     *
     * @return A new {@code Builder} instance.
     */
    public static TurtleSerializerOptions.Builder builder() {
        return new TurtleSerializerOptions.Builder();
    }
}
