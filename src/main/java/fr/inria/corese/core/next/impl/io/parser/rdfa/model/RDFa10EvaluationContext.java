package fr.inria.corese.core.next.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;

import java.util.*;

/**
 * This class is to be used during the evaluation of an HTML file to generate triples during the DOM traversal.
 * @see <a href="https://www.w3.org/TR/rdfa-syntax/#sec_5.2.">RDFa recommandation<a/>
 */
public class RDFa10EvaluationContext extends AbstractRDFaEvaluationContext {

    public RDFa10EvaluationContext(IRI baseIri) {
        this(baseIri, baseIri);
    }

    public RDFa10EvaluationContext(IRI baseIri, IRI parentSubjectResource) {
        super(baseIri, parentSubjectResource);
    }

    public RDFa10EvaluationContext(RDFa10EvaluationContext context) {
        super(context);
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
