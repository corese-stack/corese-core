package fr.inria.corese.core.next.data.api.term;

import java.io.Serial;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.CoreDatatypes;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.literal.XSDDatatype;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractLiteral;

/** Immutable default implementation of an RDF literal. */
public final class SimpleLiteral extends AbstractLiteral {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String label;
    private final String language;
    private final CoreDatatype coreDatatype;

    public SimpleLiteral(String label) {
        this(label, XSDDatatype.STRING.getIRI(), XSDDatatype.STRING);
    }

    public SimpleLiteral(String label, String language) {
        super(RDFDatatype.LANGSTRING.getIRI());
        this.label = Objects.requireNonNull(label, "label");
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("Language must not be null or blank");
        }
        this.language = language.toLowerCase(Locale.ROOT);
        this.coreDatatype = RDFDatatype.LANGSTRING;
    }

    public SimpleLiteral(String label, IRI datatype) {
        this(label, datatype, null);
    }

    public SimpleLiteral(String label, IRI datatype, CoreDatatype coreDatatype) {
        super(datatype == null ? XSDDatatype.STRING.getIRI() : datatype);
        this.label = Objects.requireNonNull(label, "label");
        this.language = null;
        this.coreDatatype = coreDatatype == null ? CoreDatatypes.from(this.datatype) : coreDatatype;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String stringValue() {
        return label;
    }

    @Override
    public Optional<String> getLanguage() {
        return Optional.ofNullable(language);
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return coreDatatype;
    }

    @Override
    public IRI getDatatype() {
        return this.datatype;
    }

    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new IncorrectOperationException("SimpleLiteral is immutable");
    }

    @Override
    public boolean booleanValue() {
        if (coreDatatype == XSDDatatype.BOOLEAN) {
            return switch (label) {
                case "true", "1" -> true;
                case "false", "0" -> false;
                default -> throw new IncorrectOperationException("Invalid boolean literal: " + label);
            };
        }
        return super.booleanValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Literal other)) {
            return false;
        }
        return label.equals(other.getLabel())
                && datatype.equals(other.getDatatype())
                && Objects.equals(getLanguage(), other.getLanguage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, datatype, getLanguage());
    }
}
