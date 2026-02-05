package fr.inria.corese.core.next.impl.query.io.serializer.tsv;

import fr.inria.corese.core.next.impl.query.io.serializer.common.CharacterSeparatedValuesSerializerOptions;

public class TSVSerializerOptions extends CharacterSeparatedValuesSerializerOptions {
    protected TSVSerializerOptions(TSVSerializerOptions.Builder builder) {
        super(builder);
    }

    public static class Builder extends CharacterSeparatedValuesSerializerOptions.AbstractBuilder<TSVSerializerOptions.Builder> {
        @Override
        public TSVSerializerOptions build() {
            return new TSVSerializerOptions(this);
        }
    }
}
