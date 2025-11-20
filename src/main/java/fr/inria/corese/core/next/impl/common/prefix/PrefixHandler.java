package fr.inria.corese.core.next.impl.common.prefix;

import fr.inria.corese.core.next.impl.common.vocabulary.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified prefix handler for managing namespace prefix mappings across all RDF formats.
 */
public class PrefixHandler {

    /**
     * Map of prefix to namespace URI.
     */
    private final ConcurrentHashMap<String, String> prefixToNamespace;

    /**
     * Map of namespace URI to prefix (for reverse lookup).
     */
    private final ConcurrentHashMap<String, String> namespaceToPrefix;


    /**
     * Creates a new PrefixHandler.
     *
     * @param includeStandardVocabularies if true, initializes with standard W3C vocabularies
     *                                    (rdf, rdfs, xsd, owl)
     */
    public PrefixHandler(boolean includeStandardVocabularies) {
        this.prefixToNamespace = new ConcurrentHashMap<>();
        this.namespaceToPrefix = new ConcurrentHashMap<>();

        if (includeStandardVocabularies) {
            initializeStandardVocabularies();
        }
    }

    /**
     * Initializes the handler with standard W3C vocabulary prefixes by using the
     * dedicated Vocabulary enum classes.
     */
    private void initializeStandardVocabularies() {
        List<Class<? extends Enum<? extends Vocabulary>>> vocabularyClasses = Arrays.asList(
                RDF.class,
                RDFS.class,
                XSD.class,
                OWL.class,
                FOAF.class
        );

        for (Class<? extends Enum<? extends Vocabulary>> vocabClass : vocabularyClasses) {
            Enum<? extends Vocabulary>[] constants = vocabClass.getEnumConstants();
            if (constants.length > 0) {
                Vocabulary vocabInstance = (Vocabulary) constants[0];
                setPrefix(vocabInstance.getPreferredPrefix(), vocabInstance.getNamespace());
            }
        }
    }

    /**
     * Sets or updates a prefix mapping.
     *
     * @param prefix    the prefix
     * @param namespace the namespace URI
     * @throws IllegalArgumentException if prefix or namespace is null
     */
    public void setPrefix(String prefix, String namespace) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }
        if (namespace == null) {
            throw new IllegalArgumentException("Namespace cannot be null");
        }

        String oldNamespace = prefixToNamespace.get(prefix);
        if (oldNamespace != null) {
            namespaceToPrefix.remove(oldNamespace);
        }

        prefixToNamespace.put(prefix, namespace);
        namespaceToPrefix.put(namespace, prefix);
    }

    /**
     * Gets the namespace URI for a given prefix.
     *
     * @param prefix the prefix to look up
     * @return the namespace URI, or null if the prefix is not registered
     */
    public String getNamespace(String prefix) {
        return prefixToNamespace.get(prefix);
    }

    /**
     * Gets the prefix for a given namespace URI.
     *
     * @param namespace the namespace URI to look up
     * @return the prefix, or null if the namespace is not registered
     */
    public String getPrefix(String namespace) {
        return namespaceToPrefix.get(namespace);
    }

    /**
     * Checks if a prefix is registered.
     *
     * @param prefix the prefix to check
     * @return true if the prefix is registered, false otherwise
     */
    public boolean hasPrefix(String prefix) {
        return prefixToNamespace.containsKey(prefix);
    }


    /**
     * Removes a prefix mapping.
     *
     * @param prefix the prefix to remove
     * @return true if the prefix was removed, false if it didn't exist
     */
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
    public void clear() {
        prefixToNamespace.clear();
        namespaceToPrefix.clear();
    }

    /**
     * Returns all registered prefixes.
     *
     * @return an immutable set of all prefixes
     */
    public Set<String> getPrefixes() {
        return Set.copyOf(prefixToNamespace.keySet());
    }

    /**
     * Returns all registered namespaces (namespace URIs).
     *
     * @return an immutable set of all namespace URIs
     */
    public Set<String> getNamespaces() {
        return Set.copyOf(namespaceToPrefix.keySet());
    }

    /**
     * Returns all prefix mappings as an unmodifiable map.
     *
     * @return an unmodifiable map where keys are prefixes and values are namespace URIs
     */
    public Map<String, String> getPrefixMap() {
        return Collections.unmodifiableMap(new HashMap<>(prefixToNamespace));
    }

    /**
     * Returns the number of registered prefix mappings.
     *
     * @return the number of mappings
     */
    public int size() {
        return prefixToNamespace.size();
    }

    /**
     * Checks if there are no registered prefix mappings.
     *
     * @return true if no mappings exist, false otherwise
     */
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
    public String expandPrefix(String prefixedName) {
        if (prefixedName == null) {
            throw new IllegalArgumentException("Prefixed name cannot be null");
        }

        int colonIndex = prefixedName.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Invalid prefixed name (no colon): " + prefixedName);
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
    public String compressIRI(String iri) {
        if (iri == null) {
            throw new IllegalArgumentException("IRI cannot be null");
        }

        String bestPrefix = null;
        int bestLength = 0;

        for (String namespace : namespaceToPrefix.keySet()) {
            if (iri.startsWith(namespace) && namespace.length() > bestLength) {
                bestPrefix = namespaceToPrefix.get(namespace);
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
     * Copies all prefix mappings from another PrefixHandler.
     * Existing mappings are preserved unless overridden.
     *
     * @param other the PrefixHandler to copy from
     * @throws IllegalArgumentException if other is null
     */
    public void copyFrom(PrefixHandler other) {
        if (other == null) {
            throw new IllegalArgumentException("Source PrefixHandler cannot be null");
        }

        for (String prefix : other.getPrefixes()) {
            String namespace = other.getNamespace(prefix);
            setPrefix(prefix, namespace);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PrefixHandler{");
        boolean first = true;
        for (String prefix : prefixToNamespace.keySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(prefix).append("->").append(prefixToNamespace.get(prefix));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}