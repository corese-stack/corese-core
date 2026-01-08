package fr.inria.corese.core.next.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RDFaLocalValues {

    // Local context
    private boolean skipElement = false;
    private Resource newSubject = null;
    private Resource currentObjectResource = null;
    private Resource typedResource = null;
    private Map<String, IRI> iRIMappings = new HashMap<>();
    private Set<RDFaIncompleteStatement> incompleteStatements = null;
    private Map<IRI, Set<Value>> listMappings = new HashMap<>();
    private String currentLanguage = null;
    private Value currentPropertyValue = null;
    private String defaultVocabulary = null;

    public RDFaLocalValues() {
    }

    /**
     * Constructor to be used in step 1 of RDFa processing
     * @param context
     */
    public RDFaLocalValues(RDFaEvaluationContext context) {
        this.skipElement = false;
        this.newSubject = null;
        this.currentObjectResource = null;
        this.typedResource = null;
        this.iRIMappings = context.getIriMappings();
        this.incompleteStatements = context.getIncompleteStatement();
        this.listMappings = context.getListMappings();
        this.currentLanguage = context.getLanguage();
        this.defaultVocabulary = context.getDefaultVocabulary();
    }

    public RDFaLocalValues(RDFaLocalValues other) {
        this.skipElement = other.skipElement;
        this.newSubject = other.newSubject;
        this.currentObjectResource = other.currentObjectResource;
        this.typedResource = other.typedResource;
        this.iRIMappings = other.iRIMappings;
        this.incompleteStatements = other.incompleteStatements;
        this.listMappings = other.listMappings;
        this.currentLanguage = other.currentLanguage;
        this.currentPropertyValue = other.currentPropertyValue;
        this.defaultVocabulary = other.defaultVocabulary;
    }

    public boolean isSkipElement() {
        return skipElement;
    }

    public void setSkipElement(boolean skipElement) {
        this.skipElement = skipElement;
    }

    public Resource getNewSubject() {
        return newSubject;
    }

    public void setNewSubject(Resource newSubject) {
        this.newSubject = newSubject;
    }

    public Resource getCurrentObjectResource() {
        return currentObjectResource;
    }

    public void setCurrentObjectResource(Resource currentObjectResource) {
        this.currentObjectResource = currentObjectResource;
    }

    public Resource getTypedResource() {
        return typedResource;
    }

    public void setTypedResource(Resource typedResource) {
        this.typedResource = typedResource;
    }

    public Map<String, IRI> getIRIMappings() {
        return iRIMappings;
    }

    public void setIRIMappings(Map<String, IRI> iRIMappings) {
        this.iRIMappings = iRIMappings;
    }

    public void addIRIMappings(String key, IRI value) {
        if(this.iRIMappings == null) {
            this.iRIMappings = new HashMap<>();
        }
        this.iRIMappings.put(key, value);
    }

    public Set<RDFaIncompleteStatement> getIncompleteStatements() {
        return incompleteStatements;
    }

    public void setIncompleteStatements(Set<RDFaIncompleteStatement> incompleteStatements) {
        this.incompleteStatements = incompleteStatements;
    }

    public void addIncompleteStatement(RDFaIncompleteStatement statement) {
        this.incompleteStatements.add(statement);
    }

    public Map<IRI, Set<Value>> getListMappings() {
        return listMappings;
    }

    public void setListMappings(Map<IRI, Set<Value>> listMappings) {
        this.listMappings = listMappings;
    }

    public void addListMapping(IRI key, Value value) {
        if(! this.listMappings.containsKey(key)) {
            this.listMappings.put(key, new HashSet<>());
        }
        this.listMappings.get(key).add(value);
    }

    public void addListMappings(IRI key, Set<Value> objects) {
        this.listMappings.put(key, objects);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public void setCurrentLanguage(String currentLanguage) {
        this.currentLanguage = currentLanguage;
    }

    public Value getCurrentPropertyValue() {
        return currentPropertyValue;
    }

    public void setCurrentPropertyValue(Value currentPropertyValue) {
        this.currentPropertyValue = currentPropertyValue;
    }

    public String getDefaultVocabulary() {
        return defaultVocabulary;
    }

    public void setDefaultVocabulary(String defaultVocabulary) {
        this.defaultVocabulary = defaultVocabulary;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("newSubject: ").append(this.newSubject).append(" ");
        sb.append("currentObjectResource: ").append(this.currentObjectResource).append(" ");
        sb.append("typedResource: ").append(this.typedResource).append(" ");
        sb.append("currentLanguage: ").append(this.currentLanguage).append(" ");
        sb.append("currentPropertyValue: ").append(this.currentPropertyValue).append(" ");
        sb.append("defaultVocabulary: ").append(this.defaultVocabulary).append(" ");

        return sb.toString();
    }
}
