package fr.inria.corese.core.next.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;

import java.util.*;

public abstract class AbstractRDFaEvaluationContext implements RDFaEvaluationContext {

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
     * An index of locally defined IRI prefixes
     */
    private Map<String, IRI> iriMappings = new HashMap<>();

    /**
     * Set of statement in the process of building.
     */
    private Set<RDFaIncompleteStatement> incompleteStatement = new HashSet<>();

    /**
     * The language of the document. Note that there is no default language.
     */
    private String language = null;

    protected AbstractRDFaEvaluationContext(IRI baseIri) {
        this(baseIri, baseIri);
    }

    protected AbstractRDFaEvaluationContext(IRI baseIri, IRI parentSubjectResource) {
        this.baseIri = baseIri;
        this.parentSubjectResource = parentSubjectResource;
    }

    protected AbstractRDFaEvaluationContext(AbstractRDFaEvaluationContext context) {
        this.baseIri = context.baseIri;
        this.parentSubjectResource = context.parentSubjectResource;
        this.parentObjectResource = context.parentObjectResource;
        this.iriMappings = new HashMap<>(context.iriMappings);
        this.incompleteStatement = new HashSet<>(context.incompleteStatement);
        this.language = context.language;
    }

    @Override
    public IRI getBaseIri() {
        return baseIri;
    }

    @Override
    public void setBaseIri(IRI baseIri) {
        this.baseIri = baseIri;
    }

    @Override
    public Resource getParentSubjectResource() {
        return parentSubjectResource;
    }

    @Override
    public void setParentSubjectResource(Resource parentSubjectResource) {
        this.parentSubjectResource = parentSubjectResource;
    }

    @Override
    public Resource getParentObjectResource() {
        return parentObjectResource;
    }

    @Override
    public void setParentObjectResource(Resource parentObjectResource) {
        this.parentObjectResource = parentObjectResource;
    }

    @Override
    public Map<String, IRI> getIriMappings() {
        return iriMappings;
    }

    @Override
    public void setIriMappings(Map<String, IRI> iriMappings) {
        this.iriMappings = iriMappings;
    }

    @Override
    public boolean hasIriMapping(String prefix) {
        return this.iriMappings.containsKey(prefix);
    }

    /**
     * @param prefix the prefix WITHOUT ":"
     * @return the IRI associated to the prefix in this context
     */
    @Override
    public IRI getIriMapping(String prefix) {
        return this.iriMappings.get(prefix);
    }

    @Override
    public void addIriMapping(String prefix, IRI prefixIri) {
        this.iriMappings.put(prefix, prefixIri);
    }

    @Override
    public Set<RDFaIncompleteStatement> getIncompleteStatement() {
        return incompleteStatement;
    }

    @Override
    public void setIncompleteStatements(Set<RDFaIncompleteStatement> incompleteStatement) {
        this.incompleteStatement = incompleteStatement;
    }

    @Override
    public Iterator<RDFaIncompleteStatement> getIncompleteStatementIterator() {
        return this.incompleteStatement.iterator();
    }

    @Override
    public void addStatementWithoutSubject(IRI property, Value object) {
        RDFaIncompleteStatement newStatement = new RDFaIncompleteStatement(property);
        newStatement.setObject(object);
        this.incompleteStatement.add(newStatement);
    }

    @Override
    public void addStatementWithoutObject(Resource subject, IRI property) {
        RDFaIncompleteStatement newStatement = new RDFaIncompleteStatement(property);
        newStatement.setSubject(subject);
        this.incompleteStatement.add(newStatement);
    }

    public void clearIncompleteStatements() {
        this.incompleteStatement.clear();
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        this.language = language;
    }
}
