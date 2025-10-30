package fr.inria.corese.core.next.impl.io.parser.ntriples;

import fr.inria.corese.core.next.api.base.io.AbstractIOOptions;

/**
 * Options used to configure a NTriples parser
 */
public class NTriplesParserOptions extends AbstractIOOptions {

    private final NTriplesParserOptions.Builder builder;

    protected NTriplesParserOptions(NTriplesParserOptions.Builder builder) {
        this.builder = builder;
    }

    public static class Builder extends AbstractIOOptions.Builder<NTriplesParserOptions> {

        @Override
        public NTriplesParserOptions build() {
            return new NTriplesParserOptions(this);
        }
    }
}

