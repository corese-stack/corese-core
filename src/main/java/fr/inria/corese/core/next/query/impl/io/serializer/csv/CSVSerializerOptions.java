package fr.inria.corese.core.next.query.impl.io.serializer.csv;

import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.CharacterSeparatedValuesSerializerOptions;

public class CSVSerializerOptions extends CharacterSeparatedValuesSerializerOptions {
    protected CSVSerializerOptions(CSVSerializerOptions.Builder builder) {
        super(builder);
    }

    public static class Builder extends CharacterSeparatedValuesSerializerOptions.AbstractBuilder<Builder> {
        @Override
        public CSVSerializerOptions build() {
            return new CSVSerializerOptions(this);
        }
    }
}
