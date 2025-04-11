package fr.inria.corese.core.next.api.base.model.literal;

import fr.inria.corese.core.next.api.IRI;

public abstract class AbstractString extends AbstractLiteral implements Comparable<AbstractString> {
    protected AbstractString(IRI datatype) {
        super(datatype);
    }

    public String stringValue() {
        return getLabel();
    }

    @Override
    public int compareTo(AbstractString abstractString) {
        return stringValue().compareTo(abstractString.stringValue());
    }
}