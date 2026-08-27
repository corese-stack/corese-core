package fr.inria.corese.core.next.data.impl.io.parser.turtle;

import fr.inria.corese.core.next.data.spi.io.AbstractIOOptions;
import fr.inria.corese.core.next.data.api.io.option.BaseIRIOptions;
import fr.inria.corese.core.next.data.api.io.option.RDFParsingOptions;
import fr.inria.corese.core.next.data.impl.io.parser.support.ParserConstants;

/**
 * Configuration class for the parsing of Turtle documents
 */
public class TurtleParserOptions extends AbstractIOOptions implements BaseIRIOptions, RDFParsingOptions {

    private final TurtleParserOptions.Builder builder;
    private final String baseIRI;

    protected TurtleParserOptions(TurtleParserOptions.Builder builder) {
        this.builder = builder;
        this.baseIRI = this.builder.baseIRI;
    }

    @Override
    public String getBaseIRI() {
        return this.baseIRI;
    }

    public static class Builder extends AbstractIOOptions.Builder<TurtleParserOptions> {

        protected String baseIRI = ParserConstants.getDefaultBaseURI();

        @Override
        public TurtleParserOptions build() {
            return new TurtleParserOptions(this);
        }

        /**
         * Set the base IRI used for relative IRI processing
         * @param baseIRI An IRI
         * @return this
         */
        public TurtleParserOptions.Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        }

    }

}
