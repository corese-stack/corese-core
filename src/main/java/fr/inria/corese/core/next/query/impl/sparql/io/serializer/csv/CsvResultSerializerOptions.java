package fr.inria.corese.core.next.query.impl.sparql.io.serializer.csv;

import fr.inria.corese.core.next.query.impl.sparql.io.serializer.support.CharacterSeparatedValuesSerializerOptions;

public class CsvResultSerializerOptions extends CharacterSeparatedValuesSerializerOptions {
    protected CsvResultSerializerOptions(CsvResultSerializerOptions.Builder builder) {
        super(builder);
    }

    public static class Builder extends CharacterSeparatedValuesSerializerOptions.AbstractBuilder<Builder> {
        @Override
        public CsvResultSerializerOptions build() {
            return new CsvResultSerializerOptions(this);
        }
    }
}
