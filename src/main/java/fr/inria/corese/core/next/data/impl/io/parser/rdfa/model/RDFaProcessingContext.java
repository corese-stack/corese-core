package fr.inria.corese.core.next.data.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.data.api.term.IRI;
import fr.inria.corese.core.next.data.api.term.Resource;
import fr.inria.corese.core.next.data.api.term.Value;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Corresponds to the local values for the processing of an element and the current context at the moment of its evaluation
 */
public class RDFaProcessingContext {

    // Local context
    private String elementName = null;
    private boolean skipElement = false;
    private Resource newSubject = null;
    private Resource currentObjectResource = null;
    private Resource typedResource = null;
    private Set<RDFaIncompleteStatement> incompleteStatements = null;
    private Map<IRI, List<Value>> listMappings = new HashMap<>();
    private String currentLanguage = null;
    private Value currentPropertyValue = null;
    private String defaultVocabulary = null;
    private Map<String, String> elementAttributes = new HashMap<>();

    /** Namespace declarations explicitly in scope for XML literal serialization. */
    private Map<String, String> namespaceDeclarations = new HashMap<>();

    /**
     * Buffer for accumulating character data between start and end tags.
     */
    private StringBuilder characters = new StringBuilder();

    /**
     * Serialized XML descendants used when this element establishes an
     * {@code rdf:XMLLiteral} property value.
     */
    private StringBuilder xmlLiteralContent = new StringBuilder();

    private boolean xmlLiteralProperty;

    private boolean isRootElement = true;

    private RDFaEvaluationContext evaluationContext = null;

    /**
     * Constructor to be used in step 1 of RDFa processing
     * @param context
     */
    public RDFaProcessingContext(RDFaEvaluationContext context) {
        this.skipElement = false;
        this.newSubject = null;
        this.currentObjectResource = null;
        this.typedResource = null;
        // The RDFa processing sequence defines this as an element-local value.
        // Descendants receive it through their evaluation context, but siblings
        // must never share it after this element closes.
        this.incompleteStatements = new HashSet<>();
        // The RDFa algorithm explicitly defines this as a reference to the
        // evaluation context's list mapping. Step 8 replaces it when a new
        // subject establishes a new list owner.
        this.listMappings = context.getListMappings();
        this.currentLanguage = context.getLanguage();
        this.defaultVocabulary = context.getDefaultVocabulary();
        this.evaluationContext = context;
    }

    public Map<String, String> getNamespaceDeclarations() {
        return namespaceDeclarations;
    }

    public void setNamespaceDeclarations(Map<String, String> namespaceDeclarations) {
        this.namespaceDeclarations = new HashMap<>(namespaceDeclarations);
    }

    public void addNamespaceDeclaration(String prefix, String namespace) {
        this.namespaceDeclarations.put(prefix, namespace);
    }

    /**
     *
     * @return The skip element flag, which indicates whether the current element can safely be ignored since it has no relevant RDFa attributes. Note that descendant elements will still be processed.
     */
    public boolean isSkipElement() {
        return skipElement;
    }

    /**
     *
     * @param skipElement The skip element flag, which indicates whether the current element can safely be ignored since it has no relevant RDFa attributes. Note that descendant elements will still be processed.
     */
    public void setSkipElement(boolean skipElement) {
        this.skipElement = skipElement;
    }

    /**
     *
     * @return A new subject value, which once calculated will set the parent subject in an evaluation context, as well as being used to complete any incomplete triples, as described in the next section.
     */
    public Resource getNewSubject() {
        return newSubject;
    }

    /**
     *
     * @param newSubject A new subject value, which once calculated will set the parent subject in an evaluation context, as well as being used to complete any incomplete triples, as described in the next section.
     */
    public void setNewSubject(Resource newSubject) {
        this.newSubject = newSubject;
    }

    /**
     *
     * @return A value for the current object resource, the resource to use when creating triples that have a resource object.
     */
    public Resource getCurrentObjectResource() {
        return currentObjectResource;
    }

    /**
     *
     * @param currentObjectResource A value for the current object resource, the resource to use when creating triples that have a resource object.
     */
    public void setCurrentObjectResource(Resource currentObjectResource) {
        this.currentObjectResource = currentObjectResource;
    }

    /**
     *
     * @return A value for the typed resource, the source for creating rdf:type relationships to types specified in @typeof.
     */
    public Resource getTypedResource() {
        return typedResource;
    }

    /**
     *
     * @param typedResource A value for the typed resource, the source for creating rdf:type relationships to types specified in @typeof.
     */
    public void setTypedResource(Resource typedResource) {
        this.typedResource = typedResource;
    }

    /**
     *
     * @return A list of incomplete triples. A triple can be incomplete when no object resource is provided alongside a predicate that requires a resource (i.e., @rel or @rev). The triples can be completed when a resource becomes available, which will be when the next subject is specified (part of the process called chaining).
     */
    public Set<RDFaIncompleteStatement> getIncompleteStatements() {
        return incompleteStatements;
    }

    /**
     *
     * @param incompleteStatements A list of incomplete triples. A triple can be incomplete when no object resource is provided alongside a predicate that requires a resource (i.e., @rel or @rev). The triples can be completed when a resource becomes available, which will be when the next subject is specified (part of the process called chaining).
     */
    public void setIncompleteStatements(Set<RDFaIncompleteStatement> incompleteStatements) {
        this.incompleteStatements = incompleteStatements;
    }

    /**
     *
     * @param statement An incomplete triples. A triple can be incomplete when no object resource is provided alongside a predicate that requires a resource (i.e., @rel or @rev). The triples can be completed when a resource becomes available, which will be when the next subject is specified (part of the process called chaining).
     */
    public void addIncompleteStatement(RDFaIncompleteStatement statement) {
        this.incompleteStatements.add(statement);
    }

