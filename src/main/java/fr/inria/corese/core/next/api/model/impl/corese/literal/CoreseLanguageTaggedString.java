package fr.inria.corese.core.next.api.model.impl.corese.literal;


import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.api.model.IRI;
import fr.inria.corese.core.next.api.model.base.CoreDatatype;
import fr.inria.corese.core.next.api.model.impl.corese.CoreseIRI;
import fr.inria.corese.core.next.api.model.impl.literal.AbstractString;
import fr.inria.corese.core.sparql.api.IDatatype;

import java.util.Optional;


/**
 * CoreseTagged is an implementation of the Literal used by Corese
 * CoreseLanguageTaggedString has two conditions, if and only if datatype IRI is http://www.w3.org/1999/02/22-rdf-syntax-ns#langString, a non-empty language tag is defined
 */

public class CoreseLanguageTaggedString extends AbstractString implements CoreseDatatypeAdapter {
    private final fr.inria.corese.core.sparql.datatype.CoreseLiteral coreseObject;

    private String language;
    private String value;

    public CoreseLanguageTaggedString(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseLiteral) {
            this.coreseObject = (fr.inria.corese.core.sparql.datatype.CoreseLiteral) coreseObject;
            this.language = coreseObject.getLang();
            this.value = coreseObject.getLabel();
        }
        else {
            throw new IncorrectOperationException("Cannot create CoreseLiteral from a non-literal Corese object");
        }
    }


    public CoreseLanguageTaggedString(String value, String language) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseLiteral(value, language));
        this.language = language;
        this.value = value;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {}

    @Override
    public String getLabel() {
        return coreseObject.getLabel();
    }

    public String getValue() {
        return this.value;
    }

    public Optional<String> getLanguage() {return Optional.ofNullable(language);}

    @Override
    public CoreDatatype getCoreDatatype() {
        return CoreDatatype.RDF.LANGSTRING;
    }

    @Override
    public IDatatype getIDatatype() {
        return this.coreseObject;
    }

    @Override
    public Node getCoreseNode() {
        return this.coreseObject;
    }

    public IRI getDatatype() {
        return CoreDatatype.RDF.LANGSTRING.getIRI();
    }
}