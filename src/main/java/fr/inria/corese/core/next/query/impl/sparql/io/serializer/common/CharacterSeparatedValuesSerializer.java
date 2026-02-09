package fr.inria.corese.core.next.query.impl.sparql.io.serializer.common;

import fr.inria.corese.core.next.api.io.serializer.LineEndingOptions;
import fr.inria.corese.core.next.impl.io.serialization.util.SerializationConstants;
import fr.inria.corese.core.next.kgram.api.core.Node;
import fr.inria.corese.core.next.kgram.core.Mapping;
import fr.inria.corese.core.next.kgram.core.Mappings;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.query.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.impl.exception.SerializationException;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CharacterSeparatedValuesSerializer implements ResultSerializer {

    private final String separator;
    private final Mappings results;
    private final IOOptions config;

    protected CharacterSeparatedValuesSerializer(String separator, Mappings results, IOOptions options) {
        this.separator = separator;
        this.results = results;
        this.config = options;
    }

    @Override
    public void write(Writer writer) throws SerializationException {
        List<Node> selectVariables = this.results.getSelect();
        String newLine = SerializationConstants.NEWLINE;
        if(this.config instanceof LineEndingOptions lnOptions) {
            newLine = lnOptions.getLineEnding();
        }

        // Header
        String headerString = selectVariables.stream().map(Node::getLabel).collect(Collectors.joining(separator));
        try {
            writer.write(headerString);
            writer.write(newLine);
        } catch (IOException e) {
            throw new SerializationException("Exception during writing of the " + this.getFormatName() + " header", this.getFormat(), 0, -1, e);
        }

        // Mappings
        int lineCount = 0;
        for (Mapping mapping : this.results) {
            String mappingString = mapping.getMap().keySet().stream().map(this::escapeCharacter).collect(Collectors.joining(separator));
            try {
                writer.write(mappingString);
                writer.write(newLine);
            } catch (IOException e) {
                throw new SerializationException("Exception during writing of the " + this.getFormatName() + " line", this.getFormat(), lineCount, -1, e);
            }

            lineCount++;
        }

    }

    private String escapeCharacter(String svCell) {
        svCell = svCell.replaceAll(this.separator, "\\" + this.separator);
        return svCell;
    }

}
