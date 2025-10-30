package fr.inria.corese.core.next.impl.io.parser.nquads;

import fr.inria.corese.core.next.api.base.io.AbstractIOOptions;

/**
 * Options used to configure a NQuads parser
 */
public class NQuadsParserOptions extends AbstractIOOptions {

    private final NQuadsParserOptions.Builder builder;

    protected NQuadsParserOptions(NQuadsParserOptions.Builder builder) {
        this.builder = builder;
    }

    public static class Builder extends AbstractIOOptions.Builder<NQuadsParserOptions> {

        @Override
        public NQuadsParserOptions build() {
            return new NQuadsParserOptions(this);
        }
    }
}
