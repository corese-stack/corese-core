package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.vocabulary.XSD;

import java.util.Objects;
import java.util.Optional;

import static fr.inria.corese.core.next.api.model.base.CoreDatatype.XSD.STRING;

public class LocalizedAbstractString extends AbstractLiteral {

    private final String value;
    private final String language;
    private final IRI datatype;
    private final CoreDatatype coreDatatype = STRING;

    protected LocalizedAbstractString(IRI datatype, String value, String language) {
        super(datatype);
        this.value = value;
        this.datatype = XSD.xsdString.getIRI();
        this.language = language;
    }

    @Override
    public String getLabel() {
        return value;
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

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {}

    @Override
    public Optional<String> getLanguage() {
        return Optional.ofNullable(language);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalizedAbstractString that = (LocalizedAbstractString) o;
        return Objects.equals(value, that.value) && Objects.equals(language, that.language) && Objects.equals(datatype, that.datatype) && Objects.equals(coreDatatype, that.coreDatatype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, language, datatype, coreDatatype);
    }

    @Override
    public String toString() {
        final String value = '"' + this.value + '"';
        return getLanguage()
                .map(language -> value + '@' + language)
                .orElseGet(() -> value + "^^<" + getDatatype().stringValue() + ">");
    }
}