    /**
     *
     * @return A list mapping that associates IRIs with lists.
     */
    public Map<IRI, List<Value>> getListMappings() {
        return listMappings;
    }

    /**
     *
     * @param listMappings A list mapping that associates IRIs with lists.
     */
    public void setListMappings(Map<IRI, List<Value>> listMappings) {
        this.listMappings = listMappings;
    }

    /**
     *
     * @param key The IRI of the list
     * @param value The resource associated to this list
     */
    public void addListMapping(IRI key, Value value) {
        this.listMappings.computeIfAbsent(key, k -> new ArrayList<>());
        this.listMappings.get(key).add(value);
    }

    /**
     *
     * @param key The IRI of the list
     * @param objects The resources associated to this list
     */
    public void addListMappings(IRI key, List<Value> objects) {
        this.listMappings.put(key, objects);
    }

    /**
     *
     * @return The language. Note that there is no default language.
     */
    public String getCurrentLanguage() {
        if (currentLanguage != null) {
            return currentLanguage.isEmpty() ? null : currentLanguage;
        }
        return evaluationContext != null ? evaluationContext.getLanguage() : null;
    }

    /**
     *
     * @param currentLanguage The language. Note that there is no default language.
     */
    public void setCurrentLanguage(String currentLanguage) {
        this.currentLanguage = currentLanguage;
    }

    /**
     *
     * @return A value for the current property value, the literal to use when creating triples that have a literal object, or IRI-s in the absence of @rel or @rev.
     */
    public Value getCurrentPropertyValue() {
        return currentPropertyValue;
    }

    /**
     *
     * @param currentPropertyValue A value for the current property value, the literal to use when creating triples that have a literal object, or IRI-s in the absence of @rel or @rev.
     */
    public void setCurrentPropertyValue(Value currentPropertyValue) {
        this.currentPropertyValue = currentPropertyValue;
    }

    /**
     *
     * @return The default vocabulary, a value to use as the prefix IRI when a term unknown to the RDFa Processor is used.
     */
    public String getDefaultVocabulary() {
        if (defaultVocabulary != null) {
            return defaultVocabulary.isEmpty() ? null : defaultVocabulary;
        }
        return evaluationContext != null ? evaluationContext.getDefaultVocabulary() : null;
    }

    /**
     *
     * @param defaultVocabulary The default vocabulary, a value to use as the prefix IRI when a term unknown to the RDFa Processor is used.
     */
    public void setDefaultVocabulary(String defaultVocabulary) {
        this.defaultVocabulary = defaultVocabulary;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.elementName).append(" ");
        sb.append("newSubject: ").append(this.newSubject).append(" ");
        sb.append("currentObjectResource: ").append(this.currentObjectResource).append(" ");
        sb.append("typedResource: ").append(this.typedResource).append(" ");
        sb.append("currentLanguage: ").append(this.currentLanguage).append(" ");
        sb.append("currentPropertyValue: ").append(this.currentPropertyValue).append(" ");
        sb.append("defaultVocabulary: ").append(this.defaultVocabulary).append(" ");
        sb.append("characters: ").append(this.getCharacters().trim()).append(" ");
        sb.append("Evaluation context: ").append(this.getEvaluationContext()).append(" ");
        sb.append("Attributes: ").append(this.elementAttributes.keySet()).append(" ");

        return sb.toString();
    }

    /**
     *
     * @return The string created by concatenating the text content of each of the descendant elements of the current element in document order.
     */
    public String getCharacters() {
        return characters.toString();
    }

    /**
     * Clear the current character buffer
     */
    public void clearCharacters() {
        this.characters = new StringBuilder();
    }

    /**
     * Adds characters to the character buffer
     */
    public void addCharacters(char[] ch, int start, int length) {
        this.characters.append(ch, start, length);
    }

    public boolean isXmlLiteralProperty() {
        return xmlLiteralProperty;
    }

    public void setXmlLiteralProperty(boolean xmlLiteralProperty) {
        this.xmlLiteralProperty = xmlLiteralProperty;
    }

    public String getXmlLiteralContent() {
        return xmlLiteralContent.toString();
    }

    public void appendXmlLiteralContent(String content) {
        this.xmlLiteralContent.append(content);
    }

    /**
     *
     * @return The map of the XML attribute of the current element
     */
    public Map<String, String> getElementAttributes() {
        return elementAttributes;
    }

    /**
     *
     * @param elementAttributes The map of the XML attribute of the current element
     */
    public void setElementAttributes(Attributes elementAttributes) {
        for(int i = 0; i < elementAttributes.getLength(); i++) {
            this.elementAttributes.put(elementAttributes.getQName(i), elementAttributes.getValue(i));
        }
    }

    /**
     *
     * @return The flag that indicates that the current element is at the root of the document
     */
    public boolean isRootElement() {
        return isRootElement;
    }

    /**
     *
     * @param rootElement The flag that indicates that the current element is at the root of the document
     */
    public void setRootElement(boolean rootElement) {
        isRootElement = rootElement;
    }

    /**
     *
     * @return The evaluation context that is used to evaluate the current element
     */
    public RDFaEvaluationContext getEvaluationContext() {
        return evaluationContext;
    }

    /**
     *
     * @param evaluationContext The evaluation context that is used to evaluate the current element
     */
    public void setEvaluationContext(RDFaEvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
    }

    /**
     *
     * @return The name ot the current element
     */
    public String getElementName() {
        return elementName;
    }

    /**
     *
     * @param elementName The name ot the current element
     */
    public void setElementName(String elementName) {
        this.elementName = elementName;
    }
}
