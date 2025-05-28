package fr.inria.corese.core.next.impl.temp.literal;

import java.util.Optional;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.base.model.literal.AbstractStringLiteral;
import fr.inria.corese.core.next.api.literal.CoreDatatype;
import fr.inria.corese.core.next.impl.common.literal.RDF;
import fr.inria.corese.core.next.impl.exception.IncorrectOperationException;
import fr.inria.corese.core.next.impl.temp.CoreseIRI;
import fr.inria.corese.core.sparql.api.IDatatype;

/**
 * An implementation of a language-tagged string literal used by Corese.
 * This class represents a string literal that is associated with a language
 * tag, specifically when the datatype IRI is
 * {@code http://www.w3.org/1999/02/22-rdf-syntax-ns#langString}.
 * It extends {@link AbstractStringLiteral} and implements
 * {@link CoreseDatatypeAdapter}.
 */

public class CoreseLanguageTaggedStringLiteral extends AbstractStringLiteral implements CoreseDatatypeAdapter {
    /**
     * The Corese object that holds the literal value and language tag in the old
     * API.
     */
    private final fr.inria.corese.core.sparql.datatype.CoreseLiteral coreseObject;

    /**
     * The language tag associated with the literal.
     */
    private String language;
    /**
     * The value of the language-tagged string literal.
     */
    private String value;

    /**
     * Constructs a {@link CoreseLanguageTaggedStringLiteral} instance from an
     * {@link IDatatype} Corese object.
     * The Corese object should be an instance of
     * {@link fr.inria.corese.core.sparql.datatype.CoreseString}.
     * 
     * @param coreseObject The {@link IDatatype} Corese object representing the
     *                     tagged literal.
     * @throws IncorrectOperationException If the provided {@code coreseObject} is
     *                                     not a valid
     *                                     {@link fr.inria.corese.core.sparql.datatype.CoreseLiteral}.
     */
    public CoreseLanguageTaggedStringLiteral(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof fr.inria.corese.core.sparql.datatype.CoreseLiteral) {
            this.coreseObject = (fr.inria.corese.core.sparql.datatype.CoreseLiteral) coreseObject;
            this.language = coreseObject.getLang();
            this.value = coreseObject.getLabel();
        } else {
            throw new IncorrectOperationException("Cannot create CoreseLiteral from a non-literal Corese object");
        }
    }

    /**
     * Constructs a {@link CoreseLanguageTaggedStringLiteral} instance with the
     * given value and language tag.
     * This constructor creates a {@link CoreseLanguageTaggedStringLiteral} from the
     * provided string value and language tag.
     *
     * @param value    The value of the language-tagged string literal.
     * @param language The language tag associated with the literal.
     */
    public CoreseLanguageTaggedStringLiteral(String value, String language) {
        this(new fr.inria.corese.core.sparql.datatype.CoreseLiteral(value, language));
        this.language = language;
        this.value = value;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
    }

    @Override
    public String getLabel() {
        return coreseObject.getLabel();
    }

    public String getValue() {
        return this.value;
    }

    /**
     * Returns the language tag of the language-tagged literal, wrapped in an
     * {@link Optional}.
     * If no language tag is set, an empty {@link Optional} will be returned.
     *
     * @return An {@link Optional} containing the language tag, or an empty
     *         {@link Optional} if no language is set.
     */
    public Optional<String> getLanguage() {
        return Optional.ofNullable(language);
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return RDF.LANGSTRING;
    }

    @Override
    public IDatatype getIDatatype() {
        return this.coreseObject;
    }

    @Override
    public Node getCoreseNode() {
        return this.coreseObject;
    }

    /**
     * Returns the datatype IRI for language-tagged string literals, which is
     * {@code http://www.w3.org/1999/02/22-rdf-syntax-ns#langString}.
     *
     * @return The datatype IRI for language-tagged string literals.
     */
    public IRI getDatatype() {
        return RDF.LANGSTRING.getIRI();
    }
}