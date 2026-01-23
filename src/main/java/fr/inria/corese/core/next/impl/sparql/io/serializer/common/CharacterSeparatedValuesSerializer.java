package fr.inria.corese.core.next.impl.sparql.io.serializer.common;

import fr.inria.corese.core.kgram.core.Mappings;
import fr.inria.corese.core.next.api.io.IOOptions;
import fr.inria.corese.core.next.api.sparql.io.serializer.ResultSerializer;
import fr.inria.corese.core.next.impl.exception.SerializationException;

import java.io.Writer;

public abstract class CharacterSeparatedValuesSerializer implements ResultSerializer {

    private char separator;
    private Mappings results;
    private IOOptions config;

    protected CharacterSeparatedValuesSerializer(char separator, Mappings results, IOOptions options) {
        this.separator = separator;
        this.results = results;
        this.config = options;
    }

    @Override
    public void write(Writer writer) throws SerializationException {

    }

}
