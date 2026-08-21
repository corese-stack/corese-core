package fr.inria.corese.core.next.query.impl.sparql.io.serializer.support;

import fr.inria.corese.core.next.data.api.term.Value;
import fr.inria.corese.core.next.data.api.io.serializer.option.LineEndingOptions;
import fr.inria.corese.core.next.data.api.support.io.IOConstants;
import fr.inria.corese.core.next.data.api.io.option.IOOptions;
import fr.inria.corese.core.next.query.api.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.data.api.exception.SerializationException;
import fr.inria.corese.core.next.query.api.result.BindingSet;
import fr.inria.corese.core.next.query.api.result.TupleQueryResult;

import java.io.IOException;
import java.io.Writer;

/**
 * Abstract serializer for the two character-separated-values formats for the SPARQL results (CSV and TSV)
 */
public abstract class CharacterSeparatedValuesSerializer implements ResultSerializer {

    private final String separator;
    private final TupleQueryResult results;
    private final IOOptions config;

    protected CharacterSeparatedValuesSerializer(String separator, TupleQueryResult results, IOOptions options) {
        this.separator = separator;
        this.results = results;
        this.config = options;
    }

    protected TupleQueryResult getResults() {
        return this.results;
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        String newLine = IOConstants.NEWLINE;
        if(this.config instanceof LineEndingOptions lnOptions) {
            newLine = lnOptions.getLineEnding();
        }

        // Header
        try {
            writer.write(headerString());
            writer.write(newLine);
        } catch (IOException e) {
            throw new SerializationException("Exception during writing of the " + this.getFormatName() + " header", this.getFormat(), e);
        }

        // Mappings
        int lineCount = 0;
        for (BindingSet mapping : this.results) {
            StringBuilder mappingStringBuilder = new StringBuilder();
            int bindingNumber = 0;
            for(String bindingName: this.results.getBindingNames()) {
                if(mapping.hasBinding(bindingName)) {
                    mappingStringBuilder.append(valuetoString(mapping.getValue(bindingName)));
                }
                if(bindingNumber < this.results.getBindingNames().size()-1) {
                    mappingStringBuilder.append(separator);
                }
                bindingNumber++;
            }
            try {
                writer.write(mappingStringBuilder.toString());
                writer.write(newLine);
            } catch (IOException e) {
                throw new SerializationException("Exception during writing of the " + lineCount + " line", this.getFormat(), e);
            }

            lineCount++;
        }

    }

    /**
     * Returns the String representation of RDF terms for each format (e.g. TSV is closer to turtle, CSV returns almost raw string value)
     * @param value The RDF term value of a binding
     * @return the String representation of the RDF term value
     */
    protected abstract String valuetoString(Value value);

    protected String headerString() {
        return String.join(this.separator, this.results.getBindingNames());
    }

}
