package fr.inria.corese.core.next.impl.io.parser.rdfa.model;

import fr.inria.corese.core.next.api.IRI;
import fr.inria.corese.core.next.api.Resource;
import fr.inria.corese.core.next.api.Value;

/**
 * This utility class represents triples in the process of creation during the chaining of element in an RDFa document.
 * This class in not intended to be used anywhere outside the RDFa parser
 * @see <a href="https://www.w3.org/TR/rdfa-core/#s_Completing_Incomplete_Triples">Incomplete triples</a>
 */
public class RDFaIncompleteStatement {

    public enum Direction {
        FORWARD,
        BACKWARD,
        NONE
    }

    private Resource subject = null;
    private IRI predicate = null;
    private Value object = null;
    private Direction direction = null;

    private RDFaIncompleteStatement() {
        this.direction = Direction.FORWARD;
    }

    public RDFaIncompleteStatement(IRI predicate) {
        this();
        this.predicate = predicate;
    }

    public RDFaIncompleteStatement(IRI predicate, Direction direction) {
        this();
        this.predicate = predicate;
        this.direction = direction;
    }

    public boolean isForward() {
        return this.direction == Direction.FORWARD;
    }

    public boolean isBackward() {
        return this.direction == Direction.BACKWARD;
    }

    public Direction getDirection() {
        return this.direction;
    }

    public void setForward() {
        this.direction = Direction.FORWARD;
    }

    public void setBackward() {
        this.direction = Direction.BACKWARD;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if(! (o instanceof RDFaIncompleteStatement oStat)) {
            return false;
        }
        return oStat.getSubject().equals(this.getSubject())
                && oStat.getPredicate().equals(this.getPredicate())
                && oStat.getObject().equals(this.getObject())
                && oStat.getDirection().equals(this.getDirection());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (getSubject() == null ? 0 : getSubject().hashCode());
        hash = 31 * hash + (getPredicate() == null ? 0 : getPredicate().hashCode());
        hash = 31 * hash + (getObject() == null ? 0 : getObject().hashCode());
        hash = 31 * hash + (getDirection() == null ? 0 : getDirection().hashCode());
        return hash;
    }
}
