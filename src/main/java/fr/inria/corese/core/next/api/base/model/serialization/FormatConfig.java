package fr.inria.corese.core.next.api.base.model.serialization;

import java.util.Objects;

/**
 * Configuration options for the {@link NTriplesFormat} serializer.
 * Use {@link FormatConfig # Builder} to create instances.
 */
public class FormatConfig {

    private final String blankNodePrefix;


    /**
     * Private constructor to enforce usage of the Builder.
     *
     * @param builder The builder instance.
     */
    private FormatConfig(Builder builder) {
        this.blankNodePrefix = builder.blankNodePrefix;

    }

    /**
     * Returns the prefix to use for blank nodes.
     *
     * @return The blank node prefix.
     */
    public String getBlankNodePrefix() {
        return blankNodePrefix;
    }

    /**
     * Builder class for {@link FormatConfig}.
     */
    public static class Builder {

        private String blankNodePrefix = "_:";


        public Builder() {

        }

        /**
         * Sets the prefix to use for blank nodes. Default is "_:".
         *
         * @param blankNodePrefix The desired blank node prefix.
         * @return The builder instance.
         */
        public Builder blankNodePrefix(String blankNodePrefix) {
            this.blankNodePrefix = Objects.requireNonNull(blankNodePrefix, "Blank node prefix cannot be null");
            return this;
        }


        /**
         * Builds a new {@link FormatConfig} instance.
         *
         * @return A new NFormatConfig instance.
         */
        public FormatConfig build() {
            return new FormatConfig(this);
        }
    }
}