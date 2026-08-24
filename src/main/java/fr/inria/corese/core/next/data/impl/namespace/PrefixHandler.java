package fr.inria.corese.core.next.data.impl.namespace;

import fr.inria.corese.core.next.data.api.namespace.PrefixMapping;
import fr.inria.corese.core.next.data.api.namespace.Namespace;
import fr.inria.corese.core.next.data.api.namespace.SimpleNamespace;
import fr.inria.corese.core.next.data.api.vocabulary.FOAF;
import fr.inria.corese.core.next.data.api.vocabulary.OWL;
import fr.inria.corese.core.next.data.api.vocabulary.RDF;
import fr.inria.corese.core.next.data.api.vocabulary.RDFS;
import fr.inria.corese.core.next.data.api.vocabulary.XSD;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Unified prefix handler for managing namespace prefix mappings across all RDF formats.
 */
public class PrefixHandler implements PrefixMapping, Cloneable {

    /**
     * Map of prefix to namespace URI.
     */
    private ConcurrentHashMap<String, String> prefixToNamespace;

    /**
     * Map of namespace URI to prefix (for reverse lookup).
     */
    private ConcurrentHashMap<String, String> namespaceToPrefix;

    /**
     * The default namespace
     */
    private String defaultNamespace;

    /**
     * Creates a new PrefixHandler.
     */
    public PrefixHandler() {
        this(false);
    }

    /**
     * Creates a new PrefixHandler.
     *
     * @param includeStandardVocabularies if true, initializes with standard W3C vocabularies
     *                                    (rdf, rdfs, xsd, owl, foaf)
     */
    public PrefixHandler(boolean includeStandardVocabularies) {
        this.prefixToNamespace = new ConcurrentHashMap<>();
        this.namespaceToPrefix = new ConcurrentHashMap<>();
        this.defaultNamespace = null;

        if (includeStandardVocabularies) {
            initializeStandardVocabularies();
        }
    }

    /**
     * Copy constructor
     */
    public PrefixHandler(PrefixHandler oHandler) {
        this.prefixToNamespace = new ConcurrentHashMap<>(oHandler.prefixToNamespace);
        this.namespaceToPrefix = new ConcurrentHashMap<>(oHandler.namespaceToPrefix);
        this.defaultNamespace = oHandler.defaultNamespace;
    }

    /**
     * Initializes the handler with standard W3C vocabulary prefixes by using the
     * dedicated Vocabulary enum classes.
     */
    private void initializeStandardVocabularies() {
        setPrefix(RDF.getVocabularyPreferredPrefix(), RDF.getVocabularyNamespace());
        setPrefix(RDFS.getVocabularyPreferredPrefix(), RDFS.getVocabularyNamespace());
        setPrefix(XSD.getVocabularyPreferredPrefix(), XSD.getVocabularyNamespace());
        setPrefix(OWL.getVocabularyPreferredPrefix(), OWL.getVocabularyNamespace());
        setPrefix(FOAF.getVocabularyPreferredPrefix(), FOAF.getVocabularyNamespace());
    }

    /**
     * Sets or updates a prefix mapping.
     * Validates that the prefix matches XML NCName rules.
     *
     * @param prefix    the prefix
     * @param namespace the namespace URI
     * @throws IllegalArgumentException if prefix is invalid or namespace is null
     */
    @Override
    public void setPrefix(String prefix, String namespace) {
        if (!isValidPrefix(prefix)) {
            throw new IllegalArgumentException(
                    "Invalid prefix format: '" + prefix +
                            "' (must be empty or valid according to Turtle/TriG specification)");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("Namespace cannot be null");
        }

        String oldNamespace = prefixToNamespace.get(prefix);
        if (oldNamespace != null && ! oldNamespace.equals(namespace)) {
            namespaceToPrefix.remove(oldNamespace);
            prefixToNamespace.remove(prefix);
        }
        String oldPrefix = namespaceToPrefix.get(namespace);
        if(oldPrefix != null && ! oldPrefix.equals(prefix)) {
            namespaceToPrefix.remove(namespace);
            prefixToNamespace.remove(oldPrefix);
        }

        prefixToNamespace.put(prefix, namespace);
        namespaceToPrefix.put(namespace, prefix);
    }

