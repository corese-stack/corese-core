package fr.inria.corese.core.next.data.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;

import java.util.*;

/**
 * This class is to be used during the evaluation of an HTML file to generate triples during the DOM traversal.
 * @see <a href="https://www.w3.org/TR/rdfa-core/#evaluation-context">RDFa recommandation<a/>
 */
public class RDFaEvaluationContext {
    /**
     * The base. This will usually be the IRI of the document being processed, but it could be some other IRI, set by some other mechanism, such as the (X)HTML base element. The important thing is that it establishes an IRI against which relative paths can be resolved.
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
     * Set of statements in the process of building.
     */
    private Set<RDFaIncompleteStatement> incompleteStatements = new HashSet<>();

    /**
     * The language of the document. Note that there is no default language.
     */
    private String language = null;
    /**
     * A list mapping that associates IRIs with lists.
     */
    private Map<IRI, Set<Value>> listMappings = new HashMap<>();
    /**
     * The term mappings, a list of terms and their associated IRIs. This specification does not define an initial list. Host Languages MAY define an initial list.
     */
    private Map<String, IRI> termMappings = new HashMap<>();

    /**
     * The default vocabulary, a value to use as the prefix IRI when a term unknown to the RDFa Processor is used. This specification does not define an initial setting for the default vocabulary. Host Languages MAY define an initial setting.
     */
    private String defaultVocabulary = null;

    public RDFaEvaluationContext(IRI baseIri) {
        this.baseIri = baseIri;
    }

    public RDFaEvaluationContext(RDFaEvaluationContext context) {
        this.baseIri = context.baseIri;
        this.defaultVocabulary = context.defaultVocabulary;
        this.incompleteStatements = new HashSet<>(context.incompleteStatements);
        this.language = context.language;
        this.listMappings = new HashMap<>(context.listMappings);
        this.parentObjectResource = context.parentObjectResource;
        this.parentSubjectResource = context.parentSubjectResource;
        this.termMappings = new HashMap<>(context.termMappings);
    }

    public IRI getBaseIri() {
        return baseIri;
    }

    public void setBaseIri(IRI baseIri) {
        this.baseIri = baseIri;
    }

    public Resource getParentSubjectResource() {
        return parentSubjectResource;
    }

    public void setParentSubjectResource(Resource parentSubjectResource) {
        this.parentSubjectResource = parentSubjectResource;
    }

    public Resource getParentObjectResource() {
        return parentObjectResource;
    }

    public void setParentObjectResource(Resource parentObjectResource) {
        this.parentObjectResource = parentObjectResource;
    }

    public Set<RDFaIncompleteStatement> getIncompleteStatements() {
        return incompleteStatements;
    }

    public void setIncompleteStatements(Set<RDFaIncompleteStatement> incompleteStatement) {
        this.incompleteStatements = incompleteStatement;
    }

    public Iterator<RDFaIncompleteStatement> getIncompleteStatementIterator() {
        return this.incompleteStatements.iterator();
    }

    public void addStatementWithoutSubject(IRI property, Value object) {
        RDFaIncompleteStatement newStatement = new RDFaIncompleteStatement(property);
        newStatement.setObject(object);
        this.incompleteStatements.add(newStatement);
    }

    public void addStatementWithoutObject(Resource subject, IRI property) {
        RDFaIncompleteStatement newStatement = new RDFaIncompleteStatement(property);
        newStatement.setSubject(subject);
        this.incompleteStatements.add(newStatement);
    }

    public void clearIncompleteStatements() {
        this.incompleteStatements.clear();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDefaultVocabulary() {
        return defaultVocabulary;
    }

    public void setDefaultVocabulary(String defaultVocabulary) {
        this.defaultVocabulary = defaultVocabulary;
    }

    public void addTermMapping(String term, IRI iri) {
        this.termMappings.put(term, iri);
    }

    public IRI getTermMapping(String term) {
        return this.termMappings.get(term);
    }

    public Map<String, IRI> getTermMappings() {
        return this.termMappings;
    }

    public Map<IRI, Set<Value>> getListMappings() {
        return listMappings;
    }

    public void setListMappings(Map<IRI, Set<Value>> listMappings) {
        this.listMappings = listMappings;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("BaseURI: ").append(this.getBaseIri().stringValue()).append(" ");
        if(this.getParentSubjectResource() != null) {
            sb.append("Subject:").append(this.getParentSubjectResource().stringValue()).append(" ");
        } else {
            sb.append("Subject:").append((Object) null).append(" ");
        }
        if(this.getParentObjectResource() != null) {
            sb.append("Object: ").append(this.getParentObjectResource().stringValue()).append(" ");
        } else {
            sb.append("Object: ").append((Object) null).append(" ");
        }
        if(! this.getIncompleteStatements().isEmpty()) {
            sb.append(this.getIncompleteStatements().size()).append(" incomplete statements.");
        }

        return sb.toString();
    }
}
