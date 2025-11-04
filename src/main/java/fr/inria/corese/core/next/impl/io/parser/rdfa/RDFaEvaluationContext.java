package fr.inria.corese.core.next.impl.io.parser.rdfa;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;
import fr.inria.corese.core.next.impl.io.parser.rdfa.model.RDFaIncompleteStatement;

import java.util.*;

/**
 * This class is to be used during the evaluation of an HTML file to generate triples during the DOM traversal.
 * @see <a href="https://www.w3.org/TR/rdfa-syntax/#sec_5.2.">RDFa recommandation<a/>
 */
public class RDFaEvaluationContext {

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

    public RDFaEvaluationContext(IRI baseIri) {
        this.baseIri = baseIri;
        this.parentSubjectResource = baseIri;
    }

    public RDFaEvaluationContext(IRI baseIri, IRI parentSubjectResource) {
        this.baseIri = baseIri;
        this.parentSubjectResource = parentSubjectResource;
    }

    public RDFaEvaluationContext(RDFaEvaluationContext context) {
        this.baseIri = context.baseIri;
        this.parentSubjectResource = context.parentSubjectResource;
        this.parentObjectResource = context.parentObjectResource;
        this.uriMappings = new HashMap<>(context.uriMappings);
        this.incompleteStatement = new HashSet<>(context.incompleteStatement);
        this.language = context.language;
    }

    public IRI baseIri() {
        return baseIri;
    }

    public RDFaEvaluationContext baseIri(IRI baseIri) {
        this.baseIri = baseIri;
        return this;
    }

    public RDFaEvaluationContext incompleteStatements(Set<RDFaIncompleteStatement> incompleteStatement) {
        this.incompleteStatement = new HashSet<>(incompleteStatement);
        return this;
    }

    public Iterator<RDFaIncompleteStatement> getIncompleteStatementIterator() {
        return this.incompleteStatement.iterator();
    }

    public RDFaEvaluationContext addStatementWithoutSubject(IRI property, Value object) {
        RDFaIncompleteStatement newStatement = new RDFaIncompleteStatement(property);
        newStatement.setObject(object);
        this.incompleteStatement.add(newStatement);
        return this;
    }

    public RDFaEvaluationContext addStatementWithoutObject(Resource subject, IRI property) {
        RDFaIncompleteStatement newStatement = new RDFaIncompleteStatement(property);
        newStatement.setSubject(subject);
        this.incompleteStatement.add(newStatement);
        return this;
    }

    public void clearIncompleteStatements() {
        this.incompleteStatement.clear();
    }

    public Resource parentSubjectResource() {
        return parentSubjectResource;
    }

    public RDFaEvaluationContext parentSubjectResource(Resource parentSubjectResource) {
        this.parentSubjectResource = parentSubjectResource;
        return this;
    }

    public Resource parentObjectResource() {
        return parentObjectResource;
    }

    public RDFaEvaluationContext parentObjectResource(Resource parentObjectResource) {
        this.parentObjectResource = parentObjectResource;
        return this;
    }

    public Map<String, IRI> uriMappings() {
        return uriMappings;
    }

    public RDFaEvaluationContext uriMappings(Map<String, IRI> uriMappings) {
        this.uriMappings = uriMappings;
        return this;
    }

    public boolean hasUriMapping(String prefix) {
        return this.uriMappings.containsKey(prefix);
    }

    /**
     * @param prefix the prefix WITHOUT ":"
     * @return the IRI associated to the prefix in this context
     */
    public IRI uriMapping(String prefix) {
        return this.uriMappings.get(prefix);
    }

    public void addUriMapping(String prefix, IRI prefixIri) {
        this.uriMappings.put(prefix, prefixIri);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("BaseURI: ").append(this.baseIri.stringValue()).append(" ");
        sb.append("Mappings: [");
        this.uriMappings.forEach((key, value) -> sb.append("(").append(key).append(", ").append(value.stringValue()).append(") "));
        sb.append("] ");
        if(this.parentSubjectResource != null) {
            sb.append("Subject:").append(this.parentSubjectResource.stringValue()).append(" ");
        } else {
            sb.append("Subject:").append((Object) null).append(" ");
        }
        if(this.parentObjectResource != null) {
            sb.append("Object: ").append(this.parentObjectResource.stringValue()).append(" ");
        } else {
            sb.append("Object: ").append((Object) null).append(" ");
        }
        if(! this.incompleteStatement.isEmpty()) {
            sb.append(this.incompleteStatement.size()).append(" incomplete statements.");
        }

        return sb.toString();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