    /**
     * Sets a namespace using a Namespace object.
     *
     * @param namespace the Namespace object to register
     * @throws IllegalArgumentException if namespace is null
     */
    @Override
    public void setNamespace(Namespace namespace) {
        if (namespace == null) {
            throw new IllegalArgumentException("Namespace cannot be null");
        }
        setPrefix(namespace.getPrefix(), namespace.getNamespace());
    }

    /**
     * Gets the namespace URI for a given prefix.
     *
     * @param prefix the prefix to look up
     * @return the namespace URI, or null if the prefix is not registered
     */
    @Override
    public String getNamespace(String prefix) {
        return prefixToNamespace.get(prefix);
    }

    /**
     * Gets the prefix for a given namespace URI.
     *
     * @param namespace the namespace URI to look up
     * @return the prefix, or null if the namespace is not registered
     */
    @Override
    public String getPrefix(String namespace) {
        return namespaceToPrefix.get(namespace);
    }

    /**
     * Checks if a prefix is registered.
     *
     * @param prefix the prefix to check
     * @return true if the prefix is registered, false otherwise
     */
    @Override
    public boolean hasPrefix(String prefix) {
        return prefixToNamespace.containsKey(prefix);
    }

    @Override
    public boolean hasNamespace(String namespace) {
        return namespaceToPrefix.containsKey(namespace);
    }

    /**
     * Gets the default namespace
     *
     * @return the default namespace URI, or null if not set
     */
    @Override
    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    /**
     * Sets the default namespace.
     *
     * @param namespace the default namespace IRI, or null to unset
     */
    @Override
    public void setDefaultNamespace(String namespace) {
        this.defaultNamespace = namespace;
    }

    /**
     * Removes a prefix mapping.
     *
     * @param prefix the prefix to remove
     * @return true if the prefix was removed, false if it didn't exist
     */
    @Override
    public boolean removePrefix(String prefix) {
        String namespace = prefixToNamespace.remove(prefix);
        if (namespace != null) {
            namespaceToPrefix.remove(namespace);
            return true;
        }
        return false;
    }

    /**
     * Removes all prefix mappings.
     */
    @Override
    public void clear() {
        prefixToNamespace.clear();
        namespaceToPrefix.clear();
        defaultNamespace = null;
    }

    /**
     * Returns all registered prefixes.
     *
     * @return an immutable set of all prefixes
     */
    @Override
    public Set<String> getPrefixes() {
        return Set.copyOf(prefixToNamespace.keySet());
    }

    /**
     * Returns all registered namespaces (namespace URIs).
     *
     * @return an immutable set of all namespace URIs
     */
    @Override
    public Set<String> getNamespaces() {
        return Set.copyOf(namespaceToPrefix.keySet());
    }

    /**
     * Returns all prefix mappings as an unmodifiable map.
     *
     * @return an unmodifiable map where keys are prefixes and values are namespace URIs
     */
    @Override
    public Map<String, String> getPrefixMap() {
        return Collections.unmodifiableMap(new HashMap<>(prefixToNamespace));
    }

    /**
     * Returns all namespaces mappings as an unmodifiable map.
     *
     * @return an unmodifiable map where keys are namespaces IRIs and values are prefixes
     */
    @Override
    public Map<String, String> getNamespaceMap() {
        return Collections.unmodifiableMap(new HashMap<>(namespaceToPrefix));
    }

