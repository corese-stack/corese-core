package fr.inria.corese.core.next.api.base.model.literal;

import fr.inria.corese.core.next.api.IRI;

public abstract class AbstractNumber extends AbstractLiteral implements Comparable<AbstractNumber> {
    
    protected AbstractNumber(IRI datatype) {
        super(datatype);
    }

    /**
     * Comparison using double value
     * @param abstractNumber
     * @return
     */
    @Override
    public int compareTo(AbstractNumber abstractNumber) {
        return Double.compare(this.doubleValue(), abstractNumber.doubleValue());
    }
}
