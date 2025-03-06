package fr.inria.corese.core.next.api.model.impl.literal;

import fr.inria.corese.core.next.api.model.IRI;

public abstract class AbstractNumber extends AbstractLiteral implements Comparable<AbstractNumber> {
    
    protected AbstractNumber(IRI datatype) {
        super(datatype);
    }

    /**
     * Comprison using double value
     * @param abstractNumber
     * @return
     */
    @Override
    public int compareTo(AbstractNumber abstractNumber) {
        return Double.compare(this.doubleValue(), abstractNumber.doubleValue());
    }
}
