package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Statement;
import fr.inria.corese.core.next.api.ValueFactory;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaIncompleteStatement;

import java.util.*;

/**
 * This class is to be used during the evaluation of an HTML file to generate triples during the DOM traversal.
 * @see <a href="https://www.w3.org/TR/rdfa-syntax/#sec_5.2.">RDFa recommandation<a/>
 */
public class RDFaEvaluationContextHandler {

    private ValueFactory factory;

    /**
     * This will usually be the URL of the document being processed, but it could be some other URL, set by some other mechanism, such as the XHTML base element. The important thing is that it establishes a URL against which relative paths can be resolved.
     */
    private IRI baseIri;

    /**
     * The initial value will be the same as the initial value of [base], but it will usually change during the course of processing.
     */
    private Resource parentSubjectResource ;

    /**
     *  In some situations the object of a statement becomes the subject of any nested statements, and this property is used to convey this value. Note that this value may be a bnode, since in some situations a number of nested statements are grouped together on one bnode. This means that the bnode must be set in the containing statement and passed down, and this property is used to convey this value.
     */
    private Resource parentObjectResource = null;

    /**
     * An index of locally defined IRI prefixes
     */
    private Map<String, IRI> uriMappings = new HashMap<>();

    /**
     * Set of statement in the process of building.
     */
    private Set<RDFaIncompleteStatement> incompleteStatement = new HashSet<>();

    /**
     * The language of the document. Note that there is no default language.
     */
    private String language = null;

    private Statement incompleteStatementToStatement(RDFaIncompleteStatement incompleteStatement) {
        Objects.requireNonNull(incompleteStatement.getSubject(), "Null subject, IncompleteStatement can only be converted if all its component are non-null.");
        Objects.requireNonNull(incompleteStatement.getPredicate(), "Null predicate, IncompleteStatement can only be converted if all its component are non-null.");
        Objects.requireNonNull(incompleteStatement.getObject(), "Null object, IncompleteStatement can only be converted if all its component are non-null.");

        return factory.createStatement(incompleteStatement.getSubject(), incompleteStatement.getPredicate(), incompleteStatement.getObject());
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
}
