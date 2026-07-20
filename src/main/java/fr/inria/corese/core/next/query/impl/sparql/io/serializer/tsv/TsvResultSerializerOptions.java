package fr.inria.corese.core.next.query.impl.sparql.io.serializer.tsv;

import fr.inria.corese.core.next.query.impl.sparql.io.serializer.common.CharacterSeparatedValuesSerializerOptions;

public class TsvResultSerializerOptions extends CharacterSeparatedValuesSerializerOptions {
    protected TsvResultSerializerOptions(TsvResultSerializerOptions.Builder builder) {
        super(builder);
    }

    public static class Builder extends CharacterSeparatedValuesSerializerOptions.AbstractBuilder<TsvResultSerializerOptions.Builder> {
        @Override
        public TsvResultSerializerOptions build() {
            return new TsvResultSerializerOptions(this);
        }
    }
}
