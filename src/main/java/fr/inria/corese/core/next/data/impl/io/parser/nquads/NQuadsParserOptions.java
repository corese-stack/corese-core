package fr.inria.corese.core.next.data.impl.io.parser.nquads;

import fr.inria.corese.core.next.data.api.base.io.AbstractIOOptions;

/**
 * Options used to configure a NQuads parser
 */
public class NQuadsParserOptions extends AbstractIOOptions {


    protected NQuadsParserOptions() {
    }

    public static class Builder extends AbstractIOOptions.Builder<NQuadsParserOptions> {

        @Override
        public NQuadsParserOptions build() {
            return new NQuadsParserOptions();
        }
    }
}
