package fr.inria.corese.core.next.impl.sparql.io.serializer.common;

import fr.inria.corese.core.next.api.base.io.AbstractIOOptions;
import fr.inria.corese.core.next.api.io.serializer.LineEndingOptions;

public class CharacterSeparatedValuesOptions extends AbstractIOOptions implements LineEndingOptions {

    CharacterSeparatedValuesOptions.Builder builder;

    protected CharacterSeparatedValuesOptions(CharacterSeparatedValuesOptions.Builder builder) {
        this.builder = builder;
    }

    @Override
    public String getLineEnding() {
        return this.builder.lineEnding;
    }

    public abstract static class Builder extends AbstractIOOptions.Builder<CharacterSeparatedValuesOptions> {

        private String lineEnding;

        protected Builder() {
            this.lineEnding = "\n";
        }

        Builder setLineEnding(String ln) {
            this.lineEnding = ln;
            return this;
        }

        @Override
        public CharacterSeparatedValuesOptions build() {
            return new CharacterSeparatedValuesOptions(this);
        }
    }
}
