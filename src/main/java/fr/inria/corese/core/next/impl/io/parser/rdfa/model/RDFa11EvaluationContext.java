package fr.inria.corese.core.next.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;

import java.util.*;

/**
 * This class is to be used during the evaluation of an HTML file to generate triples during the DOM traversal.
 * @see <a href="https://www.w3.org/TR/rdfa-core/#evaluation-context">RDFa recommandation<a/>
 */
public class RDFa11EvaluationContext extends AbstractRDFaEvaluationContext {

    /**
     * A list mapping that associates IRIs with lists.
     */
    private Map<IRI, Set<Value>> listMappings = new HashMap<>();
    /**
     * The language. Note that there is no default language.
     */
    private String language = null;
    /**
     * The term mappings, a list of terms and their associated IRIs. This specification does not define an initial list. Host Languages MAY define an initial list.
     */
    private Map<String, IRI> termMappings = new HashMap<>();

    /**
     * The default vocabulary, a value to use as the prefix IRI when a term unknown to the RDFa Processor is used. This specification does not define an initial setting for the default vocabulary. Host Languages MAY define an initial setting.
     */
    private String defaultVocabulary = null;


    public RDFa11EvaluationContext(IRI baseIri) {
        this(baseIri, baseIri);
    }

    public RDFa11EvaluationContext(IRI baseIri, IRI parentSubjectResource) {
        super(baseIri, parentSubjectResource);
    }

    public RDFa11EvaluationContext(RDFa11EvaluationContext context) {
        super(context);
        this.listMappings = new HashMap<>(context.listMappings);
        this.termMappings = new HashMap<>(context.termMappings);
        this.defaultVocabulary = context.defaultVocabulary;;
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
        sb.append("Mappings: [");
        this.getIriMappings().forEach((key, value) -> sb.append("(").append(key).append(", ").append(value.stringValue()).append(") "));
        sb.append("] ");
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
        if(! this.getIncompleteStatement().isEmpty()) {
            sb.append(this.getIncompleteStatement().size()).append(" incomplete statements.");
        }

        return sb.toString();
    }
}
