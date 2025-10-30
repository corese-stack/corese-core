package fr.inria.corese.core.next.impl.io.parser.trig;

import fr.inria.corese.core.next.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.api.io.common.BaseIRIOptions;
import fr.inria.corese.core.next.impl.io.parser.util.ParserConstants;

public class TriGParserOptions  extends AbstractIOOptions implements BaseIRIOptions {

    private final TriGParserOptions.Builder builder;
    private final String baseIRI;

    protected TriGParserOptions(TriGParserOptions.Builder builder) {
        this.builder = builder;
        this.baseIRI = this.builder.baseIRI;
    }

    @Override
    public String getBaseIRI() {
        return this.baseIRI;
    }

    public static class Builder extends AbstractIOOptions.Builder<TriGParserOptions> {

        protected String baseIRI = ParserConstants.getDefaultBaseURI();

        @Override
        public TriGParserOptions build() {
            return new TriGParserOptions(this);
        }

        public TriGParserOptions.Builder baseIRI(String baseIRI) {
            this.baseIRI = baseIRI;
            return this;
        }

    }

}