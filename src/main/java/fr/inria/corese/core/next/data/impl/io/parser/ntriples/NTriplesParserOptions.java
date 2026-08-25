package fr.inria.corese.core.next.data.impl.io.parser.ntriples;

import fr.inria.corese.core.next.data.api.support.io.AbstractIOOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;

/**
 * Options used to configure a NTriples parser
 */
public class NTriplesParserOptions extends AbstractIOOptions implements RDFParsingOptions {

    protected NTriplesParserOptions() {

    }

    public static class Builder extends AbstractIOOptions.Builder<NTriplesParserOptions> {

        @Override
        public NTriplesParserOptions build() {
            return new NTriplesParserOptions();
        }
    }
}
