package fr.inria.corese.core.next.data.impl.adapter.literal;

import java.util.Optional;
import java.util.Locale;
import java.util.Objects;

import fr.inria.corese.core.kgram.api.core.Node;
import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.spi.term.literal.AbstractStringLiteral;
import fr.inria.corese.core.next.data.api.literal.CoreDatatype;
import fr.inria.corese.core.next.data.api.literal.RDFDatatype;
import fr.inria.corese.core.next.data.api.exception.IncorrectOperationException;
import fr.inria.corese.core.next.data.impl.adapter.node.CoreseIRI;
import fr.inria.corese.core.sparql.api.IDatatype;
import fr.inria.corese.core.sparql.datatype.CoreseLiteral;

/**
 * An implementation of a language-tagged string literal used by Corese.
 * This class represents a string literal that is associated with a language
 * tag, specifically when the datatype IRI is
 * {@code http://www.w3.org/1999/02/22-rdf-syntax-ns#langString}.
 * It extends {@link AbstractStringLiteral} and implements
 * {@link CoreseDatatypeAdapter}.
 */

@SuppressWarnings("java:S2160")
public class CoreseLanguageTaggedStringLiteral extends AbstractStringLiteral implements CoreseDatatypeAdapter {
    /**
     * The Corese object that holds the literal value and language tag in the old
     * API.
     */
    private final transient CoreseLiteral coreseObject;

    /**
     * The language tag associated with the literal.
     */
    private final String language;
    /**
     * The value of the language-tagged string literal.
     */
    private String value;

    /**
     * Constructs a {@link CoreseLanguageTaggedStringLiteral} instance from an
     * {@link IDatatype} Corese object.
     * The Corese object should be an instance of
     * {@link CoreseLiteral}.
     *
     * @param coreseObject The {@link IDatatype} Corese object representing the
     *                     tagged literal.
     * @throws IncorrectOperationException If the provided {@code coreseObject} is
     *                                     not a valid
     *                                     {@link CoreseLiteral}.
     */
    public CoreseLanguageTaggedStringLiteral(IDatatype coreseObject) {
        super(new CoreseIRI(coreseObject.getDatatypeURI()));
        if (coreseObject instanceof CoreseLiteral literal) {
            this.coreseObject = literal;
            this.language = normalizeLanguage(coreseObject.getLang());
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
        this(new CoreseLiteral(value, normalizeLanguage(language)));
        this.value = value;
    }

    @Override
    public void setCoreDatatype(CoreDatatype coreDatatype) {
        // Intentionally no-op: language-tagged string literal datatype is fixed to rdf:langString.
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
    @Override
    public Optional<String> getLanguage() {
        return Optional.ofNullable(language);
    }

    @Override
    public CoreDatatype getCoreDatatype() {
        return RDFDatatype.LANGSTRING;
    }

    @Override
    public IDatatype getIDatatype() {
        return this.coreseObject;
    }

    @Override
    public Node getCoreseNode() {
        return this.coreseObject;
    }

    private static String normalizeLanguage(String language) {
        return Objects.requireNonNull(language, "language").toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the datatype IRI for language-tagged string literals, which is
     * {@code http://www.w3.org/1999/02/22-rdf-syntax-ns#langString}.
     *
     * @return The datatype IRI for language-tagged string literals.
     */
    @Override
    public IRI getDatatype() {
        return RDFDatatype.LANGSTRING.getIRI();
    }
}
