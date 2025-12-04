package fr.inria.corese.core.next.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public interface RDFaEvaluationContext {

    IRI getBaseIri();
    void setBaseIri(IRI baseIri);

    void setParentSubjectResource(Resource parentSubjectResource);
    Resource getParentSubjectResource();

    void setParentObjectResource(Resource parentObjectResource);
    Resource getParentObjectResource();

    void setIncompleteStatements(Set<RDFaIncompleteStatement> incompleteStatement);
    Set<RDFaIncompleteStatement> getIncompleteStatement();
    Iterator<RDFaIncompleteStatement> getIncompleteStatementIterator();
    void addStatementWithoutSubject(IRI property, Value object);
    void addStatementWithoutObject(Resource subject, IRI property);
    void clearIncompleteStatements();

    boolean hasIriMapping(String prefix);
    IRI getIriMapping(String prefix);
    Map<String, IRI> getIriMappings();
    void addIriMapping(String prefix, IRI prefixIri);
    void setIriMappings(Map<String, IRI> iriMappings);

    String getLanguage();
    void setLanguage(String language);

}
