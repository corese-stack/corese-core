package fr.inria.corese.core.next.impl.query.io.serializer.csv;

import fr.inria.corese.core.next.impl.query.io.serializer.common.CharacterSeparatedValuesSerializerOptions;

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
