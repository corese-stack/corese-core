package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.Literal;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;

import static fr.inria.corese.core.next.api.model.base.CoreDatatype.XSD.STRING;

public abstract class AbstractString extends AbstractLiteral {
    protected String value;
    protected IRI datatype;

    protected AbstractString(IRI datatype, String value) {
        super(datatype);
        this.value = value;
        this.datatype = XSD.xsdString.getIRI();
    }

    @Override
    public String getLabel() {
        return this.value;
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return STRING;
    }

    @Override
    public String stringValue() {
        return this.value;
    }

    public String toString() {
        return this.value + "^^<" + getDatatype().stringValue() + ">";
    }

    @Override
    public int hashCode() {
        return getLabel().hashCode();
    }

    public boolean equals(Object o) {
        return this == o || o instanceof Literal
                && getLabel().equals(((Literal) o).getLabel())
                && getDatatype().equals(((Literal) o).getDatatype());
    }
}