    /**
     * Returns all namespace objects as an immutable set.
     * Each Namespace object contains both prefix and IRI.
     *
     * @return a set of Namespace objects
     */
    @Override
    public Set<Namespace> getNamespaceObjects() {
        return namespaceToPrefix.entrySet().stream()
                .map(e -> new SimpleNamespace(e.getValue(), e.getKey()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns the number of registered prefix mappings.
     *
     * @return the number of mappings
     */
    @Override
    public int size() {
        return prefixToNamespace.size();
    }

    /**
     * Checks if there are no registered prefix mappings.
     *
     * @return true if no mappings exist, false otherwise
     */
    @Override
    public boolean isEmpty() {
        return prefixToNamespace.isEmpty();
    }

    /**
     * Expands a prefixed name to a full IRI.
     *
     * @param prefixedName the prefixed name
     * @return the full IRI, or null if the prefix is not registered
     * @throws IllegalArgumentException if prefixedName is null or doesn't contain ":"
     */
    @Override
    public String expandPrefix(String prefixedName) {
        if (prefixedName == null) {
            throw new IllegalArgumentException("Prefixed name cannot be null");
        }

        int colonIndex = prefixedName.indexOf(':');

        if (colonIndex == -1) {
            if (defaultNamespace != null) {
                return defaultNamespace + prefixedName;
            }
            return null;
        }

        String prefix = prefixedName.substring(0, colonIndex);
        String localName = prefixedName.substring(colonIndex + 1);

        String namespace = getNamespace(prefix);
        if (namespace == null) {
            return null;
        }

        return namespace + localName;
    }

    /**
     * Compresses a full IRI to a prefixed name if possible.
     *
     * @param iri the full IRI to compress
     * @return the prefixed name if a matching namespace is found, otherwise the original IRI
     * @throws IllegalArgumentException if iri is null
     */
    @Override
    public String compressIRI(String iri) {
        if (iri == null) {
            throw new IllegalArgumentException("IRI cannot be null");
        }

        String bestPrefix = null;
        int bestLength = 0;

        for (Map.Entry<String, String> entry : namespaceToPrefix.entrySet()) {
            String namespace = entry.getKey();
            if (iri.startsWith(namespace) && namespace.length() > bestLength) {
                bestPrefix = entry.getValue();
                bestLength = namespace.length();
            }
        }

        if (bestPrefix != null) {
            String localName = iri.substring(bestLength);
            return bestPrefix + ":" + localName;
        }

        return iri;
    }

    /**
     * Checks if a prefix is valid according
     * Empty string "" is considered valid (for Turtle default prefix)
     *
     * @param prefix the prefix to validate
     * @return true if the prefix is valid, false otherwise
     */
    @Override
    public boolean isValidPrefix(String prefix) {
        if (prefix == null) {
            return false;
        }
        if (prefix.isEmpty()) {
            return true;
        }
        return !prefix.contains(":");
    }

    /**
     * Copies all prefix mappings from another handler.
     * Existing mappings in this handler are overwritten if prefixes match.
     *
     * @param other the handler to copy from
     * @throws IllegalArgumentException if other is null
     */
    @Override
    public void copyFrom(PrefixMapping other) {
        if (other == null) {
            throw new IllegalArgumentException("Source handler cannot be null");
        }

        new HashMap<>(other.getPrefixMap()).forEach(this::setPrefix);

        String otherDefault = other.getDefaultNamespace();
        if (otherDefault != null) {
            this.setDefaultNamespace(otherDefault);
        }
    }

    /**
     * Creates a deep copy of this handler.
     * The returned handler is independent and can be modified without
     * affecting the original.
     *
     * @return a new PrefixHandler instance with same mappings
     */
    @SuppressWarnings("java:S2975")
    @Override
    public PrefixHandler clone() {
        try {
            PrefixHandler cloned = (PrefixHandler) super.clone();
            cloned.prefixToNamespace = new ConcurrentHashMap<>(this.prefixToNamespace);
            cloned.namespaceToPrefix = new ConcurrentHashMap<>(this.namespaceToPrefix);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PrefixHandler{");
        boolean first = true;
        for (Map.Entry<String, String> entry : prefixToNamespace.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append('"').append(entry.getKey()).append("\": \"").append(entry.getValue()).append('"');
            first = false;
        }
        if (defaultNamespace != null) {
            sb.append(", default: \"").append(defaultNamespace).append('"');
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PrefixMapping mapping)) {
            return false;
        }
        return Objects.equals(this.getDefaultNamespace(), mapping.getDefaultNamespace())
                && Objects.equals(this.getNamespaceMap(), mapping.getNamespaceMap())
                && Objects.equals(this.getPrefixMap(), mapping.getPrefixMap());
    }

    @Override
    public int hashCode() {
        return Objects.hash(defaultNamespace, prefixToNamespace, namespaceToPrefix);
    }

}
