package fr.inria.corese.core.next.api.model.impl.literal;

public abstract class AbstractNumber implements AbstractLiteral, Comparable<AbstractNumber> {
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
