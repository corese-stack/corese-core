package fr.inria.corese.core.next.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;

/**
 * This class represents triples in the process of creation during the chaining of element in an RDFa document.
 */
public class RDFaIncompleteStatement {

    private Resource subject = null;
    private IRI predicate = null;
    private Value object = null;

    public RDFaIncompleteStatement() {

    }

    public RDFaIncompleteStatement(IRI predicate) {

    }

    public Resource getSubject() {
        return subject;
    }

    public void setSubject(Resource subject) {
        this.subject = subject;
    }

    public IRI getPredicate() {
        return predicate;
    }

    public void setPredicate(IRI predicate) {
        this.predicate = predicate;
    }

    public Value getObject() {
        return object;
    }

    public void setObject(Value object) {
        this.object = object;
    }

    public boolean hasSubject() {
        return this.getSubject() != null;
    }

    public boolean hasPredicate() {
        return this.getPredicate() != null;
    }

    public boolean hasObject() {
        return this.getObject() != null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(this.hasSubject()) {
            sb.append(this.getSubject().toString());
        } else {
            sb.append("?");
        }
        sb.append(" ");

        if(this.hasPredicate()) {
            sb.append(this.getPredicate().toString());
        } else {
            sb.append("?");
        }
        sb.append(" ");

        if(this.hasObject()) {
            sb.append(this.getObject().toString());
        } else {
            sb.append("?");
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (getSubject() == null ? 0 : getSubject().hashCode());
        hash = 31 * hash + (getPredicate() == null ? 0 : getPredicate().hashCode());
        hash = 31 * hash + (getObject() == null ? 0 : getObject().hashCode());
        return hash;
    }
}
