package fr.inria.corese.core.next.data.impl.adapter.literal;

import java.util.Objects;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.support.term.literal.AbstractLiteral;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseUndefLiteral;

/**
 * RDF literal whose lexical form is outside the lexical space of its recognized
 * datatype.
 *
 * <p>The RDF term remains valid and keeps its original lexical form and datatype.
 * Value conversions inherited from {@link AbstractLiteral} fail because an
 * ill-typed literal has no value for that datatype.</p>
 */
public final class CoreseIllTypedLiteral extends AbstractLiteral implements CoreseDatatypeAdapter {

    private final String lexicalValue;
    private final CoreDatatype coreDatatype;

    /**
     * Creates an ill-typed RDF literal term.
     *
     * @param lexicalValue original lexical form
     * @param datatype original datatype IRI
     * @param coreDatatype recognized datatype used for value operations
     */
    public CoreseIllTypedLiteral(String lexicalValue, IRI datatype, CoreDatatype coreDatatype) {
        super(Objects.requireNonNull(datatype, "datatype"));
        this.lexicalValue = Objects.requireNonNull(lexicalValue, "lexicalValue");
        this.coreDatatype = Objects.requireNonNull(coreDatatype, "coreDatatype");
    }

    @Override
    public String getLabel() {
        return lexicalValue;
    }

    @Override
    public String stringValue() {
        return lexicalValue;
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return coreDatatype;
    }

    @Override
    protected void setCoreDatatype(CoreDatatype coreDatatype) {
        throw new UnsupportedOperationException("The datatype of an RDF literal is immutable");
    }

    @Override
    public Node getCoreseNode() {
        return createCoreseObject();
    }

    @Override
    public IDatatype getIDatatype() {
        return createCoreseObject();
    }

    private CoreseUndefLiteral createCoreseObject() {
        return new CoreseUndefLiteral(lexicalValue, datatype.stringValue());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
