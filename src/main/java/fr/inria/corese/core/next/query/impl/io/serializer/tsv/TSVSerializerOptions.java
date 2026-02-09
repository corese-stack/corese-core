package fr.inria.corese.core.next.query.impl.io.serializer.tsv;

import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.CharacterSeparatedValuesSerializerOptions;

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
